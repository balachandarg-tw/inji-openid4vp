package io.mosip.sampleapp.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.mosip.sampleapp.Screen
import io.mosip.sampleapp.data.SharedViewModel

@Composable
fun HomeScreen(navController: NavHostController, viewModel: SharedViewModel) {
    var showFabMenu by remember { mutableStateOf(false) }
    var expandedRowIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadVerifiers()
    }

    val verifiers = viewModel.verifiers

    // Print the verifiers directly in the UI (for debug purposes)
    LaunchedEffect(verifiers) {
        verifiers.forEach { verifier ->
            Log.d("HomeScreen", "Client ID: ${verifier.clientId}")
            Log.d("HomeScreen", "Redirect URIs: ${verifier.redirectUris}")
            Log.d("HomeScreen", "Response URIs: ${verifier.responseUris}")
        }
    }
    val items = viewModel.items

    Box(Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No items found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items) { index, jsonObj ->
                    Card(
                        elevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectItem(jsonObj)
                                navController.navigate(Screen.Details.route)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val typeArray = jsonObj.getAsJsonArray("type")
                            val typeLabel = if (typeArray != null && typeArray.size() > 1) {
                                typeArray[1].asString
                            } else "Unnamed"

                            Text(typeLabel, style = MaterialTheme.typography.body1)

                            Box {
                                IconButton(onClick = {
                                    expandedRowIndex = if (expandedRowIndex == index) null else index
                                }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                }

                                DropdownMenu(
                                    expanded = expandedRowIndex == index,
                                    onDismissRequest = { expandedRowIndex = null }
                                ) {
                                    DropdownMenuItem(onClick = {
                                        viewModel.removeItem(index)
                                        expandedRowIndex = null
                                    }) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB dropdown with custom labels
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    // Use availableCredentials from ViewModel with labels
                    viewModel.availableCredentials.forEach { (label, credential) ->
                        ExtendedFloatingActionButton(
                            text = { Text(label) },
                            onClick = {
                                viewModel.addItem(credential.deepCopy()) // Avoid shared ref
                                showFabMenu = false
                            },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    }
}