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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Simple data model for the UI placeholder
data class ActivityEvent(
val id: String,
val description: String,
val timestamp: String,
val location: String,
val type: ActivityType
)

enum class ActivityType { SCANNED, DELIVERED, CREATED }

@Composable
fun ActivityTab() {
// Dummy data to simulate the "Real-time feed" described in app.md
val events = listOf(
ActivityEvent("1", "Package #102 delivered to recipient", "Today, 10:42 AM", "Berlin, DE", ActivityType.DELIVERED),
ActivityEvent("2", "Package #102 scanned by Courier", "Today, 08:15 AM", "Hamburg, DE", ActivityType.SCANNED),
ActivityEvent("3", "Shipment #103 created by Logistics", "Yesterday, 04:30 PM", "Rotterdam, NL", ActivityType.CREATED),
ActivityEvent("4", "Inbound Machinery scanned at Hub", "Yesterday, 02:15 PM", "Rotterdam, NL", ActivityType.SCANNED)
)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Title
        item {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // The list of events
        items(events) { event ->
            ActivityItem(event)
        }
    }
}

@Composable
fun ActivityItem(event: ActivityEvent) {
Row(
modifier = Modifier
.fillMaxWidth()
.padding(vertical = 4.dp),
verticalAlignment = Alignment.CenterVertically
) {
// 1. Leading Icon (Event Type)
val (icon, color) = when (event.type) {
ActivityType.DELIVERED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
ActivityType.SCANNED -> Icons.Default.LocationOn to MaterialTheme.colorScheme.secondary
ActivityType.CREATED -> Icons.Default.ShoppingCart to MaterialTheme.colorScheme.tertiary
}

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Text Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${event.timestamp} • ${event.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // 3. Subtle Divider (app.md requirement)
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, top = 12.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )
}