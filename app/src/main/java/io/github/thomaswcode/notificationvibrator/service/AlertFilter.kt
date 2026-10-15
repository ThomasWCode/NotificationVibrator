package io.github.thomaswcode.notificationvibrator.service

enum class RingerMode { SILENT, VIBRATE, NORMAL }

/** Mirrors Notification.GROUP_ALERT_ALL / GROUP_ALERT_SUMMARY / GROUP_ALERT_CHILDREN. */
enum class GroupAlert { ALL, SUMMARY, CHILDREN }

/** Everything the filter needs about one posted notification, free of Android classes. */
data class AlertInput(
    val packageName: String,
    val isOwnPackage: Boolean = false,
    val isOngoing: Boolean = false,
    val isCall: Boolean = false,
    val isGroupSummary: Boolean = false,
    /** In a group but not its summary. */
    val isGroupChild: Boolean = false,
    val groupAlert: GroupAlert = GroupAlert.ALL,
    val onlyAlertOnce: Boolean = false,
    /** A notification with the same key was already showing. */
    val isUpdate: Boolean = false,
    val channelImportance: Int = AlertFilter.IMPORTANCE_DEFAULT,
    val matchesInterruptionFilter: Boolean = true,
    val ringerMode: RingerMode = RingerMode.VIBRATE,
    /** Do Not Disturb is on (any interruption filter other than "all"). */
    val dndActive: Boolean = false,
)

/** Why a notification did not vibrate. [logged] reasons appear in the activity log. */
enum class SkipReason(val label: String, val logged: Boolean) {
    OWN_APP("Own notification", false),
    ONGOING("Ongoing notification", false),
    CALL("Incoming call", true),
    GROUP_SUMMARY("Group summary", false),
    GROUP_CHILD_SILENCED("Group member (app alerts via summary)", false),
    ALERT_ONCE_UPDATE("Update to a notification already shown", false),
    SILENT_CHANNEL("Silent notification category", true),
    NO_VIBRATE("Set to No vibrate", true),
    SILENT_MODE("Phone in Silent mode", true),
    DO_NOT_DISTURB("Blocked by Do Not Disturb", true),
    COOLDOWN("Buzzed for this app moments ago", true),
}

sealed interface AlertDecision {
    data object Play : AlertDecision
    data class Skip(val reason: SkipReason) : AlertDecision
}

/**
 * Decides whether a posted notification should play its pattern. Mirrors when Android itself would
 * have alerted, and enforces Silent mode and Do Not Disturb, which Android does not apply to the
 * alarm-type vibrations used in global mute mode.
 *
 * Not thread-safe: call from a single thread.
 */
class AlertFilter {
    private val lastPlayedAt = HashMap<String, Long>()

    fun decide(
        input: AlertInput,
        patternIsSilent: Boolean,
        cooldownMs: Long,
        includeSilentChannels: Boolean,
        nowMs: Long,
    ): AlertDecision {
        val reason = skipReason(input, patternIsSilent, includeSilentChannels)
            ?: cooldownReason(input.packageName, cooldownMs, nowMs)
        if (reason != null) return AlertDecision.Skip(reason)
        lastPlayedAt[input.packageName] = nowMs
        return AlertDecision.Play
    }

    private fun skipReason(input: AlertInput, patternIsSilent: Boolean, includeSilentChannels: Boolean): SkipReason? =
        when {
            input.isOwnPackage -> SkipReason.OWN_APP
            // Calls are usually also ongoing; check them first so they get the logged reason
            input.isCall -> SkipReason.CALL
            input.isOngoing -> SkipReason.ONGOING
            // Like Android: only GROUP_ALERT_CHILDREN silences a summary
            input.isGroupSummary && input.groupAlert == GroupAlert.CHILDREN -> SkipReason.GROUP_SUMMARY
            input.isGroupChild && input.groupAlert == GroupAlert.SUMMARY -> SkipReason.GROUP_CHILD_SILENCED
            input.onlyAlertOnce && input.isUpdate -> SkipReason.ALERT_ONCE_UPDATE
            !isAlertingImportance(input.channelImportance, includeSilentChannels) -> SkipReason.SILENT_CHANNEL
            patternIsSilent -> SkipReason.NO_VIBRATE
            // While DND is on, Android (notably One UI) reports the ringer as Silent even when the
            // sound mode is Vibrate, so DND decides via matchesInterruptionFilter instead
            input.ringerMode == RingerMode.SILENT && !input.dndActive -> SkipReason.SILENT_MODE
            !input.matchesInterruptionFilter -> SkipReason.DO_NOT_DISTURB
            else -> null
        }

    private fun isAlertingImportance(importance: Int, includeSilentChannels: Boolean): Boolean =
        importance >= IMPORTANCE_DEFAULT || (includeSilentChannels && importance == IMPORTANCE_LOW)

    private fun cooldownReason(packageName: String, cooldownMs: Long, nowMs: Long): SkipReason? {
        val last = lastPlayedAt[packageName] ?: return null
        return if (nowMs - last < cooldownMs) SkipReason.COOLDOWN else null
    }

    companion object {
        /** Same values as NotificationManager.IMPORTANCE_LOW / IMPORTANCE_DEFAULT. */
        const val IMPORTANCE_LOW = 2
        const val IMPORTANCE_DEFAULT = 3
    }
}
