package io.mosip.sampleapp

import io.mosip.openID4VP.authorizationRequest.AuthorizationRequest
import io.mosip.openID4VP.authorizationRequest.presentationDefinition.Constraints
import io.mosip.sampleapp.vc.SampleVcJson

class OVPHelper {
    fun getVcsMatchingAuthRequest(
        authorizationRequest: AuthorizationRequest,
        vcs: List<Any> = listOf(SampleVcJson.MOCK_VC, SampleVcJson.MOSIP_VC, SampleVcJson.INSURANCE_VC)
    ): Map<String, Any> {

        val matchingVCs = mutableMapOf<String, MutableList<Any>>()
        val requestedClaimsByVerifier = mutableSetOf<String>()

        val presentationDefinition = authorizationRequest.presentationDefinition
        val inputDescriptors = presentationDefinition.inputDescriptors

        var hasFormatOrConstraints = false

        vcs.forEach { vc ->
            inputDescriptors.forEach { inputDescriptor ->
                val format = inputDescriptor.format ?: presentationDefinition.format

                hasFormatOrConstraints = hasFormatOrConstraints ||
                        (format != null) ||
                        (inputDescriptor.constraints?.fields != null)

                val areMatchingFormatAndProofType = areVCFormatAndProofTypeMatchingRequest(format, vc)

                if (!areMatchingFormatAndProofType) {
                    return@forEach
                }

                val isMatchingConstraints = isVCMatchingRequestConstraints(
                    inputDescriptor.constraints,
                    vc,
                    requestedClaimsByVerifier
                )

                val hasFields = inputDescriptor.constraints?.fields != null

                val shouldInclude = if (hasFields && format != null) {
                    isMatchingConstraints && areMatchingFormatAndProofType
                } else {
                    isMatchingConstraints || areMatchingFormatAndProofType
                }

                if (shouldInclude) {
                    val id = inputDescriptor.id
                    if (!matchingVCs.containsKey(id)) {
                        matchingVCs[id] = mutableListOf()
                    }
                    matchingVCs[id]?.add(vc)
                }
            }
        }

        if (!hasFormatOrConstraints && inputDescriptors.isNotEmpty()) {
            val id = inputDescriptors[0].id
            matchingVCs[id] = vcs.toMutableList()
        }

        if (matchingVCs.isEmpty()) {
            throw Exception("No matching VCs found") // Or call error handler
        }

        val requestedClaims = requestedClaimsByVerifier.joinToString(",")

        return mapOf(
            "matchingVCs" to matchingVCs,
            "requestedClaims" to requestedClaims,
            "purpose" to (presentationDefinition.purpose ?: "")
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun areVCFormatAndProofTypeMatchingRequest(format: Map<String, Any>?, vc: Any): Boolean {
        if (format == null) return false

        val proofTypes = (format["ldp_vc"] as? Map<String, Any?>)?.get("proof_type") as? List<*>
            ?: return false

        val vcMap = vc as? Map<String, Any?> ?: return false

        val proof = vcMap["proof"] as? Map<String, Any?> ?: return false
        val proofType = proof["type"] as? String ?: return false

        return proofTypes.contains(proofType)
    }


    @Suppress("UNCHECKED_CAST")
    fun isVCMatchingRequestConstraints(
        constraints: Constraints?,
        vc: Any,
        requestedClaimsByVerifier: MutableSet<String>
    ): Boolean {
        val fields = constraints?.fields ?: return true

        val vcMap = vc as? Map<String, Any?> ?: return false
        val credentialSubject = vcMap["credentialSubject"] as? Map<String, Any?> ?: return false

        return fields.all { field ->
            field.path.any { path ->
                // Extract claim name from JSON path, assuming path like "$.credentialSubject.email"
                val claimName = path.substringAfterLast(".")

                requestedClaimsByVerifier.add(claimName)

                val vcValue = credentialSubject[claimName] ?: return@any false

                val filter = field.filter
                if (filter?.pattern != null && vcValue is String) {
                    Regex(filter.pattern).matches(vcValue)
                } else {
                    true
                }
            }
        }
    }


}