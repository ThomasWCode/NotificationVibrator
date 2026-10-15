package io.github.thomaswcode.notificationvibrator.vibration

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.thomaswcode.notificationvibrator.data.MuteMode

/** Plays patterns on the phone's vibration motor. */
class PatternPlayer(context: Context) {
    private val vibrator: Vibrator =
        context.getSystemService(VibratorManager::class.java).defaultVibrator

    val hasAmplitudeControl: Boolean get() = vibrator.hasAmplitudeControl()

    fun play(pattern: VibrationPattern, intensity: Float, muteMode: MuteMode) {
        if (pattern.isSilent || pattern.timings.none { it > 0 }) return
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(pattern.timings, Waveforms.scaledAmplitudes(pattern, intensity), NO_REPEAT)
        } else {
            VibrationEffect.createWaveform(Waveforms.toOnOff(pattern.timings, Waveforms.baseAmplitudes(pattern)), NO_REPEAT)
        }
        vibrator.vibrate(effect, attributesFor(muteMode))
    }

    /** Continuous buzz used as feedback while a finger is held on the tap-to-record pad. */
    fun startHold(muteMode: MuteMode) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(TapRecorder.MAX_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE),
            attributesFor(muteMode),
        )
    }

    fun cancel() = vibrator.cancel()

    private fun attributesFor(muteMode: MuteMode): VibrationAttributes =
        VibrationAttributes.createForUsage(usageFor(muteMode))

    companion object {
        private const val NO_REPEAT = -1

        /**
         * Global mute sets the system notification vibration intensity to 0, so alarm-type
         * vibrations are used; Android does not silence those, so AlertFilter checks Silent/DND itself.
         */
        fun usageFor(muteMode: MuteMode): Int = when (muteMode) {
            MuteMode.GLOBAL -> VibrationAttributes.USAGE_ALARM
            MuteMode.PER_APP -> VibrationAttributes.USAGE_NOTIFICATION
        }
    }
}
