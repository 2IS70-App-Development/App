package app.cryptoseal.tabs.creator

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cryptoseal.data.api.ApiService
import app.cryptoseal.tabs.PackagesViewModel
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorTab(
    creatorViewModel: CreatorViewModel = viewModel(),
    packagesViewModel: PackagesViewModel? = null,
    onFinish: () -> Unit = {}
) {
    val context = LocalContext.current
    val users by creatorViewModel.users.collectAsState()
    val isLoadingUsers by creatorViewModel.isLoadingUsers.collectAsState()
    val createOrderResult by creatorViewModel.createOrderResult.collectAsState()
    val isCreatingOrder by creatorViewModel.isCreatingOrder.collectAsState()

    LaunchedEffect(Unit) { creatorViewModel.loadAllUsers() }

    var shipmentName by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<UserDisplay?>(null) }
    var userSearchQuery by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var receiverExpanded by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showQrSheet by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var qrBitmapToSave by remember { mutableStateOf<Bitmap?>(null) }

    val resetFormAndFinish = {
        shipmentName = ""; comment = ""; selectedUser = null; userSearchQuery = ""; selectedBitmap =
        null; showQrSheet = false
        creatorViewModel.clearCreateResult(); onFinish()
    }

    val saveQrLauncher =
        rememberLauncherForActivityResult(CreateDocumentWithName("image/*")) { uri ->
        uri?.let {
            qrBitmapToSave?.let { bitmap ->
                context.contentResolver.openOutputStream(uri)
                    ?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) photoUri?.let { selectedBitmap = loadBitmapFromUri(context, it) }
    }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { selectedBitmap = loadBitmapFromUri(context, it) }
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            photoUri = uri; cameraLauncher.launch(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = shipmentName,
            onValueChange = { shipmentName = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = ApiService.currentUser?.email ?: "Loading...",
            onValueChange = { },
            label = { Text("Sender") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        )
        Spacer(modifier = Modifier.height(16.dp))

        ReceiverDropdown(
            query = userSearchQuery,
            onQueryChange = {
                userSearchQuery = it; if (selectedUser?.email != it) selectedUser = null
            },
            selectedUser = selectedUser,
            expanded = receiverExpanded,
            onExpandedChange = { receiverExpanded = it },
            users = users.filter { it.email.contains(userSearchQuery, ignoreCase = true) },
            isLoading = isLoadingUsers,
            onSelect = { selectedUser = it; userSearchQuery = it.email; receiverExpanded = false }
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )
        Spacer(modifier = Modifier.height(16.dp))
        PhotoAttachmentArea(
            selectedBitmap = selectedBitmap,
            onClick = { showImageSourceDialog = true })
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                selectedUser?.let { user ->
                    val photoBase64 = selectedBitmap?.let {
                        val baos = ByteArrayOutputStream()
                        it.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                        android.util.Base64.encodeToString(
                            baos.toByteArray(),
                            android.util.Base64.NO_WRAP
                        )
                    }
                    creatorViewModel.createOrder(user.id, shipmentName, "", comment, photoBase64)
                    showQrSheet = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedUser != null && shipmentName.isNotBlank() && !isCreatingOrder
        ) {
            if (isCreatingOrder) CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            else Text("Generate QR Code", fontWeight = FontWeight.Bold)
        }
    }

    if (showImageSourceDialog) {
        ImageSourceDialog(onDismiss = { showImageSourceDialog = false }, onCamera = {
            val permission = Manifest.permission.CAMERA
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                val uri =
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                photoUri = uri; cameraLauncher.launch(uri)
            } else cameraPermissionLauncher.launch(permission)
            showImageSourceDialog = false
        }, onGallery = { galleryLauncher.launch("image/*"); showImageSourceDialog = false })
    }

    if (showQrSheet && createOrderResult != null) {
        CreateOrderResultDialog(
            result = createOrderResult!!,
            packagesViewModel = packagesViewModel,
            onSave = { bitmap, name -> qrBitmapToSave = bitmap; saveQrLauncher.launch(name) },
            onDone = resetFormAndFinish,
            onCloseError = { showQrSheet = false; creatorViewModel.clearCreateResult() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiverDropdown(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedUser: UserDisplay?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    users: List<UserDisplay>,
    isLoading: Boolean,
    onSelect: (UserDisplay) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = query, onValueChange = onQueryChange, label = { Text("Receiver") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            if (isLoading) DropdownMenuItem(text = {
                CircularProgressIndicator(
                    modifier = Modifier.size(
                        20.dp
                    )
                )
            }, onClick = {})
            else if (users.isEmpty()) DropdownMenuItem(
                text = { Text("No users found") },
                onClick = {})
            else users.forEach { user ->
                DropdownMenuItem(
                    text = { Text(user.email) },
                    onClick = { onSelect(user) })
            }
        }
    }
}
