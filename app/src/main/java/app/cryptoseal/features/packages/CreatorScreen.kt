package app.cryptoseal.feature.packages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreatorScreen(viewModel: PackagesViewModel) {
    var shipmentName by remember { mutableStateOf("") }
    var routingInfo by remember { mutableStateOf("") }
    var manifestData by remember { mutableStateOf("") }
    var generatedOrderId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = shipmentName,
            onValueChange = { shipmentName = it },
            label = { Text("Shipment Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = routingInfo,
            onValueChange = { routingInfo = it },
            label = { Text("Routing Information (Public)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = manifestData,
            onValueChange = { manifestData = it },
            label = { Text("Manifest Data (Private)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Call the ViewModel to add the item and get the new ID
                generatedOrderId = viewModel.createAndAddPackage(
                    name = shipmentName,
                    routingInfo = routingInfo,
                    manifestData = manifestData
                )
                // Clear the form
                shipmentName = ""
                routingInfo = ""
                manifestData = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate & Sign")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Display the generated Order ID (This is where the actual QR graphic would go later)
        generatedOrderId?.let { orderId ->
            Text(text = "QR Code Generated!")
            Text(text = "Order ID: $orderId")
        }
    }
}