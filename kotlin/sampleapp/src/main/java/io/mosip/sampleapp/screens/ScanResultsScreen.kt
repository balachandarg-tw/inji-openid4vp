package io.mosip.sampleapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.mosip.sampleapp.data.SharedViewModel

@Composable
fun ScanResultScreen(sharedViewModel: SharedViewModel) {
    val matches = sharedViewModel.findMatchingCredentials()

    if (matches.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("No matching credentials found.")
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(matches) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Type: ${item.getAsJsonArray("type")?.get(1)?.asString ?: "Unknown"}")
                        Text("Preview: ${item.toString().take(80)}...")
                    }
                }
            }
        }
    }
}
