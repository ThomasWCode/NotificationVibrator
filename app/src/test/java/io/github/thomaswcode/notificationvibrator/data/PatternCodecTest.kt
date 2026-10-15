package io.github.thomaswcode.notificationvibrator.data

import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory
import io.github.thomaswcode.notificationvibrator.vibration.VibrationPattern
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatternCodecTest {
    @Test
    fun timingsRoundTrip() {
        val timings = longArrayOf(0, 120, 80, 400)
        assertArrayEquals(timings, PatternCodec.decodeTimings(PatternCodec.encode(timings)))
    }

    @Test
    fun amplitudesRoundTrip() {
        val amplitudes = intArrayOf(0, 255, 0, 40)
        assertArrayEquals(amplitudes, PatternCodec.decodeAmplitudes(PatternCodec.encode(amplitudes)))
    }

    @Test
    fun blankTextDecodesToEmpty() {
        assertEquals(0, PatternCodec.decodeTimings("").size)
    }

    @Test
    fun customEntityBecomesCustomPattern() {
        val entity = CustomPatternEntity(id = 7, name = "Mine", timings = "0,100,50,100", amplitudes = null, source = "TAP", createdAt = 1)
        val pattern = entity.toPattern()
        assertEquals("custom:7", pattern.id)
        assertEquals(PatternCategory.CUSTOM, pattern.category)
        assertArrayEquals(longArrayOf(0, 100, 50, 100), pattern.timings)
        assertNull(pattern.amplitudes)
        assertEquals(true, pattern.isCustom)
        assertEquals(false, VibrationPattern("builtin:x", "x", PatternCategory.SINGLE, longArrayOf(0, 1)).isCustom)
    }
}
