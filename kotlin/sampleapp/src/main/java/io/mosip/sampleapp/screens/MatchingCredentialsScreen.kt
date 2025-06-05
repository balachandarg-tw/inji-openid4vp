package io.mosip.sampleapp.screens

import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.mosip.openID4VP.authorizationResponse.vpTokenSigningResult.VPTokenSigningResult
import io.mosip.openID4VP.authorizationResponse.vpTokenSigningResult.types.ldp.LdpVPTokenSigningResult
import io.mosip.openID4VP.constants.FormatType
import io.mosip.sampleapp.Constants
import io.mosip.sampleapp.KeyType
import io.mosip.sampleapp.OpenID4VPManager
import io.mosip.sampleapp.Screen
import io.mosip.sampleapp.SignedVPJWT
import io.mosip.sampleapp.VPTokenSigner
import io.mosip.sampleapp.data.SharedViewModel
import io.mosip.sampleapp.vc.SampleVcJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MatchingCredentialsScreen(
    sharedViewModel: SharedViewModel,
    navController: NavHostController
) {
    val matchResult by sharedViewModel.matchResult.collectAsState()
    val selectedItems = remember { mutableStateListOf<Pair<String, JsonObject>>() }

    var showConsentDialog by remember { mutableStateOf(false) }
    var showDeclineConfirmationDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            val coroutineScope = rememberCoroutineScope()

            IconButton(onClick = {
                coroutineScope.launch(Dispatchers.IO) {

                    OpenID4VPManager.sendErrorToVerifier(Constants.ERR_DECLINED)

                    withContext(Dispatchers.Main) {
                        navController.popBackStack(Screen.Share.route, inclusive = false)
                    }
                }
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
                            val credential = vc.getAsJsonObject("verifiableCredential")
                                ?.getAsJsonObject("credential")

                            val typeArray = credential?.getAsJsonArray("type")
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
                onClick = {
                        showConsentDialog = true

              },
                enabled = selectedItems.isNotEmpty()
            ) {
                Text("Share")
            }

            Spacer(modifier = Modifier.height(8.dp))



            TextButton(onClick = {
                handleDecline(coroutineScope) {
                    navController.popBackStack(Screen.Share.route, inclusive = false)
                }
            }) {
                Text("Reject", color = Color.Red)
            }
        }
    }


    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false},
            title = { Text("Consent Required") },
            text = { Text("Do you want to share selected credentials?") },
            confirmButton = {
                TextButton(onClick = {
                    showConsentDialog = false
                    coroutineScope.launch {
                        print(":::::: $selectedItems")
                        testSigning()
                    }
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
            onDismissRequest = { showDeclineConfirmationDialog = false },
            title = { Text("Are you sure?") },
            text = { Text("Do you want to go back to scanning?") },
            confirmButton = {
                TextButton(onClick = {
                    handleDecline(coroutineScope) {
                        showDeclineConfirmationDialog = false
                        navController.popBackStack(Screen.Share.route, inclusive = false)

                    }
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

fun handleDecline(
    coroutineScope: CoroutineScope,
    onDeclineConfirmed: () -> Unit
) {
    coroutineScope.launch(Dispatchers.IO) {
        OpenID4VPManager.sendErrorToVerifier(Constants.ERR_DECLINED)
        withContext(Dispatchers.Main) {
            onDeclineConfirmed()
        }
    }
}




fun constructUnsignedVpToken() {

    val selectedLdpCredentialsList = mapOf(
        "id card credential" to mapOf(
            FormatType.LDP_VC to listOf(
                SampleVcJson.MOSIP_VC
            )
        )
    )
    val unsignedVpToken = OpenID4VPManager.instance.constructUnsignedVPToken(selectedLdpCredentialsList)
    println("======unsignedVpToken$unsignedVpToken")
}

suspend fun testSigning() = withContext(Dispatchers.IO) {
    val selectedLdpCredentialsList = mapOf(
        "id card credential" to mapOf(
            FormatType.LDP_VC to listOf(SampleVcJson.MOSIP_VC)
        )
    )

    val unsignedVpTokenMap = OpenID4VPManager.instance.constructUnsignedVPToken(selectedLdpCredentialsList)
    val vpPayload = unsignedVpTokenMap[FormatType.LDP_VC] ?: run {
        println("No LDP_VC payload found")
        return@withContext
    }

    val gson = Gson()
    val jsonElement = gson.toJsonTree(vpPayload)
    val mapPayload: Map<String, Any> = gson.fromJson(
        jsonElement,
        object : TypeToken<Map<String, Any>>() {}.type
    )

    val keyType = KeyType.RSA
    val keyPair = VPTokenSigner.generateKeyPair(keyType)
    val result: SignedVPJWT = VPTokenSigner.signVPToken(keyPair, keyType, mapPayload)

    val ldpSigningResult = LdpVPTokenSigningResult(
        jws = result.jwt,
        signatureAlgorithm = result.algorithm,
        publicKey = result.publicJWK,
        domain = "OpenID4VP"
    )

    val vpTokenSigningResultMap: Map<FormatType, VPTokenSigningResult> = mapOf(
        FormatType.LDP_VC to ldpSigningResult
    )

    try {
        val finalResponse = OpenID4VPManager.instance.shareVerifiablePresentation(vpTokenSigningResultMap)
        Log.d("VP_SHARE", "######## $finalResponse")
    } catch (e: Exception) {
        Log.e("VP_SHARE", "Error sharing VP", e)
    }

    Log.d("VP_SIGNED", "Signed JWT: ${result.jwt}")
    Log.d("VP_SIGNED", "Public JWK: ${result.publicJWK}")
    Log.d("VP_SIGNED", "Alg Used: ${result.algorithm}")
}


