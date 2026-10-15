package io.github.thomaswcode.notificationvibrator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.thomaswcode.notificationvibrator.vibration.VibrationPattern
import io.github.thomaswcode.notificationvibrator.vibration.Waveforms
import java.util.Locale

/** Screen frame with a top bar. Insets for the status bar come from the top bar; the bottom nav handles the rest. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = actions,
            )
        },
        contentWindowInsets = WindowInsets(0),
        content = content,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

/** Draws a pattern as bars: width = duration, height = strength. */
@Composable
fun PatternWaveform(
    pattern: VibrationPattern,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val amplitudes = Waveforms.baseAmplitudes(pattern)
    val total = pattern.totalDurationMs.coerceAtLeast(1L).toFloat()
    Canvas(modifier) {
        var start = 0L
        pattern.timings.forEachIndexed { i, duration ->
            val amplitude = amplitudes.getOrElse(i) { 0 }
            if (amplitude > 0 && duration > 0) {
                val barHeight = size.height * (0.2f + 0.8f * amplitude / Waveforms.MAX_AMPLITUDE)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * start / total, (size.height - barHeight) / 2),
                    size = Size((size.width * duration / total).coerceAtLeast(2f), barHeight),
                    cornerRadius = CornerRadius(2f, 2f),
                )
            }
            start += duration
        }
    }
}

fun formatSeconds(ms: Long): String = String.format(Locale.getDefault(), "%.1f s", ms / 1000f)

/** One pattern in a list: optional radio button, name, waveform and length. */
@Composable
fun PatternRow(
    pattern: VibrationPattern,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected != null) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(pattern.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!pattern.isSilent) {
                Text(
                    formatSeconds(pattern.totalDurationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(Modifier.width(110.dp).height(24.dp)) {
            if (!pattern.isSilent) PatternWaveform(pattern, Modifier.width(110.dp).height(24.dp))
        }
        if (trailing != null) trailing()
    }
}
