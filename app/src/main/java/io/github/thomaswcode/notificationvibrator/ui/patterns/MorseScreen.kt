package io.github.thomaswcode.notificationvibrator.ui.patterns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.thomaswcode.notificationvibrator.appContainer
import io.github.thomaswcode.notificationvibrator.data.CustomPatternSource
import io.github.thomaswcode.notificationvibrator.ui.components.PatternWaveform
import io.github.thomaswcode.notificationvibrator.ui.components.ScreenScaffold
import io.github.thomaswcode.notificationvibrator.ui.components.formatSeconds
import io.github.thomaswcode.notificationvibrator.vibration.MorseEncoder
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory
import io.github.thomaswcode.notificationvibrator.vibration.VibrationPattern
import kotlinx.coroutines.launch

/** Turns typed text into a Morse code custom pattern. */
@Composable
fun MorseScreen(onDone: () -> Unit) {
    val container = LocalContext.current.appContainer
    var text by remember { mutableStateOf("") }
    var unitMs by remember { mutableFloatStateOf(MorseEncoder.DEFAULT_UNIT_MS.toFloat()) }
    var name by remember { mutableStateOf("") }
    val timings = remember(text, unitMs) { MorseEncoder.encode(text, unitMs.toLong()) }
    val pattern = VibrationPattern("custom:preview", "Morse", PatternCategory.CUSTOM, timings)
    val tooLong = pattern.totalDurationMs > CUSTOM_MAX_DURATION_MS

    ScreenScaffold(title = "Morse code", onBack = onDone) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Type letters or numbers, e.g. W for WhatsApp or GH for GitHub. Short codes are easiest to recognise.")
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(20) },
                label = { Text("Text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                MorseEncoder.toMorseString(text).ifEmpty { "—" },
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
            )
            Text("Speed: ${unitMs.toLong()} ms per dot (lower is faster)")
            Slider(
                value = unitMs,
                onValueChange = { unitMs = it },
                valueRange = MorseEncoder.MIN_UNIT_MS.toFloat()..MorseEncoder.MAX_UNIT_MS.toFloat(),
                steps = ((MorseEncoder.MAX_UNIT_MS - MorseEncoder.MIN_UNIT_MS) / 10 - 1).toInt(),
            )
            if (timings.isNotEmpty()) PatternWaveform(pattern, Modifier.fillMaxWidth().height(40.dp))
            Text(
                "Length ${formatSeconds(pattern.totalDurationMs)}" +
                    if (tooLong) " — too long, max ${formatSeconds(CUSTOM_MAX_DURATION_MS)}. Shorten the text or speed it up." else "",
                color = if (tooLong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            OutlinedButton(onClick = { container.preview(pattern, 1f) }, enabled = timings.isNotEmpty()) { Text("Play") }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Name (optional)") },
                placeholder = { Text("Morse ${text.trim().uppercase()}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val patternName = name.ifBlank { "Morse ${text.trim().uppercase()}" }
                    val saved = timings
                    container.appScope.launch {
                        container.patternRepository.saveCustom(patternName, saved, null, CustomPatternSource.MORSE)
                    }
                    onDone()
                },
                enabled = timings.isNotEmpty() && !tooLong,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save pattern") }
        }
    }
}
