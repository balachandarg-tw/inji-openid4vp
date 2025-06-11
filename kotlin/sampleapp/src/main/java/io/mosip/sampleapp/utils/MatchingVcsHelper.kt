package io.mosip.sampleapp.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jayway.jsonpath.JsonPath
import io.mosip.openID4VP.constants.FormatType
import io.mosip.sampleapp.VCWithFormat
import io.mosip.sampleapp.utils.MdocKeyManager.getIssuerAuthenticationAlgorithmForMdocVC
import io.mosip.sampleapp.utils.MdocKeyManager.getMdocAuthenticationAlgorithm

class MatchingVcsHelper {
    fun getVcsMatchingAuthRequest(
        vcList: List<VCWithFormat>,
        authRequest: JsonObject
    ): MatchingResult {
        val matchingVCs = mutableMapOf<String, MutableList<VCWithFormat>>()
        val requestedClaims = mutableSetOf<String>()
        val presentationDefinition = authRequest.getAsJsonObject("presentationDefinition")
        val inputDescriptors = presentationDefinition.getAsJsonArray("inputDescriptors")
        var hasFormatOrConstraints = false

        for (vcWithFormat in vcList) {
            val vc = vcWithFormat.vc
            val vcFormat = vcWithFormat.format
            val rawCBORData = vcWithFormat.rawCBORData

            for (i in 0 until inputDescriptors.size()) {
                val inputDescriptor = inputDescriptors[i].asJsonObject
                val format = inputDescriptor.getAsJsonObject("format")
                    ?: presentationDefinition.getAsJsonObject("format")
                val constraints = inputDescriptor.getAsJsonObject("constraints")

                hasFormatOrConstraints = hasFormatOrConstraints ||
                        format != null || (constraints?.has("fields") == true)

                val matchesFormat = areVCFormatAndProofTypeMatchingRequest(format, vcWithFormat)
                val matchesConstraints = isVCMatchingRequestConstraints(constraints, vc, requestedClaims)

                val shouldInclude = if (constraints?.has("fields") == true && format != null) {
                    matchesFormat && matchesConstraints
                } else {
                    matchesFormat || matchesConstraints
                }

                if (matchesFormat) {
                    val descriptorId = inputDescriptor.get("id").asString

                    val list = matchingVCs.getOrPut(descriptorId) { mutableListOf() }

                    if (list.none { it.vc == vc && it.format == vcFormat }) {
                        list.add(VCWithFormat(vcFormat, vc.deepCopy(), vcWithFormat.keyType, rawCBORData))
                    }
                }
            }
        }

        if (!hasFormatOrConstraints && inputDescriptors.size() > 0) {
            val fallbackId = inputDescriptors[0].asJsonObject.get("id").asString
            matchingVCs[fallbackId] = vcList.map { VCWithFormat(it.format, it.vc.deepCopy(), it.keyType, it.rawCBORData) }.toMutableList()
        }

        return MatchingResult(
            matchingVCs,
            requestedClaims.joinToString(","),
            presentationDefinition.get("purpose")?.asString ?: ""
        )
    }


    fun buildSelectedVCsMapPlain(
        selectedItems: List<Pair<String, VCWithFormat>>
    ): Map<String, Map<FormatType, List<String>>> {
        val result = mutableMapOf<String, MutableMap<FormatType, MutableList<String>>>()
        val gson = Gson()

        for ((inputDescriptorId, vcWithFormat) in selectedItems) {
            val formatType = try {
                FormatType.valueOf(vcWithFormat.format.uppercase().replace("-", "_"))
            } catch (e: IllegalArgumentException) {
                continue
            }

            val credentialValue = if (formatType == FormatType.MSO_MDOC) {
                vcWithFormat.rawCBORData // Use rawCbor for mdoc
            } else {
                gson.toJson(vcWithFormat.vc) // Use JSON for others
            }

            val formatMap = result.getOrPut(inputDescriptorId) { mutableMapOf() }
            val credentialList = formatMap.getOrPut(formatType) { mutableListOf() }

            credentialValue?.let { credentialList.add(it) }
        }

        return result.mapValues { (_, innerMap) ->
            innerMap.mapValues { (_, list) -> list.toList() }
        }
    }



