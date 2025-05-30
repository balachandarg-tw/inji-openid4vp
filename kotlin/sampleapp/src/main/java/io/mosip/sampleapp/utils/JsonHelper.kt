package io.mosip.sampleapp.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.mosip.sampleapp.vc.SampleVcJson
import org.json.JSONObject

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

fun <T> dataClassToJsonObject(data: T): JSONObject {
    val gson = Gson()
    val jsonString = gson.toJson(data)
    return JSONObject(jsonString)
}
