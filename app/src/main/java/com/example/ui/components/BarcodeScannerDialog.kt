package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GraphiteBackground
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.MonoDataLarge
import com.example.ui.theme.MonoDataSmall
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerDialog(
    title: String = "Escanear Código de Barras",
    subtitle: String = "Apunta la cámara al código del producto",
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            if (hasCameraPermission) {
                CameraPreviewScanner(
                    modifier = Modifier.fillMaxSize(),
                    onBarcodeFound = { code ->
                        onBarcodeScanned(code)
                    }
                )

                // Overlay UI with Viewfinder
                ScannerOverlay(
                    title = title,
                    subtitle = subtitle,
                    onClose = onDismiss
                )
            } else {
                // Permission Denied / Request Fallback UI
                CameraPermissionDeniedCard(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onClose = onDismiss
                )
            }
        }
    }
}

@Composable
fun CameraPreviewScanner(
    modifier: Modifier = Modifier,
    onBarcodeFound: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var lastScannedCode by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableStateOf(0L) }

    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(
                        scanner = scanner,
                        imageProxy = imageProxy,
                        onSuccess = { barcodes ->
                            for (barcode in barcodes) {
                                val rawVal = barcode.rawValue?.trim()
                                if (!rawVal.isNullOrBlank()) {
                                    val now = System.currentTimeMillis()
                                    // Debounce 1.5s for same code
                                    if (rawVal != lastScannedCode || now - lastScannedTime > 1500L) {
                                        lastScannedCode = rawVal
                                        lastScannedTime = now
                                        previewView.post {
                                            onBarcodeFound(rawVal)
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    )
                }

                try {
                    cameraProvider.unbindAll()
                    val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        null
                    }

                    if (selector != null) {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalysis
                        )
                    } else {
                        Log.w("CameraPreviewScanner", "No camera device found on current hardware/emulator.")
                    }
                } catch (e: Exception) {
                    Log.e("CameraPreviewScanner", "Camera bind failed: ${e.message}", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (List<Barcode>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onSuccess(barcodes)
                }
            }
            .addOnFailureListener { e ->
                Log.e("BarcodeScanner", "Barcode analysis error: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Composable
private fun ScannerOverlay(
    title: String,
    subtitle: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ElectricLime
                    )
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GraphiteSurface.copy(alpha = 0.8f))
                    .testTag("btn_close_scanner")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar Escáner",
                    tint = Color.White
                )
            }
        }

        // Center Viewfinder Target
        Box(
            modifier = Modifier
                .size(280.dp, 200.dp)
                .border(2.dp, ElectricLime, RoundedCornerShape(16.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            // Scanner Laser Bar effect
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(2.dp)
                    .background(ElectricLime.copy(alpha = 0.85f))
            )

            // Scanning indicator icon
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = ElectricLime.copy(alpha = 0.35f),
                modifier = Modifier.size(64.dp)
            )
        }

        // Bottom Help Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            color = GraphiteSurface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Detecta automáticamente códigos EAN, UPC, Code 128 y QR. Mantén el código centrado.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionDeniedCard(
    onRequestPermission: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ElectricLime.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = "Permiso de Cámara Requerido",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Para escanear códigos de barras de repuestos y productos en tiempo real, la aplicación necesita acceso a la cámara de tu dispositivo.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricLime,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Conceder Permiso de Cámara", fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cerrar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
