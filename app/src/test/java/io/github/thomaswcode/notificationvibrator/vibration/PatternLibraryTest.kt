package io.github.thomaswcode.notificationvibrator.vibration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternLibraryTest {
    private val playable = PatternLibrary.all.filterNot { it.isSilent }

    @Test
    fun libraryHasAtLeastOneHundredPatterns() {
        assertTrue("only ${playable.size} patterns", playable.size >= 100)
    }

    @Test
    fun idsAreUniqueAndBuiltin() {
        val ids = PatternLibrary.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith(VibrationPattern.BUILTIN_PREFIX) })
    }

    @Test
    fun namesAreUnique() {
        val names = PatternLibrary.all.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun everyPatternIsAValidWaveform() {
        for (pattern in playable) {
            assertTrue("${pattern.id} has no timings", pattern.timings.isNotEmpty())
            assertTrue("${pattern.id} has a negative timing", pattern.timings.all { it >= 0 })
            assertTrue("${pattern.id} is longer than 3 s", pattern.totalDurationMs <= PatternLibrary.MAX_DURATION_MS)
            val amplitudes = Waveforms.baseAmplitudes(pattern)
            assertEquals("${pattern.id} amplitude count", pattern.timings.size, amplitudes.size)
            assertTrue("${pattern.id} amplitude out of range", amplitudes.all { it in 0..255 })
            assertTrue(
                "${pattern.id} never vibrates",
                pattern.timings.indices.any { pattern.timings[it] > 0 && amplitudes[it] > 0 },
            )
        }
    }

    @Test
    fun everyWaveformIsDistinct() {
        val shapes = playable.map { it.timings.toList() to Waveforms.baseAmplitudes(it).toList() }
        val duplicates = shapes.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue("duplicate waveforms: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun requestedExamplePatternsExist() {
        assertNotNull(PatternLibrary.find("builtin:long"))
        assertNotNull(PatternLibrary.find("builtin:double_short"))
        assertNotNull(PatternLibrary.find("builtin:short"))
        assertNotNull(PatternLibrary.find(PatternLibrary.DEFAULT_PATTERN_ID))
    }

    @Test
    fun noVibrateIsFirstAndSilent() {
        val first = PatternLibrary.all.first()
        assertEquals(PatternLibrary.NO_VIBRATE_ID, first.id)
        assertTrue(first.isSilent)
        assertEquals(0L, first.totalDurationMs)
    }

    @Test
    fun morseCoversLettersAndDigits() {
        assertEquals(36, PatternLibrary.all.count { it.category == PatternCategory.MORSE })
    }
}
