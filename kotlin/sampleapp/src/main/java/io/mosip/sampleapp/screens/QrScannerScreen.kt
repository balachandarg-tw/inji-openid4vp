import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.mosip.openID4VP.OpenID4VP
import io.mosip.sampleapp.OVPHelper
import io.mosip.sampleapp.OpenID4VPManager
import io.mosip.sampleapp.data.SharedViewModel
import io.mosip.sampleapp.getWalletMetadata
import io.mosip.sampleapp.isClientValidationRequired
import io.mosip.sampleapp.utils.dataClassToJsonObject
import io.mosip.sampleapp.vc.SampleVcJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(navController: NavHostController, sharedViewModel: SharedViewModel) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    when {
        hasCameraPermission -> {
            CameraPreviewAndScanner(sharedViewModel, navController)
        }
        else -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required to scan QR codes")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}


@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewAndScanner(
    sharedViewModel: SharedViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    var scannedText by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var scanningEnabled by remember { mutableStateOf(true) }
    val verifiers = sharedViewModel.verifiers
    LaunchedEffect(scannedText) {
        scannedText?.let { urlEncodedAuthRequest ->
            sharedViewModel.updateScannedQr(urlEncodedAuthRequest)



            try {

                val authorizationRequest = withContext(Dispatchers.IO) {
                    OpenID4VPManager.instance.authenticateVerifier(
                        urlEncodedAuthorizationRequest = urlEncodedAuthRequest,
                        sharedViewModel.verifiers,
                        walletMetadata = getWalletMetadata(sharedViewModel.allProperties),
                        isClientValidationRequired(sharedViewModel.allProperties)
                    )
                }
                val gson = Gson()
                val vcJson = sharedViewModel.items
                val vcJsonList = listOf(vcJson)
                val authRequestJson: JsonObject = gson.toJsonTree(authorizationRequest).asJsonObject

                Log.d(":::::::authrequest", "$authRequestJson")


                val matchingVcsResult = OVPHelper().getVcsMatchingAuthRequest(vcJson, authRequestJson)
                Log.d("::::::", "matching Vcs: $matchingVcsResult")

                val prettyGson = GsonBuilder().setPrettyPrinting().create()
                val prettyJson = prettyGson.toJson(matchingVcsResult)
                Log.d("::::::pretty", "$prettyJson")


                sharedViewModel.storeMatchResult(matchingVcsResult)

                delay(100)

                val hasMatchingVCs = matchingVcsResult.matchingVCs.values.any { it.isNotEmpty() }

                if (hasMatchingVCs) {
                    navController.navigate("scan_result")
                }
                else {
                    Log.d("CameraScanner", "No matching credentials found")
                    showErrorDialog = true
                    scanningEnabled = false
                }

            } catch (e: Exception) {
                Log.e("CameraScanner", "Library processing failed", e)
                showErrorDialog = true
                scanningEnabled = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(factory = { ctx ->
            val previewView = androidx.camera.view.PreviewView(ctx)

            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val barcodeScanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )

            val analysisUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysisUseCase.setAnalyzer(executor) { imageProxy ->
                if (!scanningEnabled) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                barcode.rawValue?.let { value ->
                                    if (scannedText != value) {
                                        Log.d("QrScanner", "QR Code scanned: $value")
                                        Toast.makeText(context, "Scanned: $value", Toast.LENGTH_SHORT).show()
                                        scannedText = value
                                        scanningEnabled = false
                                    }
                                }
                            }
                        }
                        .addOnFailureListener {
                            Log.e("QrScanner", "Barcode scanning failed", it)
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner,
                    cameraSelector,
                    preview,
                    analysisUseCase
                )
            } catch (e: Exception) {
                Log.e("QrScanner", "Use case binding failed", e)
            }

            previewView
        })

        // Full-Screen Error Dialog
        if (showErrorDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.95f))
                    .clickable(enabled = false) {}, // Block interaction with background
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Invalid QR Code",
                        style = MaterialTheme.typography.h5,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No matching credential found for the scanned QR code.",
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            showErrorDialog = false
                            scanningEnabled = true
                            scannedText = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}





