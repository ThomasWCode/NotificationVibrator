package io.github.thomaswcode.notificationvibrator.vibration

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class WaveformsTest {
    private val onOff = VibrationPattern("t:onoff", "On/off", PatternCategory.SINGLE, longArrayOf(0, 100, 50, 100))
    private val shaped = VibrationPattern(
        "t:shape", "Shape", PatternCategory.SHAPE,
        longArrayOf(0, 100, 100, 50, 100),
        intArrayOf(0, 60, 200, 0, 255),
    )

    @Test
    fun onOffPatternsDefaultToFullStrength() {
        assertArrayEquals(intArrayOf(0, 255, 0, 255), Waveforms.baseAmplitudes(onOff))
    }

    @Test
    fun intensityScalesNonZeroAmplitudesOnly() {
        assertArrayEquals(intArrayOf(0, 30, 100, 0, 128), Waveforms.scaledAmplitudes(shaped, 0.5f))
    }

    @Test
    fun intensityIsClampedToSupportedRange() {
        assertArrayEquals(intArrayOf(0, 18, 60, 0, 77), Waveforms.scaledAmplitudes(shaped, 0.01f))
        assertArrayEquals(intArrayOf(0, 60, 200, 0, 255), Waveforms.scaledAmplitudes(shaped, 5f))
    }

    @Test
    fun weakAmplitudesNeverScaleToZero() {
        val faint = VibrationPattern("t:faint", "Faint", PatternCategory.SOFT, longArrayOf(0, 100), intArrayOf(0, 2))
        assertArrayEquals(intArrayOf(0, 1), Waveforms.scaledAmplitudes(faint, 0.3f))
    }

    @Test
    fun toOnOffMergesConsecutiveSegments() {
        assertArrayEquals(longArrayOf(0, 200, 50, 100), Waveforms.toOnOff(shaped.timings, shaped.amplitudes!!))
    }

    @Test
    fun toOnOffStartsWithOffWhenWaveformStartsOn() {
        assertArrayEquals(longArrayOf(0, 300, 100), Waveforms.toOnOff(longArrayOf(100, 200, 100), intArrayOf(80, 255, 0)))
    }
}
