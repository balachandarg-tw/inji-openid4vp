package io.mosip.sampleapp

import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonObject
import com.jayway.jsonpath.JsonPath
import io.mosip.openID4VP.authorizationRequest.WalletMetadata
import org.json.JSONArray
import org.json.JSONObject

class OVPHelper {
    fun getVcsMatchingAuthRequest(
        vcJsonList: List<JSONObject>,
        authRequest: JSONObject
    ): MatchResult {
        val matchingVCs = mutableMapOf<String, MutableList<JSONObject>>()
        val requestedClaims = mutableSetOf<String>()
        val presentationDefinition = authRequest.getJSONObject("presentationDefinition")
        val inputDescriptors = presentationDefinition.getJSONArray("inputDescriptors")
        var hasFormatOrConstraints = false

        for (vc in vcJsonList) {
            for (i in 0 until inputDescriptors.length()) {
                val inputDescriptor = inputDescriptors.getJSONObject(i)
                val format = inputDescriptor.optJSONObject("format")
                    ?: presentationDefinition.optJSONObject("format")
                val constraints = inputDescriptor.optJSONObject("constraints")

                hasFormatOrConstraints = hasFormatOrConstraints ||
                        format != null || constraints?.has("fields") == true

                val matchesFormat = areVCFormatAndProofTypeMatchingRequest(format, vc)
              //  val matchesConstraints = isVCMatchingRequestConstraints(constraints, vc, requestedClaims)

                val shouldInclude = if (constraints?.has("fields") == true && format != null) {
                    matchesFormat
                } else {
                    matchesFormat
                }

                if (shouldInclude) {
                    val descriptorId = inputDescriptor.getString("id")
                    matchingVCs.getOrPut(descriptorId) { mutableListOf() }.add(vc)
                }
            }
        }

        if (!hasFormatOrConstraints && inputDescriptors.length() > 0) {
            val fallbackId = inputDescriptors.getJSONObject(0).getString("id")
            matchingVCs[fallbackId] = vcJsonList.toMutableList()
        }

        return MatchResult(
            matchingVCs,
            requestedClaims.joinToString(","),
            presentationDefinition.optString("purpose", "")
        )
    }


    fun areVCFormatAndProofTypeMatchingRequest(format: JSONObject?, vc: JSONObject): Boolean {
        if (format == null) return false

        val proof = vc.optJSONObject("proof") ?: return false
        val proofType = proof.optString("type", "")

        return format.keys().asSequence().any { type ->
            val inner = format.optJSONObject(type)
            val acceptedProofs = inner?.optJSONArray("proof_type")?.toList<String>() ?: emptyList()
            type == "ldp_vc" && acceptedProofs.contains(proofType)
        }
    }



    fun isVCMatchingRequestConstraints(
        constraints: JSONObject?,
        vc: JSONObject,
        requestedClaims: MutableSet<String>
    ): Boolean {
        val fields = constraints?.optJSONArray("fields") ?: return false
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

        for (i in 0 until fields.length()) {
            val field = fields.optJSONObject(i) ?: continue
            val paths = field.optJSONArray("path") ?: continue
            val filter = field.optJSONObject("filter")

            val fieldMatched = (0 until paths.length()).any { pathIndex ->
                val jsonPath = paths.optString(pathIndex)

                // Extract claim name from JSONPath
                val claimName = jsonPath
                    .replace(Regex("\\['([^']+)'\\]"), ".$1")
                    .split('.')
                    .lastOrNull { it.isNotEmpty() && it != "$" } ?: ""
                requestedClaims.add(claimName)

                try {
                    val results = JsonPath.read<Any>(processedCredential.toString(), jsonPath)

                    if (results == null || (results is List<*> && results.isEmpty())) return@any false

                    if (filter == null) return@any true

                    val expectedType = filter.optString("type")
                    results is List<*> && results.any { match -> getJsType(match) == expectedType }
                } catch (e: Exception) {
                    false
                }
            }

            if (!fieldMatched) return false
        }

        return true
    }

    fun fetchCredentialBasedOnFormat(vc: JSONObject): JSONObject? {
        val format = vc.optString("format")
        val verifiableCredential = vc.optJSONObject("verifiableCredential") ?: return null

        return when (format) {
            "ldp_vc" -> verifiableCredential.optJSONObject("credential")
            "mso_mdoc" -> {
                val processedCredential = verifiableCredential.optJSONObject("processedCredential") ?: return null
                getProcessedDataForMdoc(processedCredential)
            }
            else -> null
        }
    }

    fun getProcessedDataForMdoc(processedCredential: JSONObject): JSONObject {
        val issuerSigned = processedCredential.optJSONObject("issuerSigned") ?: return JSONObject()
        val nameSpaces = issuerSigned.optJSONObject("nameSpaces") ?: return JSONObject()

        val processedData = JSONObject()

        val nsKeys = nameSpaces.keys()
        for (nsKey in nsKeys) {
            val elementsArray = nameSpaces.optJSONArray(nsKey) ?: continue
            val asObject = JSONObject()

            for (i in 0 until elementsArray.length()) {
                val item = elementsArray.optJSONObject(i) ?: continue
                val id = item.optString("elementIdentifier")
                val value = item.opt("elementValue")
                asObject.put(id, value)
            }

            processedData.put(nsKey, asObject)
        }

        return processedData
    }




    fun buildSelectedVCsMapPlain(selectedItems: List<JsonObject>): Map<String, Map<String, List<String>>> {
        val rootMap = mutableMapOf<String, MutableMap<String, MutableList<String>>>()

        selectedItems.forEach { vc ->
            val inputDescriptorId = vc["input_descriptor_id"]?.asString ?: return@forEach
            val formatType = "ldp_vc" // You can make this dynamic if needed
            val vcString = vc.toString()

            val formatMap = rootMap.getOrPut(inputDescriptorId) { mutableMapOf() }
            val vcList = formatMap.getOrPut(formatType) { mutableListOf() }

            vcList.add(vcString)
        }

        return rootMap
    }



}

fun <T> JSONArray.toList(): List<T> {
    val result = mutableListOf<T>()
    for (i in 0 until length()) {
        @Suppress("UNCHECKED_CAST")
        result.add(get(i) as T)
    }
    return result
}


data class MatchResult(
    val matchingVCs: Map<String, List<JSONObject>>,
    val requestedClaims: String,
    val purpose: String
)


fun getWalletMetadata(allProperties: JsonObject?): WalletMetadata {
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
