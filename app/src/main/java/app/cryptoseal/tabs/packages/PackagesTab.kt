package app.cryptoseal.tabs.packages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp
import app.cryptoseal.tabs.PackagesViewModel

/**
 * UI representation of a package in the list.
 *
 * @property id Unique identifier for the package.
 * @property name Human-readable name of the package.
 * @property status Current delivery status.
 * @property isSentByMe True if the current user is the sender, false if receiver.
 */
data class PackageItem(val id: String, val name: String, val status: String, val isSentByMe: Boolean)

/**
 * The main screen for the "Packages" tab.
 * Displays a list of packages categorized into "Sent" and "Received" using a segmented control.
 *
 * @param viewModel The [PackagesViewModel] that provides data and handles logic for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesTab(
    viewModel: PackagesViewModel
) {
    val selectedIndex by viewModel.selectedTab.collectAsState()
    val allPackages by viewModel.allPackages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Trigger a refresh when the tab is first composed.
    LaunchedEffect(Unit) {
        viewModel.refreshPackages()
    }

    val options = listOf("Sent", "Received")
    // State to track which package is currently selected for detail view (bottom sheet/dialog).
    var selectedPackage by remember { mutableStateOf<PackageItem?>(null) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Segmented control to switch between Sent and Received packages.
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        onClick = { viewModel.setTab(index) },
                        selected = index == selectedIndex
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && allPackages.isEmpty()) {
                // Show loading indicator if data is being fetched and list is currently empty.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Filter packages based on whether "Sent" or "Received" is selected.
                val currentList = allPackages.filter { pkg ->
                    if (selectedIndex == 0) pkg.isSentByMe else !pkg.isSentByMe
                }

                if (currentList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No packages found", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(currentList) { pkg ->
                            PackageListItem(
                                pkg = pkg,
                                onClick = { selectedPackage = pkg }
                            )
                        }
                    }
                }
            }
        }
    }

    // Ensure the sheet uses the latest data from the state flow when the status changes.
    // If a status update happens, the list in VM changes, and we find the updated version here.
    val packageToShow = allPackages.find { it.id == selectedPackage?.id } ?: selectedPackage

    // Show the detail sheet if a package is selected.
    packageToShow?.let { pkg ->
        PackageSheet(
            pkg = pkg,
            viewModel = viewModel,
            onDismiss = { selectedPackage = null }
        )
    }
}

/**
 * A single item in the package list.
 * Displays an icon indicating direction (sent/received), package name, and status.
 *
 * @param pkg The package data to display.
 * @param onClick Callback triggered when the item is tapped.
 */
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
            // Outbound icon for sent, Inbound icon for received.
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
