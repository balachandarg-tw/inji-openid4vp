package io.mosip.sampleapp.data.repository

import io.mosip.sampleapp.data.model.Verifier
import io.mosip.sampleapp.data.network.NetworkHelper

class VerifierRepository {
    private val api = NetworkHelper.verifierApi

    suspend fun fetchVerifiers(): List<Verifier>? {
        return try {
            val response = api.getVerifiers()
            if (response.isSuccessful) {
                response.body()?.response?.verifiers
            } else null
        } catch (e: Exception) {
            null
        }
    }
}