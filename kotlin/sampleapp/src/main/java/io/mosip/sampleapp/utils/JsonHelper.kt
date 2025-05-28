package io.mosip.sampleapp.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.mosip.sampleapp.vc.SampleVcJson

class JsonHelper {
    fun getJsonObjects(): List<JsonObject> {
        val gson = Gson()
        return listOf(
            gson.fromJson(SampleVcJson.MOSIP_VC, JsonObject::class.java),
            gson.fromJson(SampleVcJson.INSURANCE_VC, JsonObject::class.java),
            gson.fromJson(SampleVcJson.MOCK_VC, JsonObject::class.java)
        )
    }
}
