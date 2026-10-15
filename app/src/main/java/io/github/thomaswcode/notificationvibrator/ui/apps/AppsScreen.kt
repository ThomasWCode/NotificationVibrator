package io.github.thomaswcode.notificationvibrator.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.thomaswcode.notificationvibrator.ui.components.AppIcon
import io.github.thomaswcode.notificationvibrator.ui.components.PatternWaveform
import io.github.thomaswcode.notificationvibrator.ui.components.ScreenScaffold
import io.github.thomaswcode.notificationvibrator.ui.components.SectionHeader

/** What the picker sheet is editing: an app, or the default pattern when [packageName] is null. */
private data class EditTarget(
    val packageName: String?,
    val title: String,
    val patternId: String,
    val intensity: Float,
    val isAssigned: Boolean,
)

@Composable
fun AppsScreen(viewModel: AppsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val patterns by viewModel.patterns.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    ScreenScaffold(title = "Apps") { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item(key = "default") {
                DefaultPatternCard(state) {
                    editing = EditTarget(null, "Default pattern", state.defaultPattern.id, state.defaultIntensity, false)
                }
            }
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.query.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                )
            }
            appSection("assigned", "Assigned", state.assigned) { editing = it.toTarget() }
            appSection("recent", "Recently notified", state.recent) { editing = it.toTarget() }
            appSection("all", "All apps", state.all) { editing = it.toTarget() }
            if (state.loadingApps) {
                item(key = "loading") {
                    Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    editing?.let { target ->
        PatternPickerSheet(
            title = target.title,
            patterns = patterns,
            initialPatternId = target.patternId,
            initialIntensity = target.intensity,
            showReset = target.isAssigned,
            onPreview = viewModel::preview,
            onSave = { patternId, intensity ->
                if (target.packageName == null) {
                    viewModel.setDefault(patternId, intensity)
                } else {
                    viewModel.assign(target.packageName, patternId, intensity)
                }
                editing = null
            },
            onReset = {
                target.packageName?.let(viewModel::reset)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

private fun AppRow.toTarget() = EditTarget(packageName, label, pattern.id, intensity, isAssigned)

private fun LazyListScope.appSection(key: String, title: String, rows: List<AppRow>, onClick: (AppRow) -> Unit) {
    if (rows.isEmpty()) return
    item(key = "header_$key") { SectionHeader(title, Modifier.padding(horizontal = 16.dp)) }
    items(rows, key = { "${key}_${it.packageName}" }) { row -> AppRowItem(row) { onClick(row) } }
}

@Composable
private fun DefaultPatternCard(state: AppsUiState, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Default pattern", style = MaterialTheme.typography.titleMedium)
            Text(
                "Used by every app you haven't assigned. Tap to change.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.defaultPattern.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (!state.defaultPattern.isSilent) {
                    PatternWaveform(state.defaultPattern, Modifier.width(110.dp).height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AppRowItem(row: AppRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(row.packageName)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (row.isAssigned) row.pattern.name else "Default · ${row.pattern.name}",
                style = MaterialTheme.typography.bodySmall,
                color = if (row.isAssigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!row.pattern.isSilent) PatternWaveform(row.pattern, Modifier.width(72.dp).height(20.dp))
    }
}