    private fun areVCFormatAndProofTypeMatchingRequest(format: JsonObject?, vcWithFormat: VCWithFormat): Boolean {
        if (format == null) return false

        val vc = vcWithFormat.vc
        val vcFormat = vcWithFormat.format

        return when (vcFormat) {
            "ldp_vc" -> {
                val proof = vc.getAsJsonObject("proof") ?: return false
                val proofType = proof.get("type")?.asString ?: return false

                format.entrySet().any { (type, value) ->
                    type == vcFormat &&
                            value.asJsonObject.getAsJsonArray("proof_type")
                                ?.mapNotNull { it.asString }
                                ?.contains(proofType) == true
                }
            }

            "mso_mdoc" -> {
                val issuerAuthArray = vc.getAsJsonObject("issuerSigned")
                    ?.getAsJsonArray("issuerAuth") ?: return false

                if (issuerAuthArray.size() < 3) return false

                val issuerProofType = issuerAuthArray[0].asJsonObject["1"]?.asInt ?: return false
                val issuerAlgorithm = getIssuerAuthenticationAlgorithmForMdocVC(issuerProofType)

                val mdocAuth = issuerAuthArray[2].asJsonObject
                val deviceAlgorithm = getMdocAuthenticationAlgorithm(mdocAuth)

                format.entrySet().any { (type, value) ->
                    type == vcFormat &&
                            value.asJsonObject.getAsJsonArray("alg")?.mapNotNull { it.asString }?.let { algList ->
                                listOf(issuerAlgorithm, deviceAlgorithm).all { algList.contains(it) }
                            } == true
                }
            }

            else -> false
        }
    }

    private fun isVCMatchingRequestConstraints(
        constraints: JsonObject?,
        vc: JsonObject,
        requestedClaims: MutableSet<String>
    ): Boolean {
        val fields = constraints?.getAsJsonArray("fields") ?: return false
        val processedCredential = fetchCredentialBasedOnFormat(vc) ?: return false

        fun getJsType(value: Any?): String = when (value) {
            is String -> "string"
            is Int, is Long, is Double, is Float -> "number"
            is Boolean -> "boolean"
            is Map<*, *> -> "object"
            is List<*> -> "array"
            null -> "undefined"
            else -> "object"
        }

        for (fieldElem in fields) {
            val field = fieldElem.asJsonObject
            val paths = field.getAsJsonArray("path") ?: continue
            val filter = field.getAsJsonObject("filter")

            val fieldMatched = paths.any { pathElem ->
                val jsonPath = pathElem.asString

                try {
                    val resultList = JsonPath.read<Any>(processedCredential.toString(), jsonPath)

                    val results = if (resultList is List<*>) resultList else listOf(resultList)
                    if (results.isEmpty()) return@any false

                    if (filter == null) return@any true

                    val expectedType = filter.get("type")?.asString ?: ""
                    val pattern = filter.get("pattern")?.asString

                    results.any { match ->
                        val jsType = getJsType(match)
                        if (jsType != expectedType) return@any false

                        if (pattern != null && match is String) {
                            Regex(pattern).containsMatchIn(match)
                        } else {
                            true
                        }
                    }
                } catch (e: Exception) {
                    println("JsonPath failed for $jsonPath: ${e.message}")
                    false
                }
            }

            if (!fieldMatched) return false

            // Add claim only if matched
            val claimName = Regex("\\['([^']+)']").replace(paths.first().asString, ".$1")
                .split('.')
                .lastOrNull { it.isNotEmpty() && it != "$" } ?: ""
            requestedClaims.add(claimName)
        }

        return true
    }

    private fun fetchCredentialBasedOnFormat(vc: JsonObject): JsonObject? {
        val format = vc.get("format")?.asString ?: "ldp_vc"
        val verifiableCredential = vc ?: return null

        return when (format) {
            "ldp_vc" -> verifiableCredential
            "mso_mdoc" -> {
                val processedCredential = verifiableCredential.getAsJsonObject("processedCredential") ?: return null
                getProcessedDataForMdoc(processedCredential)
            }
            else -> null
        }
    }

    private fun getProcessedDataForMdoc(processedCredential: JsonObject): JsonObject {
        val issuerSigned = processedCredential.getAsJsonObject("issuerSigned") ?: return JsonObject()
        val nameSpaces = issuerSigned.getAsJsonObject("nameSpaces") ?: return JsonObject()

        val processedData = JsonObject()

        for ((nsKey, elementsArrayElem) in nameSpaces.entrySet()) {
            val elementsArray = elementsArrayElem.asJsonArray ?: continue
            val asObject = JsonObject()

            for (itemElem in elementsArray) {
                val item = itemElem.asJsonObject
                val id = item.get("elementIdentifier")?.asString ?: continue
                val value = item.get("elementValue") ?: continue
                asObject.add(id, value)
            }

            processedData.add(nsKey, asObject)
        }

        return processedData
    }
}


data class MatchingResult(
    val matchingVCs: Map<String, List<VCWithFormat>>,
    val requestedClaims: String,
    val purpose: String
)