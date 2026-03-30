package app.cryptoseal.tabs.packages

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.cryptoseal.data.model.Scan
import app.cryptoseal.tabs.PackagesViewModel
import app.cryptoseal.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A comprehensive detail view for a specific package, displayed as a full-screen dialog.
 * 
 * This component acts as the primary interface for tracking a package's journey, 
 * viewing its "Chain of Custody", and performing final status updates like 
 * delivery confirmation or cancellation.
 *
 * Confusing Areas Addressed:
 * 1. Role-based actions: Only senders can cancel; only receivers can mark as delivered.
 * 2. Asynchronous scan loading: Uses LaunchedEffect to trigger API calls on item selection.
 * 3. QR Code generation: Provides a way for users to show the QR code to others for scanning.
 * 4. Timeline UI: Implements a custom vertical timeline using LazyColumn and TimelineNode.
 *
 * @param pkg The selected package item to display.
 * @param viewModel The shared [PackagesViewModel] managing the package and scan states.
 * @param onDismiss Callback to close this detail view.
 */
@Composable
fun PackageSheet(
    pkg: PackageItem,
    viewModel: PackagesViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Collecting states from the shared ViewModel.
    val scans by viewModel.scans.collectAsState()
    val isScansLoading by viewModel.isScansLoading.collectAsState()
    val users by viewModel.users.collectAsState()

    // Coroutine scope for one-off operations like QR generation.
    val scope = rememberCoroutineScope()

    // Side Effect: Fetch scan history from the backend whenever this package is shown.
    LaunchedEffect(pkg.id) {
        viewModel.fetchScans(pkg.id.toInt())
    }

    // Side Effect: Observe a shared toast message flow from the ViewModel to show UI feedback.
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Local UI State for managing sub-overlays.
    var selectedScanForDetails by remember { mutableStateOf<Scan?>(null) }
    var qrBitmapToShow by remember { mutableStateOf<Bitmap?>(null) }
    var showConfirmationDialog by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        // usePlatformDefaultWidth = false allows the card to take up most of the screen width.
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // --- Header: Name, Status, and QR Trigger ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pkg.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = pkg.status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            // Generates a QR code containing the order ID so others can scan it.
                            qrBitmapToShow = viewModel.generateQrCode(pkg.id)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Show QR Code",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Chain of Custody",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- Timeline Content ---
                if (isScansLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (scans.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No scans yet for this package",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    // Scrollable list of handover events.
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        itemsIndexed(scans) { index, scan ->
                            // Resolve the courier's email from the pre-fetched users list.
                            val courierEmail = users.find { it.id == scan.courierId }?.email
                                ?: "ID: ${scan.courierId}"
                            TimelineNode(
                                scan = scan,
                                courierEmail = courierEmail,
                                isLast = index == scans.size - 1,
                                onShowDetails = { selectedScanForDetails = scan }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Contextual Actions ---
                // Senders can cancel unless already delivered/cancelled.
                val canCancel =
                    pkg.isSentByMe && pkg.status.lowercase() != "cancelled" && pkg.status.lowercase() != "delivered"
                // Receivers can confirm delivery unless already delivered/cancelled.
                val canMarkDelivered =
                    !pkg.isSentByMe && pkg.status.lowercase() != "delivered" && pkg.status.lowercase() != "cancelled"

                if (canCancel) {
                    Button(
                        onClick = { showConfirmationDialog = "cancelled" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel Order")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (canMarkDelivered) {
                    Button(
                        onClick = { showConfirmationDialog = "delivered" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), // Success Green
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mark as Delivered")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Simple close button at the bottom of the card.
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // --- Auxiliary Dialogs ---

    // Confirmation logic before performing a destructive or final API update.
    showConfirmationDialog?.let { targetStatus ->
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = null },
            title = { Text("Confirm Action") },
            text = { Text("Are you sure you want to mark '${pkg.name}' as $targetStatus? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateOrderStatus(pkg.id, targetStatus)
                        showConfirmationDialog = null
                    }
                ) {
                    Text(
                        "Confirm",
                        color = if (targetStatus == "cancelled") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = null }) { Text("Back") }
            }
        )
    }

    // Detail view for a specific scan node (shows photo, GPS coordinates, etc.).
    selectedScanForDetails?.let { scan ->
        ScanDetailsDialog(scan = scan, onDismiss = { selectedScanForDetails = null })
    }

    // QR Code display dialog for handovers.
    qrBitmapToShow?.let { bitmap ->
        QRDisplayDialog(
            bitmap = bitmap,
            packageName = pkg.name,
            packageId = pkg.id,
            onDismiss = { qrBitmapToShow = null }
        )
    }
}

/**
 * A dialog that displays a large version of a package's QR code.
 * Includes functionality to save the QR code image to the device's gallery.
 */
@Composable
fun QRDisplayDialog(bitmap: Bitmap, packageName: String, packageId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Package QR Code",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        scope.launch {
                            val success = saveQrToGallery(context, bitmap, "QR_$packageId")
                            if (success) Toast.makeText(
                                context,
                                "QR Code saved to gallery",
                                Toast.LENGTH_SHORT
                            ).show()
                            else Toast.makeText(
                                context,
                                "Failed to save QR Code",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save to Gallery",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$packageName (ID: $packageId)",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(250.dp)
                        .background(Color.White)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Have the receiver or courier scan this QR code to confirm the transfer of custody.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}

/**
 * Saves a Bitmap image to the device's public gallery under the "Pictures/CryptoSeal" directory.
 */
suspend fun saveQrToGallery(
    context: android.content.Context,
    bitmap: Bitmap,
    name: String
): Boolean = withContext(Dispatchers.IO) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.png")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CryptoSeal")
        }
    }

    val uri =
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            true
        } catch (e: Exception) {
            false
        }
    } else {
        false
    }
}

/**
 * A custom UI node for the vertical timeline representing a single scan event.
 * Consists of a vertical connector line, a primary dot, and textual event details.
 */
@Composable
fun TimelineNode(scan: Scan, courierEmail: String, isLast: Boolean, onShowDetails: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)) {
        // Left Column: The decorative dot and line.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                )
            }
        }

        // Right Column: Event information.
        Column(modifier = Modifier
            .weight(1f)
            .padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scan.condition,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${DateTimeUtils.formatIsoDate(scan.createdAt)} • $courierEmail",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onShowDetails) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Show Scan Details",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * A dialog that displays the full details of a specific scan, 
 * including the high-resolution photo taken at the time of the handover.
 */
@Composable
fun ScanDetailsDialog(scan: Scan, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Scan Details", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                // Decode the Base64 photo string into a Bitmap for display.
                if (!scan.photo.isNullOrEmpty()) {
                    val bitmap = remember(scan.photo) {
                        try {
                            val decodedString = Base64.decode(scan.photo, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Scan Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Text("Time", style = MaterialTheme.typography.labelLarge)
                Text(
                    DateTimeUtils.formatIsoDate(scan.createdAt),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Location", style = MaterialTheme.typography.labelLarge)
                Text(
                    "${scan.latitude}, ${scan.longitude}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (scan.comment.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Comment", style = MaterialTheme.typography.labelLarge)
                    Text(scan.comment, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Dismiss") }
            }
        }
    }
}
