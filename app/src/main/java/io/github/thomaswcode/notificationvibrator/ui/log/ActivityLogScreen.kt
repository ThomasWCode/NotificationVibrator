package io.github.thomaswcode.notificationvibrator.ui.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.thomaswcode.notificationvibrator.appContainer
import io.github.thomaswcode.notificationvibrator.data.EventLogEntity
import io.github.thomaswcode.notificationvibrator.ui.components.ScreenScaffold
import io.github.thomaswcode.notificationvibrator.ui.components.rememberAppLabel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** The last 200 notification decisions, for checking why something did or didn't buzz. */
@Composable
fun ActivityLogScreen() {
    val container = LocalContext.current.appContainer
    val events by container.activityRepository.recentEvents.collectAsStateWithLifecycle(initialValue = emptyList())

    ScreenScaffold(
        title = "Activity",
        actions = {
            if (events.isNotEmpty()) {
                TextButton(onClick = { container.appScope.launch { container.activityRepository.clearLog() } }) { Text("Clear") }
            }
        },
    ) { padding ->
        if (events.isEmpty()) {
            Text(
                "Nothing yet. Each notification's result (played, or why it was skipped) will appear here.",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
        } else {
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(events, key = { it.id }) { EventRow(it) }
            }
        }
    }
}

@Composable
private fun EventRow(event: EventLogEntity) {
    val label = rememberAppLabel(event.packageName)
    val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(event.time))
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (event.played) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = if (event.played) "Played" else "Skipped",
            tint = if (event.played) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (event.played) "Played ${event.patternName}" else "Skipped: ${event.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(time, style = MaterialTheme.typography.bodySmall)
    }
}
