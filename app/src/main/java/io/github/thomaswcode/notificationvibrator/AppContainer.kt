package io.github.thomaswcode.notificationvibrator

import android.content.Context
import io.github.thomaswcode.notificationvibrator.data.ActivityRepository
import io.github.thomaswcode.notificationvibrator.data.AppDatabase
import io.github.thomaswcode.notificationvibrator.data.AppSettings
import io.github.thomaswcode.notificationvibrator.data.AssignmentEntity
import io.github.thomaswcode.notificationvibrator.data.AssignmentRepository
import io.github.thomaswcode.notificationvibrator.data.PatternRepository
import io.github.thomaswcode.notificationvibrator.data.SettingsStore
import io.github.thomaswcode.notificationvibrator.vibration.PatternLibrary
import io.github.thomaswcode.notificationvibrator.vibration.PatternPlayer
import io.github.thomaswcode.notificationvibrator.vibration.VibrationPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Everything the listener needs to route a notification, kept in memory so the hot path never waits on the database. */
data class RoutingSnapshot(
    val assignments: Map<String, AssignmentEntity>,
    val patterns: Map<String, VibrationPattern>,
    val settings: AppSettings,
) {
    fun patternFor(packageName: String): VibrationPattern {
        val id = assignments[packageName]?.patternId ?: settings.defaultPatternId
        return patterns[id] ?: PatternLibrary.default
    }

    fun intensityFor(packageName: String): Float =
        assignments[packageName]?.intensity ?: settings.defaultIntensity

    val defaultPattern: VibrationPattern get() = patterns[settings.defaultPatternId] ?: PatternLibrary.default
}

/** Manual dependency container, created once by [NotificationVibratorApp]. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AppDatabase.build(context)
    val settingsStore = SettingsStore(context)
    val patternPlayer = PatternPlayer(context)
    val assignmentRepository = AssignmentRepository(database.assignmentDao())
    val patternRepository = PatternRepository(database.customPatternDao(), database.assignmentDao(), settingsStore)
    val activityRepository = ActivityRepository(database.seenAppDao(), database.eventLogDao())

    /** True while Android has the notification listener bound. */
    val listenerConnected = MutableStateFlow(false)

    /** Null until the database and settings have loaded once. */
    val snapshot: StateFlow<RoutingSnapshot?> = combine(
        assignmentRepository.assignments,
        patternRepository.patterns,
        settingsStore.settings,
    ) { assignments, patterns, settings ->
        RoutingSnapshot(assignments.associateBy { it.packageName }, patterns.associateBy { it.id }, settings)
    }.stateIn(appScope, SharingStarted.Eagerly, null)

    /** Plays a pattern for previewing, using the vibration type of the current mute mode. */
    fun preview(pattern: VibrationPattern, intensity: Float) {
        val muteMode = snapshot.value?.settings?.muteMode ?: AppSettings().muteMode
        patternPlayer.play(pattern, intensity, muteMode)
    }
}
