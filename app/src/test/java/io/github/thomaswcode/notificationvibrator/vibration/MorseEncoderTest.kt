package io.github.thomaswcode.notificationvibrator.vibration

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MorseEncoderTest {
    @Test
    fun singleLetterUsesDotAndDashLengths() {
        // A = .-  -> dot, 1-unit gap, dash
        assertArrayEquals(longArrayOf(0, 100, 100, 300), MorseEncoder.encode("A", 100))
    }

    @Test
    fun lettersAreSeparatedByThreeUnits() {
        // E E -> dot, 3-unit gap, dot
        assertArrayEquals(longArrayOf(0, 50, 150, 50), MorseEncoder.encode("EE", 50))
    }

    @Test
    fun wordsAreSeparatedBySevenUnits() {
        assertArrayEquals(longArrayOf(0, 50, 350, 150), MorseEncoder.encode("E T", 50))
    }

    @Test
    fun lowerCaseAndUnsupportedCharactersAreHandled() {
        assertArrayEquals(MorseEncoder.encode("W", 80), MorseEncoder.encode("w!", 80))
        assertArrayEquals(MorseEncoder.encode("E T", 80), MorseEncoder.encode("?E  T", 80))
    }

    @Test
    fun emptyOrUnsupportedTextGivesEmptyWaveform() {
        assertEquals(0, MorseEncoder.encode("", 80).size)
        assertEquals(0, MorseEncoder.encode("!?  ", 80).size)
    }

    @Test
    fun sosHasNineSymbols() {
        val timings = MorseEncoder.encode("SOS", 70)
        assertEquals(1 + 9 + 8, timings.size)
        assertEquals((3 * 1 + 2 + 3 + 3 * 3 + 2 + 3 + 3 * 1 + 2) * 70L, timings.sum())
    }

    @Test
    fun morseStringIsReadable() {
        assertEquals(".-- / .- .-", MorseEncoder.toMorseString("w  aa"))
    }
}
