package io.github.thomaswcode.notificationvibrator.vibration

/** Groups shown in the pattern picker, in display order. */
enum class PatternCategory(val label: String) {
    NONE("No vibration"),
    CUSTOM("Your patterns"),
    SINGLE("Single"),
    DOUBLE("Double"),
    MULTI("Triple and more"),
    RHYTHM("Rhythms"),
    SHAPE("Shapes (strength changes)"),
    SOFT("Soft"),
    MORSE("Morse code"),
}

/**
 * A vibration waveform in the format used by VibrationEffect.createWaveform.
 *
 * [timings] are segment durations in ms. Segment 0 is a (usually zero-length) delay.
 * [amplitudes] gives each segment a strength from 0 (off) to 255, or is null for a plain
 * on/off pattern where odd segments are on and even segments are off.
 */
class VibrationPattern(
    val id: String,
    val name: String,
    val category: PatternCategory,
    val timings: LongArray,
    val amplitudes: IntArray? = null,
) {
    /** True for the "No vibrate" option. */
    val isSilent: Boolean get() = id == PatternLibrary.NO_VIBRATE_ID

    val isCustom: Boolean get() = id.startsWith(CUSTOM_PREFIX)

    val totalDurationMs: Long get() = timings.sum()

    override fun equals(other: Any?): Boolean =
        other is VibrationPattern &&
            id == other.id &&
            name == other.name &&
            category == other.category &&
            timings.contentEquals(other.timings) &&
            amplitudes.contentEquals(other.amplitudes)

    override fun hashCode(): Int =
        listOf(id, name, category, timings.contentHashCode(), amplitudes.contentHashCode()).hashCode()

    override fun toString(): String = "VibrationPattern($id, $name)"

    companion object {
        const val BUILTIN_PREFIX = "builtin:"
        const val CUSTOM_PREFIX = "custom:"
    }
}
