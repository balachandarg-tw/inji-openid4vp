package io.mosip.sampleapp.data.model

import com.google.gson.annotations.SerializedName

data class Verifier(
    @SerializedName("client_id") val clientId: String,
    @SerializedName("redirect_uris") val redirectUris: List<String>,
    @SerializedName("response_uris") val responseUris: List<String>
)

data class VerifierResponse(
    @SerializedName("response") val response: VerifierWrapper,
    @SerializedName("errors") val errors: List<Any>
)

data class VerifierWrapper(
    @SerializedName("verifiers") val verifiers: List<Verifier>
)
