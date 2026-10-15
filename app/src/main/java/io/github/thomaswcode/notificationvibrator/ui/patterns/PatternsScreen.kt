package io.github.thomaswcode.notificationvibrator.ui.patterns

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.thomaswcode.notificationvibrator.appContainer
import io.github.thomaswcode.notificationvibrator.ui.components.PatternRow
import io.github.thomaswcode.notificationvibrator.ui.components.ScreenScaffold
import io.github.thomaswcode.notificationvibrator.ui.components.SectionHeader
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory
import io.github.thomaswcode.notificationvibrator.vibration.PatternLibrary
import io.github.thomaswcode.notificationvibrator.vibration.VibrationPattern
import kotlinx.coroutines.launch

private enum class PatternsPage { LIST, TAP, MORSE }

/** Custom patterns may be longer than built-ins, but are still capped to keep buzzes bearable. */
const val CUSTOM_MAX_DURATION_MS = 5000L

@Composable
fun PatternsTab() {
    var page by rememberSaveable { mutableStateOf(PatternsPage.LIST) }
    BackHandler(enabled = page != PatternsPage.LIST) { page = PatternsPage.LIST }
    when (page) {
        PatternsPage.LIST -> PatternsScreen(
            onRecord = { page = PatternsPage.TAP },
            onMorse = { page = PatternsPage.MORSE },
        )
        PatternsPage.TAP -> TapRecordScreen(onDone = { page = PatternsPage.LIST })
        PatternsPage.MORSE -> MorseScreen(onDone = { page = PatternsPage.LIST })
    }
}

@Composable
private fun PatternsScreen(onRecord: () -> Unit, onMorse: () -> Unit) {
    val container = LocalContext.current.appContainer
    val custom by container.patternRepository.customPatterns.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Pair<VibrationPattern, Int>?>(null) }
    val builtIns = remember {
        PatternLibrary.all.filterNot { it.isSilent }.groupBy { it.category }.toSortedMap(compareBy<PatternCategory> { it.ordinal })
    }

    ScreenScaffold(title = "Patterns") { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            item(key = "actions") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = onRecord, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Record rhythm")
                    }
                    FilledTonalButton(onClick = onMorse, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Morse code")
                    }
                }
                Text(
                    "Tap any pattern to feel it. Assign patterns on the Apps tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item(key = "header_custom") { SectionHeader(PatternCategory.CUSTOM.label, Modifier.padding(horizontal = 16.dp)) }
            if (custom.isEmpty()) {
                item(key = "custom_empty") {
                    Text(
                        "None yet. Record a rhythm or type some Morse to make your own.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(custom, key = { it.id }) { pattern ->
                PatternRow(
                    pattern = pattern,
                    onClick = { container.preview(pattern, 1f) },
                    trailing = {
                        IconButton(onClick = {
                            scope.launch { pendingDelete = pattern to container.patternRepository.usageCount(pattern) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${pattern.name}")
                        }
                    },
                )
            }
            builtIns.forEach { (category, list) ->
                item(key = "header_${category.name}") { SectionHeader(category.label, Modifier.padding(horizontal = 16.dp)) }
                items(list, key = { it.id }) { pattern ->
                    PatternRow(pattern = pattern, onClick = { container.preview(pattern, 1f) })
                }
            }
        }
    }

    pendingDelete?.let { (pattern, usage) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${pattern.name}\"?") },
            text = {
                Text(
                    if (usage > 0) {
                        "It is in use ($usage). Apps using it will switch to the default pattern; if it is the default, the default resets to Medium."
                    } else {
                        "This can't be undone."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    container.appScope.launch { container.patternRepository.deleteCustom(pattern) }
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
