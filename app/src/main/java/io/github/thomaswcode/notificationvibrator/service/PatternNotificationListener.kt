package io.github.thomaswcode.notificationvibrator.service

import android.app.Notification
import android.content.ComponentName
import android.media.AudioManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.thomaswcode.notificationvibrator.AppContainer
import io.github.thomaswcode.notificationvibrator.appContainer
import io.github.thomaswcode.notificationvibrator.data.EventLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives every posted notification and plays the pattern for its app (or the default pattern).
 * All decisions run on a single thread so the cooldown and update tracking need no locking.
 */
class PatternNotificationListener : NotificationListenerService() {
    private lateinit var container: AppContainer
    private lateinit var audioManager: AudioManager

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val alertFilter = AlertFilter()

    /** Keys of notifications currently showing, used to recognise updates. */
    private val activeKeys = object : LinkedHashMap<String, Unit>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?): Boolean = size > MAX_TRACKED_KEYS
    }

    override fun onCreate() {
        super.onCreate()
        container = appContainer
        audioManager = getSystemService(AudioManager::class.java)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        container.listenerConnected.value = true
        val keys = runCatching { activeNotifications.map { it.key } }.getOrDefault(emptyList())
        scope.launch { keys.forEach { activeKeys[it] = Unit } }
    }

    override fun onListenerDisconnected() {
        container.listenerConnected.value = false
        requestRebind(ComponentName(this, PatternNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        sbn ?: return
        val ranking = Ranking()
        val hasRanking = rankingMap?.getRanking(sbn.key, ranking) == true
        val notification = sbn.notification
        val flags = notification.flags
        val isSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0
        val input = AlertInput(
            packageName = sbn.packageName,
            isOwnPackage = sbn.packageName == packageName,
            isOngoing = sbn.isOngoing || flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            isCall = notification.category == Notification.CATEGORY_CALL,
            isGroupSummary = isSummary,
            isGroupChild = notification.group != null && !isSummary,
            groupAlert = when (notification.groupAlertBehavior) {
                Notification.GROUP_ALERT_SUMMARY -> GroupAlert.SUMMARY
                Notification.GROUP_ALERT_CHILDREN -> GroupAlert.CHILDREN
                else -> GroupAlert.ALL
            },
            onlyAlertOnce = flags and Notification.FLAG_ONLY_ALERT_ONCE != 0,
            channelImportance = if (hasRanking) ranking.importance else AlertFilter.IMPORTANCE_DEFAULT,
            matchesInterruptionFilter = !hasRanking || ranking.matchesInterruptionFilter(),
            ringerMode = when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
                AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
                else -> RingerMode.NORMAL
            },
            dndActive = currentInterruptionFilter.let {
                it != INTERRUPTION_FILTER_ALL && it != INTERRUPTION_FILTER_UNKNOWN
            },
        )
        val key = sbn.key
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        scope.launch { handle(input, key, now, elapsed) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val key = sbn?.key ?: return
        scope.launch { activeKeys.remove(key) }
    }

    /** [now] is wall time for the log; [elapsed] is monotonic time for the cooldown. */
    private suspend fun handle(posted: AlertInput, key: String, now: Long, elapsed: Long) {
        val input = posted.copy(isUpdate = activeKeys.put(key, Unit) != null)
        val snapshot = container.snapshot.filterNotNull().first()
        val pattern = snapshot.patternFor(input.packageName)
        val settings = snapshot.settings
        val decision = alertFilter.decide(input, pattern.isSilent, settings.cooldownMs, settings.includeSilentChannels, elapsed)

        if (decision == AlertDecision.Play) {
            container.patternPlayer.play(pattern, snapshot.intensityFor(input.packageName), settings.muteMode)
        }

        val reason = (decision as? AlertDecision.Skip)?.reason
        val worthRecording = reason == null ||
            (reason.logged && !(reason == SkipReason.SILENT_CHANNEL && input.isUpdate))
        if (!worthRecording) return
        container.appScope.launch {
            container.activityRepository.recordSeen(input.packageName, now)
            container.activityRepository.log(
                EventLogEntity(
                    time = now,
                    packageName = input.packageName,
                    played = reason == null,
                    patternName = pattern.name,
                    reason = reason?.label ?: "Played",
                ),
            )
        }
    }

    private companion object {
        const val MAX_TRACKED_KEYS = 500
    }
}
