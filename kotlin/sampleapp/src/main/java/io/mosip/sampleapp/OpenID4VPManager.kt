package io.mosip.sampleapp

import com.google.gson.JsonObject
import io.mosip.openID4VP.OpenID4VP
import io.mosip.openID4VP.authorizationRequest.AuthorizationRequest
import io.mosip.openID4VP.authorizationRequest.Verifier
import io.mosip.openID4VP.authorizationResponse.unsignedVPToken.UnsignedVPToken
import io.mosip.openID4VP.authorizationResponse.vpTokenSigningResult.VPTokenSigningResult
import io.mosip.openID4VP.constants.FormatType

object OpenID4VPManager {
    private var _instance: OpenID4VP? = null
    val instance: OpenID4VP
        get() = _instance ?: throw IllegalStateException("OpenID4VP is not initialized")

    fun init(traceabilityId: String) {
        _instance = OpenID4VP(traceabilityId)
    }

    fun authenticateVerifier(
        urlEncodedAuthRequest: String,
        trustedVerifiers: List<Verifier>,
        allProperties: JsonObject?
    ): AuthorizationRequest {
        val walletMetadata = extractWalletMetadata(allProperties)
        val validateClient = isClientValidationRequired(allProperties)

        return instance.authenticateVerifier(
            urlEncodedAuthorizationRequest = urlEncodedAuthRequest,
            trustedVerifiers = trustedVerifiers,
            walletMetadata = walletMetadata,
            shouldValidateClient = validateClient
        )
    }

    fun constructUnsignedVpToken(selectedCredentials : Map<String, Map<FormatType, List<String>>>): Map<FormatType, UnsignedVPToken> {
        return instance.constructUnsignedVPToken(selectedCredentials)
    }

    fun shareVerifiablePresentation(signedVpToken: Map<FormatType, VPTokenSigningResult>): String {
        return instance.shareVerifiablePresentation(signedVpToken);
    }

    fun sendErrorToVerifier(errorMessage: String) {
        return instance.sendErrorToVerifier(Exception(errorMessage))
    }
}

