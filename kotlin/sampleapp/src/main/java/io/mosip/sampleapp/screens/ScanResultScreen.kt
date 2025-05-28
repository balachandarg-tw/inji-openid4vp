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
fun ScanResultScreen(sharedViewModel: SharedViewModel, navController: NavHostController) {
    val matches = sharedViewModel.findMatchingCredentials()
    val selectedItems = remember { mutableStateListOf<JsonObject>() }

    var showConsentDialog by remember { mutableStateOf(false) }
    var showDeclineConfirmationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with Close icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { navController.popBackStack(Screen.QrScanner.route, inclusive = false) }) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Text("Matched Credentials:", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        // Credential list
        if (matches.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(matches) { credential ->
                    val isSelected = selectedItems.contains(credential)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (isSelected) selectedItems.remove(credential)
                                else selectedItems.add(credential)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (it) selectedItems.add(credential)
                                    else selectedItems.remove(credential)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = credential.toString(),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        } else {
            Text("No matching credentials found.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vertical buttons at bottom
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
                onClick = {
                    // Handle reject logic if needed
                }
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