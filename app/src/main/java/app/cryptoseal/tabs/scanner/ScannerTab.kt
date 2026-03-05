package app.cryptoseal.tabs.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ScannerTab() {
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
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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
            // Wraps the camera inside a styled UI card
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up available space while respecting padding
            ) {
                CameraPreviewWithOverlay()
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required to scan QR codes.")
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun CameraPreviewWithOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraXLivePreview()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val rectSize = canvasWidth * 0.7f
            val left = (canvasWidth - rectSize) / 2
            val top = (canvasHeight - rectSize) / 2

            val cornerRadius = 40f
            val lineLength = 80f
            val strokeWidth = 12f
            val bracketColor = Color(0xFF03DAC5) // Secondary teal color

            // 1. Draw the dark semi-transparent overlay
            drawRect(Color(0x99000000))

            // 2. Punch a transparent rounded hole in the middle
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(rectSize, rectSize),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                blendMode = BlendMode.Clear
            )

            // 3. Draw the 4 corner brackets using a Path
            val path = Path().apply {
                // Top-Left Corner
                moveTo(left, top + lineLength)
                lineTo(left, top + cornerRadius)
                arcTo(Rect(left, top, left + cornerRadius * 2, top + cornerRadius * 2), 180f, 90f, false)
                lineTo(left + lineLength, top)

                // Top-Right Corner
                moveTo(left + rectSize - lineLength, top)
                lineTo(left + rectSize - cornerRadius, top)
                arcTo(Rect(left + rectSize - cornerRadius * 2, top, left + rectSize, top + cornerRadius * 2), 270f, 90f, false)
                lineTo(left + rectSize, top + lineLength)

                // Bottom-Right Corner
                moveTo(left + rectSize, top + rectSize - lineLength)
                lineTo(left + rectSize, top + rectSize - cornerRadius)
                arcTo(Rect(left + rectSize - cornerRadius * 2, top + rectSize - cornerRadius * 2, left + rectSize, top + rectSize), 0f, 90f, false)
                lineTo(left + rectSize - lineLength, top + rectSize)

                // Bottom-Left Corner
                moveTo(left + lineLength, top + rectSize)
                lineTo(left + cornerRadius, top + rectSize)
                arcTo(Rect(left, top + rectSize - cornerRadius * 2, left + cornerRadius * 2, top + rectSize), 90f, 90f, false)
                lineTo(left, top + rectSize - lineLength)
            }

            drawPath(
                path = path,
                color = bracketColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun CameraXLivePreview() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Set up the preview use case
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Select the back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    // In a production app, log this error
                    exc.printStackTrace()
                }
            }, executor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}