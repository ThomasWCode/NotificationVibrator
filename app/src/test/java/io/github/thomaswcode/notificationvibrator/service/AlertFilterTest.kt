package io.github.thomaswcode.notificationvibrator.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertFilterTest {
    private val filter = AlertFilter()
    private val message = AlertInput(packageName = "com.whatsapp")

    private fun decide(
        input: AlertInput = message,
        patternIsSilent: Boolean = false,
        cooldownMs: Long = 1500,
        includeSilentChannels: Boolean = false,
        nowMs: Long = 10_000,
    ) = filter.decide(input, patternIsSilent, cooldownMs, includeSilentChannels, nowMs)

    private fun skip(reason: SkipReason) = AlertDecision.Skip(reason)

    @Test
    fun ordinaryMessagePlays() = assertEquals(AlertDecision.Play, decide())

    @Test
    fun playsInSoundModeToo() = assertEquals(AlertDecision.Play, decide(message.copy(ringerMode = RingerMode.NORMAL)))

    @Test
    fun ownNotificationsAreSkipped() =
        assertEquals(skip(SkipReason.OWN_APP), decide(message.copy(isOwnPackage = true)))

    @Test
    fun ongoingNotificationsAreSkipped() =
        assertEquals(skip(SkipReason.ONGOING), decide(message.copy(isOngoing = true)))

    @Test
    fun callsAreSkipped() = assertEquals(skip(SkipReason.CALL), decide(message.copy(isCall = true)))

    @Test
    fun groupSummaryIsSkippedUnlessAppAlertsViaSummary() {
        assertEquals(skip(SkipReason.GROUP_SUMMARY), decide(message.copy(isGroupSummary = true)))
        assertEquals(
            skip(SkipReason.GROUP_SUMMARY),
            decide(message.copy(isGroupSummary = true, groupAlert = GroupAlert.CHILDREN)),
        )
        assertEquals(AlertDecision.Play, decide(message.copy(isGroupSummary = true, groupAlert = GroupAlert.SUMMARY)))
    }

    @Test
    fun groupChildIsSkippedWhenAppAlertsViaSummary() {
        assertEquals(
            skip(SkipReason.GROUP_CHILD_SILENCED),
            decide(message.copy(isGroupChild = true, groupAlert = GroupAlert.SUMMARY)),
        )
        assertEquals(AlertDecision.Play, decide(message.copy(isGroupChild = true, groupAlert = GroupAlert.CHILDREN)))
    }

    @Test
    fun alertOnceUpdatesAreSkippedButFirstPostPlays() {
        assertEquals(
            skip(SkipReason.ALERT_ONCE_UPDATE),
            decide(message.copy(onlyAlertOnce = true, isUpdate = true)),
        )
        assertEquals(AlertDecision.Play, decide(message.copy(onlyAlertOnce = true, isUpdate = false), nowMs = 20_000))
    }

    @Test
    fun updatesWithoutAlertOncePlayLikeAndroidDoes() =
        assertEquals(AlertDecision.Play, decide(message.copy(isUpdate = true)))

    @Test
    fun silentCategoriesAreSkippedUnlessIncluded() {
        val low = message.copy(channelImportance = AlertFilter.IMPORTANCE_LOW)
        assertEquals(skip(SkipReason.SILENT_CHANNEL), decide(low))
        assertEquals(AlertDecision.Play, decide(low, includeSilentChannels = true))
    }

    @Test
    fun minimisedAndBlockedCategoriesAreAlwaysSkipped() {
        assertEquals(skip(SkipReason.SILENT_CHANNEL), decide(message.copy(channelImportance = 1), includeSilentChannels = true))
        assertEquals(skip(SkipReason.SILENT_CHANNEL), decide(message.copy(channelImportance = 0), includeSilentChannels = true))
    }

    @Test
    fun highImportancePlays() = assertEquals(AlertDecision.Play, decide(message.copy(channelImportance = 4)))

    @Test
    fun noVibrateAssignmentIsSkipped() =
        assertEquals(skip(SkipReason.NO_VIBRATE), decide(patternIsSilent = true))

    @Test
    fun silentModeIsRespected() =
        assertEquals(skip(SkipReason.SILENT_MODE), decide(message.copy(ringerMode = RingerMode.SILENT)))

    @Test
    fun doNotDisturbIsRespected() =
        assertEquals(skip(SkipReason.DO_NOT_DISTURB), decide(message.copy(matchesInterruptionFilter = false)))

    @Test
    fun cooldownStopsBurstsPerApp() {
        assertEquals(AlertDecision.Play, decide(nowMs = 10_000))
        assertEquals(skip(SkipReason.COOLDOWN), decide(nowMs = 10_500))
        assertEquals(skip(SkipReason.COOLDOWN), decide(nowMs = 11_499))
        assertEquals(AlertDecision.Play, decide(nowMs = 11_500))
    }

    @Test
    fun cooldownIsPerApp() {
        assertEquals(AlertDecision.Play, decide(nowMs = 10_000))
        assertEquals(AlertDecision.Play, decide(message.copy(packageName = "com.google.android.gm"), nowMs = 10_100))
    }

    @Test
    fun skippedNotificationsDoNotStartTheCooldown() {
        assertEquals(skip(SkipReason.SILENT_MODE), decide(message.copy(ringerMode = RingerMode.SILENT), nowMs = 10_000))
        assertEquals(AlertDecision.Play, decide(nowMs = 10_100))
    }

    @Test
    fun zeroCooldownAllowsEveryNotification() {
        assertEquals(AlertDecision.Play, decide(cooldownMs = 0, nowMs = 10_000))
        assertEquals(AlertDecision.Play, decide(cooldownMs = 0, nowMs = 10_000))
    }
}
