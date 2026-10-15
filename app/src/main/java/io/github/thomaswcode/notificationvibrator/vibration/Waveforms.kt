package io.github.thomaswcode.notificationvibrator.vibration

import kotlin.math.roundToInt

/** Pure waveform maths used by [PatternPlayer], kept free of Android classes so it can be unit tested. */
object Waveforms {
    const val MAX_AMPLITUDE = 255
    const val MIN_INTENSITY = 0.3f
    const val MAX_INTENSITY = 1f

    /** Explicit amplitudes, or full strength on odd segments for plain on/off patterns. */
    fun baseAmplitudes(pattern: VibrationPattern): IntArray =
        pattern.amplitudes ?: IntArray(pattern.timings.size) { if (it % 2 == 1) MAX_AMPLITUDE else 0 }

    /** Scales every non-zero amplitude by [intensity], keeping it within 1..255 so it stays on. */
    fun scaledAmplitudes(pattern: VibrationPattern, intensity: Float): IntArray {
        val factor = intensity.coerceIn(MIN_INTENSITY, MAX_INTENSITY)
        return baseAmplitudes(pattern)
            .map { if (it == 0) 0 else (it * factor).roundToInt().coerceIn(1, MAX_AMPLITUDE) }
            .toIntArray()
    }

    /**
     * Collapses an amplitude waveform to alternating off/on timings for motors without
     * amplitude control. Consecutive segments in the same state are merged; output starts with off.
     */
    fun toOnOff(timings: LongArray, amplitudes: IntArray): LongArray {
        require(timings.size == amplitudes.size) { "timings and amplitudes differ in length" }
        val out = mutableListOf<Long>()
        var currentOn = false
        var current = 0L
        for (i in timings.indices) {
            val on = amplitudes[i] > 0
            if (on == currentOn) {
                current += timings[i]
            } else {
                out += current
                currentOn = on
                current = timings[i]
            }
        }
        out += current
        return out.toLongArray()
    }
}
