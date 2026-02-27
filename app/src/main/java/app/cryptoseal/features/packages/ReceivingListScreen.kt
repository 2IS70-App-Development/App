package app.cryptoseal.feature.packages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReceivingListScreen() {
    // Temporary hardcoded list to verify the receiving UI
    val receivedPackages = listOf(
        PackageItem("101", "Inbound Machinery", "Received - Intact"),
        PackageItem("102", "Confidential Contract", "Received - Verified"),
        PackageItem("103", "Office Supplies", "Received - Processing")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(receivedPackages) { pkg ->
            // Reusing the exact UI component from SendingListScreen
            PackageListItem(pkg)
        }
    }
}