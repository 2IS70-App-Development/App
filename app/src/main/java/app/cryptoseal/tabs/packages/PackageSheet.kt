package app.cryptoseal.tabs.packages

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.cryptoseal.data.model.Scan
import app.cryptoseal.tabs.PackagesViewModel
import kotlinx.coroutines.launch

/**
 * A comprehensive detail view for a specific package, displayed as a full-screen dialog.
 */
@Composable
fun PackageSheet(
    pkg: PackageItem,
    viewModel: PackagesViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scans by viewModel.scans.collectAsState()
    val isScansLoading by viewModel.isScansLoading.collectAsState()
    val users by viewModel.users.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(pkg.id) { viewModel.fetchScans(pkg.id.toInt()) }
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    var selectedScanForDetails by remember { mutableStateOf<Scan?>(null) }
    var qrBitmapToShow by remember { mutableStateOf<Bitmap?>(null) }
    var showConfirmationDialog by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
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
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)) {
                PackageSheetHeader(pkg = pkg, onQrClick = {
                    scope.launch { qrBitmapToShow = viewModel.generateQrCode(pkg.id) }
                })
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Chain of Custody",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()) {
                    if (isScansLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (scans.isEmpty()) {
                        Text(
                            "No scans yet for this package",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            itemsIndexed(scans) { index, scan ->
                                val courierEmail = users.find { it.id == scan.courierId }?.email
                                    ?: "ID: ${scan.courierId}"
                                TimelineNode(
                                    scan = scan,
                                    courierEmail = courierEmail,
                                    isLast = index == scans.size - 1,
                                    onShowDetails = { selectedScanForDetails = scan })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                PackageActions(pkg = pkg, onAction = { showConfirmationDialog = it })
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Close") }
            }
        }
    }

    if (showConfirmationDialog != null) {
        OrderActionConfirmation(
            pkgName = pkg.name,
            targetStatus = showConfirmationDialog!!,
            onConfirm = {
                viewModel.updateOrderStatus(pkg.id, showConfirmationDialog!!)
                showConfirmationDialog = null
            },
            onDismiss = { showConfirmationDialog = null })
    }
    selectedScanForDetails?.let {
        ScanDetailsDialog(
            scan = it,
            onDismiss = { selectedScanForDetails = null })
    }
    qrBitmapToShow?.let {
        QRDisplayDialog(
            bitmap = it,
            packageName = pkg.name,
            packageId = pkg.id,
            onDismiss = { qrBitmapToShow = null })
    }
}

@Composable
fun PackageSheetHeader(pkg: PackageItem, onQrClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
        IconButton(onClick = onQrClick) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = "Show QR Code",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun PackageActions(pkg: PackageItem, onAction: (String) -> Unit) {
    val canCancel = pkg.isSentByMe && pkg.status.lowercase() !in listOf("cancelled", "delivered")
    val canMarkDelivered =
        !pkg.isSentByMe && pkg.status.lowercase() !in listOf("delivered", "cancelled")

    if (canCancel) {
        Button(
            onClick = { onAction("cancelled") },
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
            onClick = { onAction("delivered") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Mark as Delivered")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun OrderActionConfirmation(
    pkgName: String,
    targetStatus: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Action") },
        text = { Text("Are you sure you want to mark '$pkgName' as $targetStatus? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Confirm",
                    color = if (targetStatus == "cancelled") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } }
    )
}
