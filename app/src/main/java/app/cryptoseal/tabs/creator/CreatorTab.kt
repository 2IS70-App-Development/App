package app.cryptoseal.tabs.creator

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.cryptoseal.tabs.PackagesViewModel
import androidx.compose.foundation.Image
import app.cryptoseal.R

@Composable
fun CreatorTab(viewModel: PackagesViewModel) {
    var shipmentName by remember { mutableStateOf("") }
    var receiver by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

// State for the contacts dropdown
    var receiverExpanded by remember { mutableStateOf(false) }
    val contacts = listOf("Elena Rostova", "Marcus Vance", "Sarah Jenkins")

    var generatedOrderId by remember { mutableStateOf<String?>(null) }

// We are keeping the "Sheet" naming convention as requested,
// but implementing it via Dialog for usability
    var showQrSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Shipment Name ---
        OutlinedTextField(
            value = shipmentName,
            onValueChange = { shipmentName = it },
            label = { Text("Shipment Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Sender (Read-only / Grayed out) ---
        OutlinedTextField(
            value = "EMP-8492 (You)",
            onValueChange = { },
            label = { Text("Sender") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false, // Grays out the field
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Receiver (Dropdown from Contacts) ---
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = receiver,
                onValueChange = { },
                label = { Text("Receiver") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Receiver",
                        modifier = Modifier.clickable { receiverExpanded = true }
                    )
                }
            )
            DropdownMenu(
                expanded = receiverExpanded,
                onDismissRequest = { receiverExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                contacts.forEach { contact ->
                    DropdownMenuItem(
                        text = { Text(contact) },
                        onClick = {
                            receiver = contact
                            receiverExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. Description (Bigger Box) ---
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. Image Submission Placeholder ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .clickable { /* TODO: Launch camera/gallery */ },
            contentAlignment = Alignment.Center
        ) {
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

        Spacer(modifier = Modifier.height(32.dp))

        // --- 6. Generate Button ---
        Button(
            onClick = {
                generatedOrderId = viewModel.createAndAddPackage(
                    name = shipmentName,
                    routingInfo = receiver,
                    manifestData = description
                )
                showQrSheet = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Generate QR Code", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // --- 7. The Modal "Sheet" (Dialog) ---
    if (showQrSheet && generatedOrderId != null) {
        Dialog(
            onDismissRequest = {
                showQrSheet = false
                shipmentName = ""
                receiver = ""
                description = ""
            },
            // This is the magic line that lets the dialog expand
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Takes up 90% of the screen width
                    .padding(vertical = 32.dp), // Gives it room to breathe vertically
                shape = RoundedCornerShape(24.dp), // Slightly rounder corners for a larger card
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp), // Increased inner padding
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Shipment Ready",
                        style = MaterialTheme.typography.headlineSmall, // Made the title larger
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Print this code and ship your package.",
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
                            painter = painterResource(id = R.drawable.demo_qr),
                            contentDescription = "Generated QR Code",
                            modifier = Modifier.size(240.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "ID: $generatedOrderId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { /* TODO: Print logic */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Print", style = MaterialTheme.typography.titleMedium)
                        }
                        Button(
                            onClick = {
                                showQrSheet = false
                                shipmentName = ""
                                receiver = ""
                                description = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}