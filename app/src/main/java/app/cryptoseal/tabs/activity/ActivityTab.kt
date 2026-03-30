package app.cryptoseal.tabs.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cryptoseal.data.model.Activity
import app.cryptoseal.util.DateTimeUtils

/**
 * The UI component for the "Activity" tab in the application.
 *
 * This screen provides a chronological feed of events relevant to the current user, 
 * such as package creation, status updates, and scan records. It includes a 
 * "Pull-to-Refresh" mechanism to manually update the feed from the server.
 *
 * @param viewModel The [ActivityViewModel] that provides the stream of activity data 
 * and handles the refresh logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTab(viewModel: ActivityViewModel) {
    // Collecting the list of activities from the StateFlow in the ViewModel.
    val activities by viewModel.activities.collectAsState()

    // Collecting the loading state to show the refresh indicator.
    val isLoading by viewModel.isLoading.collectAsState()

    // PullToRefreshBox provides the standard Android pull-down-to-refresh interaction.
    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refreshActivities() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Displayed at the top of the list.
            item {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Empty State: Displayed if there are no activities to show after loading.
            if (activities.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No activities yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Activity Items: Iterating through the list of activities provided by the ViewModel.
            items(activities) { activity ->
                ActivityItem(activity)
            }
        }
    }
}

/**
 * A composable representing a single entry in the activity feed.
 * 
 * Each item consists of a themed icon based on the event type, a text summary, 
 * and a formatted timestamp.
 *
 * @param activity The [Activity] data object containing details of the event.
 */
@Composable
fun ActivityItem(activity: Activity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Icon Selection Logic: 
        // Assigns a specific icon and color based on the 'type' string from the backend.
        val (icon, color) = when (activity.type) {
            "order_created" -> Icons.Default.AddBox to MaterialTheme.colorScheme.primary
            "order_received" -> Icons.Default.Inbox to MaterialTheme.colorScheme.secondary
            "status_changed" -> Icons.Default.Edit to MaterialTheme.colorScheme.tertiary
            "scan_created" -> Icons.Default.QrCodeScanner to Color(0xFF03A9F4) // Sky Blue
            "scan_added" -> Icons.Default.LibraryAdd to Color(0xFF4CAF50)      // Green
            else -> Icons.Default.Notifications to MaterialTheme.colorScheme.outline
        }

        // Circular background for the leading icon.
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Activity type: ${activity.type}",
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Text Content: 
        // Summary of what happened and the time it occurred.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = DateTimeUtils.formatIsoDate(activity.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // 3. Divider: 
    // Adds a visual separation between items, indented to align with the text.
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, top = 12.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )
}
