package app.cryptoseal.tabs.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * The main UI for the "Scanner" tab.
 * Uses CameraX and Google ML Kit to provide a live QR code scanning experience.
 * Once a package's QR code is detected, it opens a bottom sheet to submit a scan record.
 *
 * @param viewModel The [ScannerViewModel] that handles the scanning state and data submission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerTab(viewModel: ScannerViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State to track camera permission.
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher for camera permission request.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    // Re-check permission whenever the activity resumes (e.g., after returning from settings).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Request permission on first launch if not already granted.
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Scan Package",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (hasCameraPermission) {
                // The live camera view with QR detection logic.
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CameraXLiveScanner(onBarcodeDetected = { viewModel.onOrderIdScanned(it) })
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Align the QR code within the frame.",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                )
            } else {
                // Fallback UI when camera permission is missing.
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Camera permission is required to scan QR codes.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Button(
                        onClick = {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        // Bottom Sheet showing the scan submission form after a QR code is detected.
        if (viewModel.scannedOrderId != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.resetScanner() },
                sheetState = sheetState,
                dragHandle = null
            ) {
                ScanSubmissionForm(viewModel)
            }
        }

        // Overlay shown after a successful scan submission.
        if (viewModel.submitSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { viewModel.resetScanner() },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scan Submitted!", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.resetScanner() }) {
                            Text("Next Package")
                        }
                    }
                }
            }
        }
    }
}

/**
 * The form used to submit package scan details (photo, condition, comment).
 */
@Composable
fun ScanSubmissionForm(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val conditions = listOf("Good", "Missing", "Damaged")

    // Launcher for taking a quick photo for the scan.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.photoBitmap = bitmap
        }
    }

    // Launcher for requesting location permission before submission.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.submitScan()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Order #${viewModel.scannedOrderId}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.resetScanner() }) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Field: Package Condition (Dropdown)
        Text("Package Condition", style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { expanded = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(viewModel.condition)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                conditions.forEach { cond ->
                    DropdownMenuItem(
                        text = { Text(cond) },
                        onClick = {
                            viewModel.condition = cond
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Field: Package Photo Capture
        Text("Package Photo", style = MaterialTheme.typography.labelLarge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { cameraLauncher.launch() },
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.photoBitmap != null) {
                Image(
                    bitmap = viewModel.photoBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Capture Package Photo")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Field: Comment
        OutlinedTextField(
            value = viewModel.comment,
            onValueChange = { viewModel.comment = it },
            label = { Text("Comments") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Display submission errors if any.
        if (viewModel.submitError != null) {
            Text(
                viewModel.submitError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Action: Submit scan with current GPS location.
        Button(
            onClick = {
                val fineLoc = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (fineLoc == PackageManager.PERMISSION_GRANTED) {
                    viewModel.submitScan()
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !viewModel.isSubmitting,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (viewModel.isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Scan with Location")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * A Composable that hosts the CameraX preview and performs real-time QR code detection.
 *
 * @param onBarcodeDetected Callback triggered when a numeric QR code is successfully parsed.
 */
@Composable
fun CameraXLiveScanner(onBarcodeDetected: (Int) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Configure ML Kit barcode scanner specifically for QR codes.
                    val barcodeScanner = BarcodeScanning.getClient(
                        com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                            .build()
                    )

                    // Setup real-time image analysis.
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy, onBarcodeDetected)
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        Log.e("ScannerTab", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Draw a decorative overlay with brackets to guide the user.
        ScannerOverlay()
    }
}

/**
 * Analyzes a camera frame for QR codes using ML Kit.
 */
@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (Int) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (rawValue != null) {
                        // We expect the QR code to contain a numeric Order ID.
                        val orderId = rawValue.toIntOrNull()
                        if (orderId != null) {
                            onBarcodeDetected(orderId)
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("ScannerTab", "Barcode scanning failed", it)
            }
            .addOnCompleteListener {
                // Important: Close the proxy so the next frame can be analyzed.
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * A custom Canvas overlay that draws a dim background and clear center rectangle
 * with corner brackets to signify the scanning area.
 */
@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val rectSize = canvasWidth * 0.7f
        val left = (canvasWidth - rectSize) / 2
        val top = (canvasHeight - rectSize) / 2

        val cornerRadius = 40f
        val lineLength = 80f
        val strokeWidth = 12f
        val bracketColor = Color(0xFF03DAC5)

        // Draw the semi-transparent overlay everywhere.
        drawRect(Color(0x99000000))

        // "Cut out" the center square using Clear blend mode.
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(rectSize, rectSize),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            blendMode = BlendMode.Clear
        )

        // Draw corner brackets around the center square.
        val path = Path().apply {
            // Top-left
            moveTo(left, top + lineLength)
            lineTo(left, top + cornerRadius)
            arcTo(
                Rect(left, top, left + cornerRadius * 2, top + cornerRadius * 2),
                180f,
                90f,
                false
            )
            lineTo(left + lineLength, top)

            // Top-right
            moveTo(left + rectSize - lineLength, top)
            lineTo(left + rectSize - cornerRadius, top)
            arcTo(
                Rect(
                    left + rectSize - cornerRadius * 2,
                    top,
                    left + rectSize,
                    top + cornerRadius * 2
                ), 270f, 90f, false
            )
            lineTo(left + rectSize, top + lineLength)

            // Bottom-right
            moveTo(left + rectSize, top + rectSize - lineLength)
            lineTo(left + rectSize, top + rectSize - cornerRadius)
            arcTo(
                Rect(
                    left + rectSize - cornerRadius * 2,
                    top + rectSize - cornerRadius * 2,
                    left + rectSize,
                    top + rectSize
                ), 0f, 90f, false
            )
            lineTo(left + rectSize - lineLength, top + rectSize)

            // Bottom-left
            moveTo(left + lineLength, top + rectSize)
            lineTo(left + cornerRadius, top + rectSize)
            arcTo(
                Rect(
                    left,
                    top + rectSize - cornerRadius * 2,
                    left + cornerRadius * 2,
                    top + rectSize
                ), 90f, 90f, false
            )
            lineTo(left, top + rectSize - lineLength)
        }

        drawPath(
            path = path,
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
