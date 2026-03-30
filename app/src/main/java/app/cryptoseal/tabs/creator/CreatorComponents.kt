package app.cryptoseal.tabs.creator

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.cryptoseal.tabs.PackagesViewModel

@Composable
fun PhotoAttachmentArea(selectedBitmap: Bitmap?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selectedBitmap != null) {
            Image(
                bitmap = selectedBitmap.asImageBitmap(),
                contentDescription = "Selected Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Add Photo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to add package photo",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ImageSourceDialog(onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Photo Source") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SourceOption(icon = Icons.Default.CameraAlt, label = "Camera", onClick = onCamera)
                SourceOption(icon = Icons.Default.Photo, label = "Gallery", onClick = onGallery)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SourceOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(label)
    }
}

@Composable
fun CreateOrderResultDialog(
    result: CreateOrderResult,
    packagesViewModel: PackagesViewModel?,
    onSave: (Bitmap, String) -> Unit,
    onDone: () -> Unit,
    onCloseError: () -> Unit
) {
    Dialog(
        onDismissRequest = if (result is CreateOrderResult.Success) onDone else onCloseError,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 32.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (result) {
                    is CreateOrderResult.Success -> SuccessContent(
                        result,
                        packagesViewModel,
                        onSave,
                        onDone
                    )

                    is CreateOrderResult.Error -> ErrorContent(result, onCloseError)
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    result: CreateOrderResult.Success,
    packagesViewModel: PackagesViewModel?,
    onSave: (Bitmap, String) -> Unit,
    onDone: () -> Unit
) {
    LaunchedEffect(result) { packagesViewModel?.refreshPackages() }
    Text(
        text = "Shipment Ready",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = "Order ID: ${result.order.id}", style = MaterialTheme.typography.bodyLarge)
    Spacer(modifier = Modifier.height(32.dp))
    Box(
        modifier = Modifier
            .size(280.dp)
            .background(Color.White, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = result.qrBitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier.size(240.dp)
        )
    }
    Spacer(modifier = Modifier.height(32.dp))
    Text(text = "Name: ${result.order.name}", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(32.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = { onSave(result.qrBitmap, "QR_${result.order.id}.png") },
            modifier = Modifier.weight(1f)
        ) { Text("Save") }
        Button(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Done") }
    }
}

@Composable
private fun ErrorContent(result: CreateOrderResult.Error, onClose: () -> Unit) {
    Text(
        text = "Error",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = result.message, style = MaterialTheme.typography.bodyLarge)
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onClose) { Text("Close") }
}
