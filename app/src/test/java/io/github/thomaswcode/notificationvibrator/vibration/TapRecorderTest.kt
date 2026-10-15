package io.github.thomaswcode.notificationvibrator.vibration

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapRecorderTest {
    @Test
    fun pressesBecomeOnOffTimingsRoundedToTenMs() {
        val recorder = TapRecorder()
        recorder.press(1000); recorder.release(1203)
        recorder.press(1351); recorder.release(1448)
        assertArrayEquals(longArrayOf(0, 200, 150, 100), recorder.build())
    }

    @Test
    fun veryShortTapsAreLengthenedToBeFelt() {
        val recorder = TapRecorder()
        recorder.press(0); recorder.release(3)
        recorder.press(5); recorder.release(9)
        assertArrayEquals(
            longArrayOf(0, TapRecorder.MIN_ON_MS, TapRecorder.MIN_OFF_MS, TapRecorder.MIN_ON_MS),
            recorder.build(),
        )
    }

    @Test
    fun recordingIsCappedAtTheTimeLimit() {
        val recorder = TapRecorder(maxDurationMs = 1000)
        assertTrue(recorder.press(0)); recorder.release(100)
        assertTrue(recorder.press(900)); recorder.release(1500)
        assertFalse(recorder.press(1200))
        assertArrayEquals(longArrayOf(0, 100, 800, 100), recorder.build())
        assertEquals(1000, recorder.recordedMs)
    }

    @Test
    fun roundingUpTinyTapsNeverExceedsTheCap() {
        val recorder = TapRecorder(maxDurationMs = 1000)
        // 200 taps of 2 ms with 3 ms gaps: 1 s raw, but each rounds up to 20 ms on / 10 ms off
        for (i in 0 until 200) {
            recorder.press(i * 5L)
            recorder.release(i * 5L + 2)
        }
        val timings = recorder.build()
        assertTrue("total ${timings.sum()}", timings.sum() <= 1000)
        assertEquals("ends on a buzz", 1, (timings.size - 1) % 2)
    }

    @Test
    fun resetClearsTheRecording() {
        val recorder = TapRecorder()
        recorder.press(0); recorder.release(100)
        recorder.reset()
        assertFalse(recorder.hasRecording)
        assertEquals(0, recorder.build().size)
    }

    @Test
    fun releaseWithoutPressIsIgnored() {
        val recorder = TapRecorder()
        recorder.release(100)
        assertFalse(recorder.hasRecording)
    }
}
