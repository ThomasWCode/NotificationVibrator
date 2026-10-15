package io.github.thomaswcode.notificationvibrator.ui.setup

import android.Manifest
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.thomaswcode.notificationvibrator.appContainer
import io.github.thomaswcode.notificationvibrator.data.AppSettings
import io.github.thomaswcode.notificationvibrator.data.MuteMode
import io.github.thomaswcode.notificationvibrator.ui.components.AppIcon
import io.github.thomaswcode.notificationvibrator.ui.components.ScreenScaffold
import io.github.thomaswcode.notificationvibrator.ui.components.formatSeconds
import io.github.thomaswcode.notificationvibrator.ui.components.rememberAppLabel
import io.github.thomaswcode.notificationvibrator.vibration.PatternLibrary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val container = context.appContainer
    val settings = container.snapshot.collectAsStateWithLifecycle().value?.settings ?: AppSettings()
    val listenerConnected by container.listenerConnected.collectAsStateWithLifecycle()
    val seenApps by container.activityRepository.seenApps.collectAsStateWithLifecycle(initialValue = emptyList())
    val assignments by container.assignmentRepository.assignments.collectAsStateWithLifecycle(initialValue = emptyList())

    var hasAccess by remember { mutableStateOf(SystemSettings.hasNotificationAccess(context)) }
    var batteryUnrestricted by remember { mutableStateOf(SystemSettings.isBatteryUnrestricted(context)) }
    var ringerMode by remember { mutableIntStateOf(AudioManager.RINGER_MODE_VIBRATE) }
    var dndActive by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }

    // Statuses can change while the user is in system settings, so re-check whenever the screen resumes
    LifecycleResumeEffect(Unit) {
        hasAccess = SystemSettings.hasNotificationAccess(context)
        batteryUnrestricted = SystemSettings.isBatteryUnrestricted(context)
        ringerMode = context.getSystemService(AudioManager::class.java).ringerMode
        dndActive = SystemSettings.isDndActive(context)
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        testMessage = if (granted && SystemSettings.postSystemBuzzTest(context)) {
            TEST_1_POSTED
        } else {
            "Notification permission was refused, so the test notification can't be shown."
        }
    }

    ScreenScaffold(title = "Setup") { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (dndActive) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        "Do Not Disturb is on, so only apps and people that Do Not Disturb allows will buzz.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "The phone is in Silent mode, so nothing will vibrate. Switch to Vibrate mode to use your patterns.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            SetupCard("1. Notification access", done = hasAccess) {
                Text(
                    when {
                        !hasAccess -> "Lets this app see which app sent each notification. It only reads the app name; nothing leaves the phone."
                        listenerConnected -> "Granted and running."
                        else -> "Granted. Android connects the listener shortly after it's turned on."
                    },
                )
                if (!hasAccess) {
                    Text(
                        "If the switch is greyed out or says \"Restricted setting\": open App info, tap ⋮ (top right), " +
                            "choose \"Allow restricted settings\", then come back and try again.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { SystemSettings.openNotificationAccess(context) }) { Text("Turn on access") }
                        OutlinedButton(onClick = { SystemSettings.openAppInfo(context) }) { Text("Open App info") }
                    }
                }
            }

            SetupCard("2. Keep running in the background", done = batteryUnrestricted) {
                Text(
                    if (batteryUnrestricted) {
                        "Battery use is Unrestricted."
                    } else {
                        "Samsung can stop background apps to save battery. Allow unrestricted battery use."
                    },
                )
                if (!batteryUnrestricted) {
                    FilledTonalButton(onClick = { SystemSettings.requestUnrestrictedBattery(context) }) { Text("Allow unrestricted") }
                }
                Text(
                    "Also add this app to Samsung's never-sleeping list: Settings → Battery → Background usage limits → " +
                        "Never sleeping apps (\"Never auto sleeping apps\" on some versions) → + → Notification Vibrator.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            SetupCard("3. Silence the other apps' own vibration", done = null) {
                Text("Android won't let this app block another app's vibration, so turn it off in Samsung settings using one of these:")
                MuteModeOption(
                    selected = settings.muteMode == MuteMode.GLOBAL,
                    title = "Global mute (recommended)",
                    subtitle = "One setting silences every app's notification buzz.",
                ) { container.appScope.launch { container.settingsStore.setMuteMode(MuteMode.GLOBAL) } }
                MuteModeOption(
                    selected = settings.muteMode == MuteMode.PER_APP,
                    title = "Per-app mute",
                    subtitle = "Turn vibration off app by app. Use this if the global tests below fail.",
                ) { container.appScope.launch { container.settingsStore.setMuteMode(MuteMode.PER_APP) } }
                HorizontalDivider()

                if (settings.muteMode == MuteMode.GLOBAL) {
                    Text(
                        "1. Keep the phone in Vibrate (or Sound) mode — not Silent.\n" +
                            "2. Settings → Sounds and vibration → Vibration intensity → drag Notifications to 0.\n" +
                            "3. Run both tests below.",
                    )
                    OutlinedButton(onClick = { SystemSettings.openSoundSettings(context) }) { Text("Open Sounds and vibration") }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = {
                            if (SystemSettings.canPostNotifications(context)) {
                                SystemSettings.postSystemBuzzTest(context)
                                testMessage = TEST_1_POSTED
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }) { Text("Test 1: system buzz off") }
                        FilledTonalButton(onClick = {
                            PatternLibrary.find("builtin:double_medium")?.let { container.patternPlayer.play(it, 1f, MuteMode.GLOBAL) }
                            testMessage = "Test 2: you should have felt two buzzes. If not, this phone mutes alarm-type " +
                                "vibrations too: switch to Per-app mute and put the Notifications intensity back up."
                        }) { Text("Test 2: pattern plays") }
                    }
                } else {
                    Text(
                        "For each app below: open its notification settings → Notification categories → each category → " +
                            "turn vibration off. If there is no vibration switch, set the category to Silent and turn on the switch below " +
                            "(Silent categories also lose their pop-up banners).",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibrate for Silent categories", Modifier.weight(1f))
                        Switch(
                            checked = settings.includeSilentChannels,
                            onCheckedChange = { checked ->
                                container.appScope.launch { container.settingsStore.setIncludeSilentChannels(checked) }
                            },
                        )
                    }
                    FilledTonalButton(onClick = {
                        PatternLibrary.find("builtin:double_medium")?.let { container.patternPlayer.play(it, 1f, MuteMode.PER_APP) }
                        testMessage = "You should have felt two buzzes."
                    }) { Text("Test: pattern plays") }
                    val packages = (assignments.map { it.packageName } + seenApps.map { it.packageName }).distinct()
                    if (packages.isEmpty()) {
                        Text(
                            "Apps appear here once they've sent a notification, or once you assign them a pattern.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    packages.forEach { packageName -> AppSettingsRow(packageName) }
                }
                testMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            SetupCard("4. Repeat notifications", done = null) {
                var cooldown by remember(settings.cooldownMs) { mutableFloatStateOf(settings.cooldownMs.toFloat()) }
                Text(
                    if (cooldown < 1f) {
                        "Every notification buzzes, even in quick bursts."
                    } else {
                        "After a buzz, ignore the same app for ${formatSeconds(cooldown.toLong())} so a burst of messages buzzes once."
                    },
                )
                Slider(
                    value = cooldown,
                    onValueChange = { cooldown = it },
                    onValueChangeFinished = {
                        container.appScope.launch { container.settingsStore.setCooldownMs(cooldown.toLong()) }
                    },
                    valueRange = 0f..AppSettings.MAX_COOLDOWN_MS.toFloat(),
                    steps = (AppSettings.MAX_COOLDOWN_MS / COOLDOWN_STEP_MS - 1).toInt(),
                )
            }
        }
    }
}

private const val TEST_1_POSTED =
    "Test 1: a test notification was posted. If the phone buzzed, the Notifications intensity isn't at 0 yet."
private const val COOLDOWN_STEP_MS = 250L

@Composable
private fun SetupCard(title: String, done: Boolean?, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                when (done) {
                    true -> Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary)
                    false -> Icon(Icons.Default.Warning, contentDescription = "Needs attention", tint = MaterialTheme.colorScheme.error)
                    null -> Unit
                }
            }
            content()
        }
    }
}

@Composable
private fun MuteModeOption(selected: Boolean, title: String, subtitle: String, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppSettingsRow(packageName: String) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(packageName, size = 32.dp)
        Spacer(Modifier.width(8.dp))
        Text(rememberAppLabel(packageName), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        TextButton(onClick = { SystemSettings.openAppNotificationSettings(context, packageName) }) { Text("Open") }
    }
}
