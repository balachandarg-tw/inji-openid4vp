package io.mosip.sampleapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.gson.JsonObject
import io.mosip.sampleapp.Screen
import io.mosip.sampleapp.data.SharedViewModel

@Composable
fun ScanResultScreen(
    sharedViewModel: SharedViewModel,
    navController: NavHostController
) {
    val matchResult by sharedViewModel.matchResult.collectAsState()
    val selectedItems = remember { mutableStateListOf<Pair<String, JsonObject>>() }

    var showConsentDialog by remember { mutableStateOf(false) }
    var showDeclineConfirmationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = {
                navController.popBackStack(Screen.QrScanner.route, inclusive = false)
            }) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Text("Requested Claims: ${matchResult?.requestedClaims ?: "N/A"}", style = MaterialTheme.typography.body1)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Purpose: ${matchResult?.purpose ?: "N/A"}", style = MaterialTheme.typography.body2)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Matching Credentials:", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        if (matchResult?.matchingVCs?.isNotEmpty() == true) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                matchResult!!.matchingVCs.entries.forEach { entry ->
                    val key = entry.key
                    val vcList = entry.value

                    items(vcList) { vc ->
                        val vcItem = key to vc
                        val isSelected = selectedItems.contains(vcItem)

                        val typeLabel = runCatching {
                            val typeArray = vc["type"]?.asJsonArray
                            if (typeArray != null && typeArray.size() > 1) {
                                typeArray[1].asString
                            } else {
                                "Unnamed"
                            }
                        }.getOrElse {
                            "Unnamed"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (isSelected) selectedItems.remove(vcItem)
                                    else selectedItems.add(vcItem)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (it) selectedItems.add(vcItem)
                                        else selectedItems.remove(vcItem)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = typeLabel,
                                    style = MaterialTheme.typography.body1,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text("No matching credentials found.", style = MaterialTheme.typography.body2)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { showConsentDialog = true },
                enabled = selectedItems.isNotEmpty()
            ) {
                Text("Share")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { showDeclineConfirmationDialog = true }
            ) {
                Text("Reject", color = Color.Red)
            }
        }
    }

    // Consent Dialog
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Consent Required") },
            text = { Text("Do you want to share selected credentials?") },
            confirmButton = {
                TextButton(onClick = {
                    showConsentDialog = false
                    navController.navigate(Screen.Success.route)
                }) {
                    Text("Yes, Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConsentDialog = false
                    showDeclineConfirmationDialog = true
                }) {
                    Text("Decline")
                }
            }
        )
    }

    // Decline Confirmation Dialog
    if (showDeclineConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Are you sure?") },
            text = { Text("Do you want to go back to scanning?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeclineConfirmationDialog = false
                    navController.popBackStack(Screen.QrScanner.route, inclusive = false)
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeclineConfirmationDialog = false
                    showConsentDialog = true
                }) {
                    Text("Go Back")
                }
            }
        )
    }
}

