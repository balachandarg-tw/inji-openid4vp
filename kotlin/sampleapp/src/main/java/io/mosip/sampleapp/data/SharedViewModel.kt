package io.mosip.sampleapp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import io.mosip.sampleapp.data.model.Verifier
import io.mosip.sampleapp.data.repository.VerifierRepository
import io.mosip.sampleapp.vc.SampleVcJson
import kotlinx.coroutines.launch

class SharedViewModel : ViewModel() {
    private val _items = mutableStateListOf<JsonObject>()
    val items: List<JsonObject> get() = _items

    var scannedQr: String? by mutableStateOf(null)
        private set

    val availableCredentials = listOf(
        "Add Mosip" to SampleVcJson.get(0),
        "Add Insurance" to SampleVcJson.get(1),
        "Add Mock" to SampleVcJson.get(2)
    )

    var downloadedVcs: JsonObject? = null
        private set

    fun selectItem(item: JsonObject) {
        downloadedVcs = item
    }

    fun addItem(item: JsonObject) {
        _items.add(item)
    }

    fun removeItem(index: Int) {
        _items.removeAt(index)
    }

    fun clearItems() {
        _items.clear()
    }

    fun updateScannedQr(data: String) {
        scannedQr = data
    }

    fun findMatchingCredentials(): List<JsonObject> {
        val qr = scannedQr ?: return emptyList()
        return items.filter { json ->
            val typeArray = json["type"]?.asJsonArray
            val hasMosipType = typeArray?.any { it.asString == "MosipVerifiableCredential" } == true
            val containsValid = qr.contains("valid", ignoreCase = true)
            hasMosipType && containsValid
        }
    }


    private val repository = VerifierRepository()
    var verifiers by mutableStateOf<List<Verifier>>(emptyList())
        private set

    fun loadVerifiers() {
        viewModelScope.launch {
            repository.fetchVerifiers()?.let {
                verifiers = it
            }
        }
    }
}


