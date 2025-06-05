package io.mosip.sampleapp

import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jayway.jsonpath.JsonPath
import io.mosip.openID4VP.authorizationRequest.WalletMetadata
import io.mosip.openID4VP.constants.FormatType
import io.mosip.sampleapp.vc.VCWithFormat


class OVPHelper {
    fun getVcsMatchingAuthRequest(
        vcList: List<VCWithFormat>,
        authRequest: JsonObject
    ): MatchResult {
        val matchingVCs = mutableMapOf<String, MutableList<VCWithFormat>>()
        val requestedClaims = mutableSetOf<String>()
        val presentationDefinition = authRequest.getAsJsonObject("presentationDefinition")
        val inputDescriptors = presentationDefinition.getAsJsonArray("inputDescriptors")
        var hasFormatOrConstraints = false

        for (vcWithFormat in vcList) {
            val vc = vcWithFormat.vc
            val vcFormat = vcWithFormat.format

            for (i in 0 until inputDescriptors.size()) {
                val inputDescriptor = inputDescriptors[i].asJsonObject
                val format = inputDescriptor.getAsJsonObject("format")
                    ?: presentationDefinition.getAsJsonObject("format")
                val constraints = inputDescriptor.getAsJsonObject("constraints")

                hasFormatOrConstraints = hasFormatOrConstraints ||
                        format != null || (constraints?.has("fields") == true)

                val matchesFormat = areVCFormatAndProofTypeMatchingRequest(format, vc)
                val matchesConstraints = isVCMatchingRequestConstraints(constraints, vc, requestedClaims)

                val shouldInclude = if (constraints?.has("fields") == true && format != null) {
                    matchesFormat && matchesConstraints
                } else {
                    matchesFormat || matchesConstraints
                }

                if (shouldInclude) {
                    val descriptorId = inputDescriptor.get("id").asString

                    val list = matchingVCs.getOrPut(descriptorId) { mutableListOf() }

                    // Add VCWithFormat if not already in list
                    if (list.none { it.vc == vc && it.format == vcFormat }) {
                        list.add(VCWithFormat(vcFormat, vc.deepCopy()))
                    }
                }
            }
        }

        if (!hasFormatOrConstraints && inputDescriptors.size() > 0) {
            val fallbackId = inputDescriptors[0].asJsonObject.get("id").asString
            matchingVCs[fallbackId] = vcList.map { VCWithFormat(it.format, it.vc.deepCopy()) }.toMutableList()
        }

        return MatchResult(
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

            val credential = vcWithFormat.vc

            val credentialJson = gson.toJson(credential)

            val formatMap = result.getOrPut(inputDescriptorId) { mutableMapOf() }
            val credentialList = formatMap.getOrPut(formatType) { mutableListOf() }

            credentialList.add(credentialJson)
        }

        return result.mapValues { (_, innerMap) ->
            innerMap.mapValues { (_, list) -> list.toList() }
        }
    }



    private fun areVCFormatAndProofTypeMatchingRequest(format: JsonObject?, vc: JsonObject): Boolean {
        if (format == null) return false

        val proof = vc.getAsJsonObject("proof") ?: return false
        val proofType = proof.get("type")?.asString ?: ""

        return format.entrySet().any { (type, element) ->
            val inner = element?.asJsonObject
            val acceptedProofs = inner?.getAsJsonArray("proof_type")?.mapNotNull { it.asString } ?: emptyList()
            type == "ldp_vc" && acceptedProofs.contains(proofType)
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


data class MatchResult(
    val matchingVCs: Map<String, List<VCWithFormat>>,
    val requestedClaims: String,
    val purpose: String
)



fun extractWalletMetadata(allProperties: JsonObject?): WalletMetadata {
    val hardcodedMetadataJson = """
    {
      "presentation_definition_uri_supported": true,
      "vp_formats_supported": {
        "ldp_vc": {
          "alg_values_supported": [
            "Ed25519Signature2018",
            "Ed25519Signature2020",
            "RSASignature2018"
          ]
        },
        "mso_mdoc": {
          "alg_values_supported": ["ES256"]
        }
      },
      "client_id_schemes_supported": ["redirect_uri", "did", "pre-registered"],
      "request_object_signing_alg_values_supported": ["EdDSA"],
      "authorization_encryption_alg_values_supported": ["ECDH-ES"],
      "authorization_encryption_enc_values_supported": ["A256GCM"]
    }
    """.trimIndent()

    val objectMapper = jacksonObjectMapper()

    return try {
        val walletMetadataJson = allProperties
            ?.getAsJsonPrimitive("walletMetadata")
            ?.asString

        if (!walletMetadataJson.isNullOrBlank()) {
            objectMapper.readValue(walletMetadataJson)
        } else {
            objectMapper.readValue(hardcodedMetadataJson)
        }
    } catch (e: Exception) {
        Log.e("WalletMetadata", "Failed to parse walletMetadata, returning hardcoded.", e)
        objectMapper.readValue(hardcodedMetadataJson)
    }
}


fun isClientValidationRequired(allProperties: JsonObject?): Boolean {
    return try {
        allProperties
            ?.getAsJsonPrimitive("openid4vpClientValidation")
            ?.asString
            ?.equals("true", ignoreCase = true) == true
    } catch (e: Exception) {
        false
    }
}


