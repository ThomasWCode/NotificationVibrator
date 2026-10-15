package io.github.thomaswcode.notificationvibrator.ui.patterns

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.thomaswcode.notificationvibrator.appContainer
import io.github.thomaswcode.notificationvibrator.data.AppSettings
import io.github.thomaswcode.notificationvibrator.data.CustomPatternSource
import io.github.thomaswcode.notificationvibrator.ui.components.PatternWaveform
import io.github.thomaswcode.notificationvibrator.ui.components.ScreenScaffold
import io.github.thomaswcode.notificationvibrator.ui.components.formatSeconds
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory
import io.github.thomaswcode.notificationvibrator.vibration.TapRecorder
import io.github.thomaswcode.notificationvibrator.vibration.VibrationPattern
import kotlinx.coroutines.launch

/** Hold-to-buzz pad that records a rhythm as a custom pattern. */
@Composable
fun TapRecordScreen(onDone: () -> Unit) {
    val container = LocalContext.current.appContainer
    val recorder = remember { TapRecorder() }
    var timings by remember { mutableStateOf(LongArray(0)) }
    var pressing by remember { mutableStateOf(false) }
    var limitReached by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val preview = VibrationPattern("custom:preview", name.ifBlank { "Recording" }, PatternCategory.CUSTOM, timings)

    DisposableEffect(Unit) { onDispose { container.patternPlayer.cancel() } }

    ScreenScaffold(title = "Record a rhythm", onBack = onDone) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Hold the pad to buzz and let go to stop. Recording starts with your first press and lasts up to ${TapRecorder.MAX_DURATION_MS / 1000} seconds.")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (pressing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            if (recorder.press(SystemClock.uptimeMillis())) {
                                val muteMode = container.snapshot.value?.settings?.muteMode ?: AppSettings().muteMode
                                pressing = true
                                container.patternPlayer.startHold(muteMode)
                                tryAwaitRelease()
                                recorder.release(SystemClock.uptimeMillis())
                                container.patternPlayer.cancel()
                                pressing = false
                                timings = recorder.build()
                            } else {
                                limitReached = true
                            }
                        })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (pressing) "Buzzing…" else "Hold here",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (pressing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                "Recorded ${formatSeconds(preview.totalDurationMs)} of ${formatSeconds(TapRecorder.MAX_DURATION_MS)}" +
                    if (limitReached) " — limit reached" else "",
            )
            if (timings.isNotEmpty()) PatternWaveform(preview, Modifier.fillMaxWidth().height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    recorder.reset()
                    timings = LongArray(0)
                    limitReached = false
                }) { Text("Start over") }
                OutlinedButton(onClick = { container.preview(preview, 1f) }, enabled = timings.isNotEmpty()) { Text("Play") }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val saved = timings
                    container.appScope.launch {
                        container.patternRepository.saveCustom(name, saved, null, CustomPatternSource.TAP)
                    }
                    onDone()
                },
                enabled = timings.isNotEmpty() && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save pattern") }
        }
    }
}
