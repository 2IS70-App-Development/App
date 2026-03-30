package app.cryptoseal.tabs.creator

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cryptoseal.data.api.ApiService
import app.cryptoseal.tabs.PackagesViewModel
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * The "Creator" tab UI - where users initiate new shipments.
 *
 * This screen provides a form to input shipment details (name, receiver, comment) and 
 * attach a photo. Upon submission, it displays a unique QR code generated for the order.
 *
 * Confusing Areas Addressed:
 * 1. Image capture vs. gallery selection.
 * 2. Scoping the receiver search list.
 * 3. FileProvider setup for camera URI.
 * 4. Handling the result of the document creation launcher.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorTab(
    creatorViewModel: CreatorViewModel = viewModel(),
    packagesViewModel: PackagesViewModel? = null,
    onFinish: () -> Unit = {}
) {
    val context = LocalContext.current

    // Observation of ViewModel states.
    val users by creatorViewModel.users.collectAsState()
    val isLoadingUsers by creatorViewModel.isLoadingUsers.collectAsState()
    val createOrderResult by creatorViewModel.createOrderResult.collectAsState()
    val isCreatingOrder by creatorViewModel.isCreatingOrder.collectAsState()

    // Trigger loading of available receivers when the tab is mounted.
    LaunchedEffect(Unit) {
        creatorViewModel.loadAllUsers()
    }

    // --- Local Form State ---
    var shipmentName by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<UserDisplay?>(null) }
    var userSearchQuery by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // --- Visibility/UI State ---
    var receiverExpanded by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showQrSheet by remember { mutableStateOf(false) }

    // --- Temporary Storage for System Intents ---
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var qrBitmapToSave by remember { mutableStateOf<Bitmap?>(null) }

    /**
     * Resets the entire form and navigates back to the main list.
     * Used after a successful save or when the user closes the success dialog.
     */
    val resetFormAndFinish = {
        shipmentName = ""
        comment = ""
        selectedUser = null
        userSearchQuery = ""
        selectedBitmap = null
        showQrSheet = false
        creatorViewModel.clearCreateResult()
        onFinish()
    }

    // --- System Activity Launchers ---

    // 1. Launcher to save the generated QR code to the device storage using ACTION_CREATE_DOCUMENT.
    val saveQrLauncher = rememberLauncherForActivityResult(
        contract = CreateDocumentWithName("image/*")
    ) { uri ->
        uri?.let {
            qrBitmapToSave?.let { bitmap ->
                try {
                    // Open an output stream to the user-selected URI and compress the bitmap into it.
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    android.widget.Toast.makeText(context, "QR saved", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Failed to save", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 2. Launcher for the system camera app.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri ->
                // Once the photo is saved at the URI, load it into a Bitmap for display in the UI.
                selectedBitmap = loadBitmapFromUri(context, uri)
            }
        }
    }

    // 3. Launcher for picking an image from the device's gallery.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedBitmap = loadBitmapFromUri(context, it)
        }
    }

    // 4. Launcher for requesting runtime Camera permission.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // If granted, proceed with launching the camera.
            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            photoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    /**
     * Handles the logic of starting the camera, including permission checks and URI generation.
     */
    fun launchCamera() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            // Create a temporary file in the cache directory to hold the high-res image.
            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            // Convert file to a content URI using FileProvider for security.
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            photoUri = uri
            cameraLauncher.launch(uri)
        } else {
            // Request permission if not already granted.
            cameraPermissionLauncher.launch(permission)
        }
    }

    // Filter available users based on the current search query.
    val filteredUsers = remember(userSearchQuery, users) {
        if (userSearchQuery.isBlank()) {
            users
        } else {
            users.filter { it.email.contains(userSearchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Field: Order Title
        OutlinedTextField(
            value = shipmentName,
            onValueChange = { shipmentName = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Field: Sender (Immutable, shows the currently logged-in user)
        OutlinedTextField(
            value = ApiService.currentUser?.email ?: "Loading...",
            onValueChange = { },
            label = { Text("Sender") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Field: Receiver (Dropdown with search-as-you-type)
        ExposedDropdownMenuBox(
            expanded = receiverExpanded,
            onExpandedChange = { receiverExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = userSearchQuery,
                onValueChange = {
                    userSearchQuery = it
                    // Reset the formal 'selected' state if the user starts typing something else.
                    if (selectedUser != null && !selectedUser!!.email.contains(
                            it,
                            ignoreCase = true
                        )
                    ) {
                        selectedUser = null
                    }
                },
                label = { Text("Receiver") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = false,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = receiverExpanded) },
                isError = selectedUser == null && shipmentName.isNotBlank()
            )

            // The dropdown menu content.
            if (isLoadingUsers) {
                DropdownMenuItem(
                    text = { CircularProgressIndicator(modifier = Modifier.size(20.dp)) },
                    onClick = { }
                )
            } else {
                ExposedDropdownMenu(
                    expanded = receiverExpanded,
                    onDismissRequest = { receiverExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    if (filteredUsers.isEmpty() && userSearchQuery.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text("No users found") },
                            onClick = { }
                        )
                    } else {
                        filteredUsers.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(user.email) },
                                onClick = {
                                    selectedUser = user
                                    userSearchQuery = user.email
                                    receiverExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Field: Large Comment Box
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

        // Photo Attachment Area
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
                .clickable { showImageSourceDialog = true },
            contentAlignment = Alignment.Center
        ) {
            if (selectedBitmap != null) {
                Image(
                    bitmap = selectedBitmap!!.asImageBitmap(),
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

        Spacer(modifier = Modifier.height(32.dp))

        // Main Submit Button
        Button(
            onClick = {
                selectedUser?.let { user ->
                    // Convert the bitmap into a Base64 string for network transmission.
                    val photoBase64 = selectedBitmap?.let { bitmap ->
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                        val bytes = baos.toByteArray()
                        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    }
                    creatorViewModel.createOrder(
                        receiverId = user.id,
                        name = shipmentName,
                        meta = "", 
                        comment = comment,
                        photoBase64 = photoBase64
                    )
                    showQrSheet = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedUser != null && shipmentName.isNotBlank() && !isCreatingOrder
        ) {
            if (isCreatingOrder) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Generate QR Code", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // --- Sub-Dialogs and Overlays ---

    // Source Picker: Camera or Gallery?
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Photo Source") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showImageSourceDialog = false; launchCamera()
                            }
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Camera")
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showImageSourceDialog = false; galleryLauncher.launch("image/*")
                            }
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Gallery",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Gallery")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    // Result Overlay: Show the new shipment's QR code or an error message.
    if (showQrSheet && createOrderResult != null) {
        Dialog(
            onDismissRequest = resetFormAndFinish,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 32.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val result = createOrderResult) {
                        is CreateOrderResult.Success -> {
                            LaunchedEffect(result) { packagesViewModel?.refreshPackages() }
                            Text(
                                text = "Shipment Ready",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Order ID: ${result.order.id}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Box(
                                modifier = Modifier
                                    .size(280.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = result.qrBitmap.asImageBitmap(),
                                    contentDescription = "Generated QR Code",
                                    modifier = Modifier.size(240.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "Name: ${result.order.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TextButton(onClick = {
                                    (createOrderResult as? CreateOrderResult.Success)?.let { successResult ->
                                        qrBitmapToSave = successResult.qrBitmap
                                        saveQrLauncher.launch("QR_${successResult.order.id}.png")
                                    }
                                }, modifier = Modifier.weight(1f)) {
                                    Text("Save", style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = resetFormAndFinish,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Done", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        is CreateOrderResult.Error -> {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = {
                                showQrSheet = false; creatorViewModel.clearCreateResult()
                            }) { Text("Close") }
                        }
                        null -> { }
                    }
                }
            }
        }
    }
}

/**
 * Loads a [Bitmap] from a given [Uri] while handling API version differences.
 * Decodes the image into software memory for compatibility with most image processing tools.
 */
private fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Use modern ImageDecoder for newer Android versions.
                android.graphics.ImageDecoder.decodeBitmap(
                    android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                ) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                // Fallback to legacy BitmapFactory for older devices.
                BitmapFactory.decodeStream(inputStream)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * A custom [ActivityResultContract] to trigger the system document creation picker
 * with a pre-filled default filename.
 */
private class CreateDocumentWithName(mimeType: String) : androidx.activity.result.contract.ActivityResultContract<String, android.net.Uri?>() {
    private val mimeType_ = mimeType
    override fun createIntent(context: android.content.Context, input: String) =
        android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = mimeType_
            putExtra(android.content.Intent.EXTRA_TITLE, input)
        }
    override fun parseResult(resultCode: Int, intent: android.content.Intent?) =
        if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
}
