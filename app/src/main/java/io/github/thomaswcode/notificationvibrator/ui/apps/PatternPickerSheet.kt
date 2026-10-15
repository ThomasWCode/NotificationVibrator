package io.github.thomaswcode.notificationvibrator.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.thomaswcode.notificationvibrator.ui.components.PatternRow
import io.github.thomaswcode.notificationvibrator.ui.components.SectionHeader
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory
import io.github.thomaswcode.notificationvibrator.vibration.VibrationPattern
import io.github.thomaswcode.notificationvibrator.vibration.Waveforms
import kotlin.math.roundToInt

/** Bottom sheet for choosing a pattern and strength. Tapping a pattern previews it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternPickerSheet(
    title: String,
    patterns: List<VibrationPattern>,
    initialPatternId: String,
    initialIntensity: Float,
    showReset: Boolean,
    onPreview: (VibrationPattern, Float) -> Unit,
    onSave: (patternId: String, intensity: Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedId by remember { mutableStateOf(initialPatternId) }
    var intensity by remember { mutableFloatStateOf(initialIntensity) }
    val grouped = remember(patterns) {
        patterns.groupBy { it.category }.toSortedMap(compareBy<PatternCategory> { it.ordinal })
    }
    // Open the list scrolled to the current choice: each category adds a header item before its patterns
    val initialIndex = remember(grouped) {
        var index = 0
        for ((_, list) in grouped) {
            index++
            val position = list.indexOfFirst { it.id == initialPatternId }
            if (position >= 0) return@remember (index + position - 1).coerceAtLeast(0)
            index += list.size
        }
        0
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val selected = patterns.firstOrNull { it.id == selectedId }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                "Selected: ${selected?.name ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Strength ${(intensity * 100).roundToInt()}%", Modifier.width(110.dp))
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = Waveforms.MIN_INTENSITY..Waveforms.MAX_INTENSITY,
                    onValueChangeFinished = { selected?.let { onPreview(it, intensity) } },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (showReset) TextButton(onClick = onReset) { Text("Reset to default") }
                OutlinedButton(onClick = { selected?.let { onPreview(it, intensity) } }) { Text("Test") }
                Button(onClick = { onSave(selectedId, intensity) }) { Text("Save") }
            }
            Spacer(Modifier.padding(4.dp))
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            grouped.forEach { (category, list) ->
                item(key = "header_${category.name}") { SectionHeader(category.label, Modifier.padding(horizontal = 16.dp)) }
                items(list, key = { it.id }) { pattern ->
                    PatternRow(
                        pattern = pattern,
                        selected = pattern.id == selectedId,
                        onClick = {
                            selectedId = pattern.id
                            onPreview(pattern, intensity)
                        },
                    )
                }
            }
        }
    }
}
