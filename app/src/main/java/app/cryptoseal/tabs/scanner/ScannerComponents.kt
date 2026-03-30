package app.cryptoseal.tabs.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScanSubmissionForm(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val conditions = listOf("Good", "Missing", "Damaged")

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) viewModel.photoBitmap = bitmap
        }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) viewModel.submitScan()
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
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Package Condition", style = MaterialTheme.typography.labelLarge)
        ConditionDropdown(
            current = viewModel.condition,
            expanded = expanded,
            onExpand = { expanded = it },
            onSelect = { viewModel.condition = it; expanded = false },
            options = conditions
        )
        Spacer(modifier = Modifier.height(16.dp))
        PhotoCaptureArea(bitmap = viewModel.photoBitmap, onCapture = { cameraLauncher.launch() })
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.comment,
            onValueChange = { viewModel.comment = it },
            label = { Text("Comments") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (viewModel.submitError != null) {
            Text(
                viewModel.submitError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        SubmitButton(isSubmitting = viewModel.isSubmitting) {
            val fineLoc =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            if (fineLoc == PackageManager.PERMISSION_GRANTED) viewModel.submitScan()
            else locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ConditionDropdown(
    current: String,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    options: List<String>
) {
    Box(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onExpand(true) }
                .padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(current)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpand(false) }) {
            options.forEach { cond ->
                DropdownMenuItem(
                    text = { Text(cond) },
                    onClick = { onSelect(cond) })
            }
        }
    }
}

@Composable
fun PhotoCaptureArea(bitmap: android.graphics.Bitmap?, onCapture: () -> Unit) {
    Text("Package Photo", style = MaterialTheme.typography.labelLarge)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onCapture() }, contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
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
}

@Composable
fun SubmitButton(isSubmitting: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isSubmitting,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isSubmitting) CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.size(24.dp)
        )
        else {
            Icon(Icons.Default.LocationOn, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submit Scan with Location")
        }
    }
}

@Composable
fun CameraXLiveScanner(onBarcodeDetected: (Int) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val barcodeScanner = BarcodeScanning.getClient(
                    com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                        .build()
                )
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    processImageProxy(
                        barcodeScanner,
                        imageProxy,
                        onBarcodeDetected
                    )
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
        }, modifier = Modifier.fillMaxSize())
        ScannerOverlay()
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (Int) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image).addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                barcode.rawValue?.toIntOrNull()?.let { onBarcodeDetected(it) }
            }
        }.addOnFailureListener { Log.e("ScannerTab", "Barcode scanning failed", it) }
            .addOnCompleteListener { imageProxy.close() }
    } else imageProxy.close()
}

@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rectSize = size.width * 0.7f
        val left = (size.width - rectSize) / 2
        val top = (size.height - rectSize) / 2
        val cornerRadius = 40f
        val lineLength = 80f
        drawRect(Color(0x99000000))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(rectSize, rectSize),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            blendMode = BlendMode.Clear
        )
        val path = Path().apply {
            moveTo(left, top + lineLength); lineTo(left, top + cornerRadius); arcTo(
            Rect(
                left,
                top,
                left + cornerRadius * 2,
                top + cornerRadius * 2
            ), 180f, 90f, false
        ); lineTo(left + lineLength, top)
            moveTo(left + rectSize - lineLength, top); lineTo(
            left + rectSize - cornerRadius,
            top
        ); arcTo(
            Rect(
                left + rectSize - cornerRadius * 2,
                top,
                left + rectSize,
                top + cornerRadius * 2
            ), 270f, 90f, false
        ); lineTo(left + rectSize, top + lineLength)
            moveTo(left + rectSize, top + rectSize - lineLength); lineTo(
            left + rectSize,
            top + rectSize - cornerRadius
        ); arcTo(
            Rect(
                left + rectSize - cornerRadius * 2,
                top + rectSize - cornerRadius * 2,
                left + rectSize,
                top + rectSize
            ), 0f, 90f, false
        ); lineTo(left + rectSize - lineLength, top + rectSize)
            moveTo(left + lineLength, top + rectSize); lineTo(
            left + cornerRadius,
            top + rectSize
        ); arcTo(
            Rect(
                left,
                top + rectSize - cornerRadius * 2,
                left + cornerRadius * 2,
                top + rectSize
            ), 90f, 90f, false
        ); lineTo(left, top + rectSize - lineLength)
        }
        drawPath(
            path = path,
            color = Color(0xFF03DAC5),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )
    }
}
