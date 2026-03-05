package app.cryptoseal.tabs.packages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cryptoseal.tabs.PackagesViewModel
import androidx.compose.foundation.lazy.items

// 1. Updated data class with the boolean flag
data class PackageItem(val id: String, val name: String, val status: String, val isSentByMe: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesTab(viewModel: PackagesViewModel) {
    val selectedIndex by viewModel.selectedTab.collectAsState()

    // 2. We now observe a single unified list from the ViewModel
    val allPackages by viewModel.allPackages.collectAsState()

    val options = listOf("Sent", "Received")

    // 3. State to control the visibility of the bottom sheet
    var selectedPackage by remember { mutableStateOf<PackageItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- The Segmented Filter Button ---
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { viewModel.setTab(index) },
                    selected = index == selectedIndex
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- The List ---
        // 4. Filter the single list on the fly based on the segmented button
        val currentList = allPackages.filter { pkg ->
            if (selectedIndex == 0) pkg.isSentByMe else !pkg.isSentByMe
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(currentList) { pkg ->
                PackageListItem(
                    pkg = pkg,
                    // 5. Clicking a row assigns that package to the state variable
                    onClick = { selectedPackage = pkg }
                )
            }
        }
    }

    // 6. If a package is selected, display the sheet
    selectedPackage?.let { pkg ->
        PackageSheet(
            pkg = pkg,
            onDismiss = { selectedPackage = null } // Clears the state to close the sheet
        )
    }
}

@Composable
fun PackageListItem(pkg: PackageItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dynamic Icon based on direction
            val icon = if (pkg.isSentByMe) Icons.Default.CallMade else Icons.Default.CallReceived
            val iconTint = if (pkg.isSentByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

            Icon(
                imageVector = icon,
                contentDescription = "Package Direction",
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pkg.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pkg.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}