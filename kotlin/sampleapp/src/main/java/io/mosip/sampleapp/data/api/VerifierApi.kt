package io.mosip.sampleapp.data.api

import io.mosip.sampleapp.data.model.VerifierResponse
import retrofit2.Response
import retrofit2.http.GET

interface VerifierApi {
    @GET("v1/mimoto/verifiers")
    suspend fun getVerifiers(): Response<VerifierResponse>
}