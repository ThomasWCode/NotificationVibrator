package io.github.thomaswcode.notificationvibrator.vibration

/**
 * Records a rhythm from press/release timestamps and turns it into waveform timings.
 *
 * Recording starts at the first press. Presses after [maxDurationMs] are ignored and a press
 * still held at the limit is cut off there. Values are rounded to [quantumMs].
 */
class TapRecorder(
    private val maxDurationMs: Long = MAX_DURATION_MS,
    private val quantumMs: Long = QUANTUM_MS,
) {
    private val presses = mutableListOf<LongArray>() // [start, end] relative to the first press
    private var startTime: Long? = null
    private var pressedAt: Long? = null

    val hasRecording: Boolean get() = presses.isNotEmpty()

    /** Milliseconds recorded so far, measured from the first press to the last release. */
    val recordedMs: Long get() = presses.lastOrNull()?.get(1) ?: 0L

    /** Returns false if the press was ignored because the time limit has been reached. */
    fun press(timeMs: Long): Boolean {
        val start = startTime ?: timeMs.also { startTime = it }
        if (timeMs - start >= maxDurationMs) return false
        pressedAt = timeMs
        return true
    }

    fun release(timeMs: Long) {
        val start = startTime ?: return
        val pressed = pressedAt ?: return
        pressedAt = null
        val begin = pressed - start
        val end = minOf(timeMs - start, maxDurationMs)
        if (end > begin) presses += longArrayOf(begin, end)
    }

    fun reset() {
        presses.clear()
        startTime = null
        pressedAt = null
    }

    /** Timings in the [0, on, off, on, ...] format. Empty if nothing was recorded. */
    fun build(): LongArray {
        if (presses.isEmpty()) return LongArray(0)
        val out = mutableListOf(0L)
        var previousEnd: Long? = null
        for ((begin, end) in presses.map { it[0] to it[1] }) {
            if (previousEnd != null) out += maxOf(MIN_OFF_MS, quantise(begin - previousEnd))
            out += maxOf(MIN_ON_MS, quantise(end - begin))
            previousEnd = end
        }
        return out.toLongArray()
    }

    private fun quantise(ms: Long): Long = ((ms + quantumMs / 2) / quantumMs) * quantumMs

    companion object {
        const val MAX_DURATION_MS = 5000L
        const val QUANTUM_MS = 10L
        const val MIN_ON_MS = 20L
        const val MIN_OFF_MS = 10L
    }
}
