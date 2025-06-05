package io.mosip.sampleapp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.mosip.openID4VP.authorizationRequest.Verifier
import io.mosip.sampleapp.MatchResult
import io.mosip.sampleapp.data.repository.AllPropertiesRepository
import io.mosip.sampleapp.data.repository.VerifierRepository
import io.mosip.sampleapp.vc.SampleVcJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SharedViewModel : ViewModel() {

    val issuersList = listOf(
        "Download Mosip" to SampleVcJson.get(0),
        "Download Insurance" to SampleVcJson.get(1),
        "Download Mock" to SampleVcJson.get(2)
    )


    private val _downloadedVcs = mutableStateListOf<JsonObject>()
    val downloadedVcs: List<JsonObject> get() = _downloadedVcs

    fun addVC(item: JsonObject) {
        _downloadedVcs.add(item)
    }

    fun removeVC(index: Int) {
        _downloadedVcs.removeAt(index)
    }

    var scannedQr: String? by mutableStateOf(null)
        private set

    fun updateScannedQr(data: String) {
        scannedQr = data
    }




    private val _matchResult = MutableStateFlow<MatchResult?>(null)
    val matchResult: StateFlow<io.mosip.sampleapp.MatchResult?> = _matchResult

    fun storeMatchResult(result: io.mosip.sampleapp.MatchResult) {
        viewModelScope.launch {
            _matchResult.value = result
        }
    }




    var vcSelectedForDetails : JsonObject? = null
        private set

    fun displayVcDetails(item: JsonObject) {
        vcSelectedForDetails = item
    }






    private val repository = VerifierRepository()

    // Store verifiers as Gson JsonObjects first
    var verifiersJson by mutableStateOf<List<JsonObject>>(emptyList())
        private set

    // Store mapped domain model list for easy usage in UI
    var verifiers by mutableStateOf<List<Verifier>>(emptyList())
        private set

    fun loadVerifiers() {
        viewModelScope.launch {
            repository.fetchVerifiers()?.let { jsonList ->
                verifiersJson = jsonList
                verifiers = jsonList.map { mapJsonObjectToVerifier(it) }
            }
        }
    }

    private fun mapJsonObjectToVerifier(jsonObject: JsonObject): Verifier {
        val gson = Gson()
        return gson.fromJson(jsonObject, Verifier::class.java)
    }



    var allProperties by mutableStateOf<JsonObject?>(null)
        private set

    private val allPropertiesRepository = AllPropertiesRepository()

    fun loadAllProperties() {
        viewModelScope.launch {
            val result = allPropertiesRepository.fetchAllProperties()
            allProperties = result
        }
    }

}


