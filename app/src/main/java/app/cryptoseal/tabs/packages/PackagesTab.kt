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
 * UI representation of a package in the list, used specifically for the UI layer.
 *
 * @property id Unique identifier for the package (Order ID).
 * @property name Human-readable name or title of the package.
 * @property status Current delivery status (e.g., "SENT", "IN_TRANSIT", "DELIVERED").
 * @property isSentByMe True if the current user is the one who created/sent the package.
 */
data class PackageItem(val id: String, val name: String, val status: String, val isSentByMe: Boolean)

/**
 * The main UI screen for the "Packages" tab.
 * 
 * This screen displays a list of packages associated with the user, categorized into 
 * "Sent" (outbound) and "Received" (inbound) packages using a segmented toggle.
 * 
 * Confusing Areas Addressed:
 * 1. Categorization logic: Packages are filtered locally based on the user's role.
 * 2. Detail selection: Uses a nullable state 'selectedPackage' to trigger the [PackageSheet].
 * 3. Reactive updates: Collected from a single source of truth in the ViewModel.
 *
 * @param viewModel The shared [PackagesViewModel] providing state and data refresh logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesTab(
    viewModel: PackagesViewModel
) {
    // Observing the current tab selection (Sent vs Received).
    val selectedIndex by viewModel.selectedTab.collectAsState()

    // Observing the master list of all packages.
    val allPackages by viewModel.allPackages.collectAsState()

    // Observing the refresh status for showing the loading indicator.
    val isLoading by viewModel.isLoading.collectAsState()

    // Side Effect: Trigger a background refresh of the package list when the tab is first opened.
    LaunchedEffect(Unit) {
        viewModel.refreshPackages()
    }

    // Static labels for the segmented control.
    val options = listOf("Sent", "Received")

    // Local state to track which package the user has clicked on for more details.
    var selectedPackage by remember { mutableStateOf<PackageItem?>(null) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Segmented toggle to switch between Sent (Index 0) and Received (Index 1) categories.
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

            // Main Content Area: Loading, Empty, or List.
            if (isLoading && allPackages.isEmpty()) {
                // Initial Loading State: Show a centered spinner.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Filtering Logic: 
                // index 0 -> packages where I am the sender.
                // index 1 -> packages where I am the receiver.
                val currentList = allPackages.filter { pkg ->
                    if (selectedIndex == 0) pkg.isSentByMe else !pkg.isSentByMe
                }

                if (currentList.isEmpty()) {
                    // Empty State: Display a message if no packages match the selected category.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No packages found", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    // Result List: Displayed in a vertical scrollable column.
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

    // Dynamic Detail Resolution:
    // If a package is selected, find its latest state from the 'allPackages' flow.
    // This ensures that if the status changes while the sheet is open, the sheet reflects it.
    val packageToShow = allPackages.find { it.id == selectedPackage?.id } ?: selectedPackage

    // Overlay: Show the detailed [PackageSheet] when a package is selected.
    packageToShow?.let { pkg ->
        PackageSheet(
            pkg = pkg,
            viewModel = viewModel,
            onDismiss = { selectedPackage = null }
        )
    }
}

/**
 * A visually themed card representing a single package in the list.
 * 
 * It uses icons to denote directionality:
 * - North-east arrow (CallMade) for Sent.
 * - South-west arrow (CallReceived) for Received.
 *
 * @param pkg The [PackageItem] data to display.
 * @param onClick Event triggered when the user taps on the package card.
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
            // Contextual icon based on the package direction.
            val icon = if (pkg.isSentByMe) Icons.Default.CallMade else Icons.Default.CallReceived
            val iconTint = if (pkg.isSentByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

            Icon(
                imageVector = icon,
                contentDescription = if (pkg.isSentByMe) "Sent Package" else "Received Package",
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text column: Name and Status.
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

            // Trailing arrow to indicate that the item is clickable for more details.
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
