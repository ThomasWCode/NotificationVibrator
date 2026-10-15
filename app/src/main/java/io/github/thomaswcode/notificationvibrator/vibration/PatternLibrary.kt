package io.github.thomaswcode.notificationvibrator.vibration

import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory.DOUBLE
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory.MORSE
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory.MULTI
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory.NONE
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory.RHYTHM
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory.SHAPE
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory.SINGLE
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory.SOFT

/** The built-in pattern library. All patterns except No vibrate last at most [MAX_DURATION_MS]. */
object PatternLibrary {
    const val NO_VIBRATE_ID = "builtin:none"
    const val DEFAULT_PATTERN_ID = "builtin:medium"
    const val MAX_DURATION_MS = 3000L
    const val MORSE_UNIT_MS = 80L
    private const val SOFT_AMPLITUDE = 90
    private const val WHISPER_AMPLITUDE = 45

    val NO_VIBRATE = VibrationPattern(NO_VIBRATE_ID, "No vibrate", NONE, LongArray(0))

    val all: List<VibrationPattern> = buildList {
        add(NO_VIBRATE)

        // Single
        add(onOff("tick", "Tick", SINGLE, 25))
        add(onOff("blip", "Blip", SINGLE, 50))
        add(onOff("short", "Short", SINGLE, 90))
        add(onOff("medium", "Medium", SINGLE, 200))
        add(onOff("firm", "Firm", SINGLE, 320))
        add(onOff("long", "Long", SINGLE, 500))
        add(onOff("extra_long", "Extra long", SINGLE, 800))
        add(onOff("marathon", "Marathon", SINGLE, 1400))

        // Double
        add(onOff("double_tick", "Double tick", DOUBLE, 25, 90, 25))
        add(onOff("double_short", "Double short", DOUBLE, 90, 110, 90))
        add(onOff("double_medium", "Double medium", DOUBLE, 200, 150, 200))
        add(onOff("double_long", "Double long", DOUBLE, 450, 200, 450))
        add(onOff("short_long", "Short then long", DOUBLE, 90, 150, 450))
        add(onOff("long_short", "Long then short", DOUBLE, 450, 150, 90))
        add(onOff("rapid_double", "Rapid double", DOUBLE, 50, 50, 50))
        add(onOff("lazy_double", "Lazy double", DOUBLE, 150, 450, 150))
        add(onOff("tick_long", "Tick then long", DOUBLE, 25, 150, 500))
        add(onOff("long_tick", "Long then tick", DOUBLE, 500, 150, 25))

        // Triple and more
        add(onOff("triple_tick", "Triple tick", MULTI, *repeatPulse(25, 90, 3)))
        add(onOff("triple_short", "Triple short", MULTI, *repeatPulse(90, 110, 3)))
        add(onOff("triple_medium", "Triple medium", MULTI, *repeatPulse(200, 150, 3)))
        add(onOff("triple_long", "Triple long", MULTI, *repeatPulse(400, 200, 3)))
        add(onOff("quad_short", "Four short", MULTI, *repeatPulse(90, 100, 4)))
        add(onOff("staccato", "Staccato", MULTI, *repeatPulse(60, 70, 5)))
        add(onOff("drumroll", "Drumroll", MULTI, *repeatPulse(30, 30, 12)))
        add(onOff("waltz", "Waltz", MULTI, 450, 150, 120, 120, 120))
        add(onOff("gallop", "Gallop", MULTI, 80, 60, 80, 250, 80, 60, 80))
        add(onOff("rising", "Rising", MULTI, 80, 120, 200, 120, 400))
        add(onOff("falling", "Falling", MULTI, 400, 120, 200, 120, 80))
        add(onOff("countdown", "Countdown", MULTI, 60, 250, 60, 250, 60, 250, 350))

        // Rhythms
        add(onOff("heartbeat", "Heartbeat", RHYTHM, 80, 120, 160, 600, 80, 120, 160))
        add(onOff("heartbeat_fast", "Racing heart", RHYTHM, 60, 80, 120, 300, 60, 80, 120, 300, 60, 80, 120))
        add(onOff("knock_knock", "Knock knock", RHYTHM, 70, 180, 70, 500, 70, 180, 70))
        add(onOff("shave_haircut", "Shave and a haircut", RHYTHM, 120, 180, 60, 60, 60, 60, 120, 180, 120, 420, 120, 180, 120))
        add(onOff("cha_cha", "Cha-cha", RHYTHM, 150, 150, 150, 150, 80, 80, 80, 80, 150))
        add(onOff("tango", "Tango", RHYTHM, 300, 150, 150, 150, 100, 100, 300))
        add(pattern("sos", "SOS", RHYTHM, MorseEncoder.encode("SOS", 70)))
        add(onOff("metronome", "Metronome", RHYTHM, *repeatPulse(40, 460, 4)))
        add(onOff("telephone", "Telephone", RHYTHM, *repeatPulse(40, 40, 8), 400, *repeatPulse(40, 40, 8)))
        add(onOff("doorbell", "Doorbell", RHYTHM, 300, 200, 500))
        add(onOff("clock_tick", "Clock", RHYTHM, *repeatPulse(20, 330, 5)))
        add(onOff("syncopated", "Syncopated", RHYTHM, 100, 100, 200, 300, 100, 100, 200))
        add(onOff("samba", "Samba", RHYTHM, 80, 60, 80, 60, 200, 150, 80, 60, 80))
        add(onOff("alarm_bell", "Alarm bell", RHYTHM, *repeatPulse(100, 100, 6)))
        add(onOff("morse_ok", "OK", RHYTHM, *trimLeadingDelay(MorseEncoder.encode("OK", 60))))

        // Shapes (amplitude changes)
        add(shape("fade_in", "Fade in") { ramp(600, 8, 30, 255) })
        add(shape("fade_out", "Fade out") { ramp(600, 8, 255, 30) })
        add(shape("swell", "Swell") { ramp(400, 6, 30, 255); ramp(400, 6, 255, 30) })
        add(shape("pulse_wave", "Pulse wave") { repeat(3) { ramp(150, 3, 60, 255); pause(100) } })
        add(shape("siren", "Siren") { repeat(2) { ramp(300, 5, 60, 255); ramp(300, 5, 255, 60) } })
        add(shape("bounce", "Bounce") {
            hold(150, 255); pause(120); hold(110, 170); pause(100); hold(80, 110); pause(80); hold(60, 70)
        })
        add(shape("double_swell", "Double swell") {
            ramp(200, 4, 40, 255); ramp(200, 4, 255, 40); pause(150); ramp(200, 4, 40, 255); ramp(200, 4, 255, 40)
        })
        add(shape("earthquake", "Earthquake") {
            hold(90, 200); hold(60, 120); hold(110, 255); hold(70, 90)
            hold(100, 220); hold(80, 150); hold(120, 255); hold(60, 100)
        })
        add(shape("breathe", "Breathe") { ramp(700, 7, 20, 180); ramp(700, 7, 180, 20) })
        add(shape("ripple", "Ripple") {
            hold(80, 255); pause(80); hold(80, 190); pause(80); hold(80, 130); pause(80); hold(80, 80); pause(80); hold(80, 40)
        })
        add(shape("ramp_double", "Double ramp") { ramp(300, 5, 40, 255); pause(150); ramp(300, 5, 40, 255) })

        // Soft
        add(soft("soft_tick", "Soft tick", SOFT_AMPLITUDE, 25))
        add(soft("soft_short", "Soft short", SOFT_AMPLITUDE, 90))
        add(soft("soft_medium", "Soft medium", SOFT_AMPLITUDE, 200))
        add(soft("soft_long", "Soft long", SOFT_AMPLITUDE, 500))
        add(soft("soft_double", "Soft double", SOFT_AMPLITUDE, 90, 110, 90))
        add(soft("soft_triple", "Soft triple", SOFT_AMPLITUDE, *repeatPulse(90, 110, 3)))
        add(soft("soft_heartbeat", "Soft heartbeat", SOFT_AMPLITUDE, 80, 120, 160, 600, 80, 120, 160))
        add(soft("whisper", "Whisper", WHISPER_AMPLITUDE, 700))

        // Morse letters and digits
        MorseEncoder.CODES.keys.sorted().forEach { ch ->
            add(pattern("morse_${ch.lowercaseChar()}", "Morse $ch", MORSE, MorseEncoder.encode(ch.toString(), MORSE_UNIT_MS)))
        }
    }

    private val byId: Map<String, VibrationPattern> = all.associateBy { it.id }

    fun find(id: String): VibrationPattern? = byId[id]

    val default: VibrationPattern get() = byId.getValue(DEFAULT_PATTERN_ID)

    private fun pattern(slug: String, name: String, category: PatternCategory, timings: LongArray, amplitudes: IntArray? = null) =
        VibrationPattern(VibrationPattern.BUILTIN_PREFIX + slug, name, category, timings, amplitudes)

    /** [onOffMs] alternates on, off, on, ... starting with an on segment. */
    private fun onOff(slug: String, name: String, category: PatternCategory, vararg onOffMs: Long) =
        pattern(slug, name, category, longArrayOf(0L, *onOffMs))

    private fun soft(slug: String, name: String, amplitude: Int, vararg onOffMs: Long): VibrationPattern {
        val timings = longArrayOf(0L, *onOffMs)
        return pattern(slug, name, SOFT, timings, IntArray(timings.size) { if (it % 2 == 1) amplitude else 0 })
    }

    private fun shape(slug: String, name: String, build: ShapeBuilder.() -> Unit): VibrationPattern {
        val builder = ShapeBuilder().apply(build)
        return pattern(slug, name, SHAPE, builder.timings.toLongArray(), builder.amplitudes.toIntArray())
    }

    /** [count] pulses of [onMs] separated by [offMs], without a trailing pause. */
    private fun repeatPulse(onMs: Long, offMs: Long, count: Int): LongArray =
        LongArray(count * 2 - 1) { if (it % 2 == 0) onMs else offMs }

    private fun trimLeadingDelay(timings: LongArray): LongArray = timings.copyOfRange(1, timings.size)

    /** Builds amplitude waveforms from held, paused and ramped segments. */
    private class ShapeBuilder {
        val timings = mutableListOf(0L)
        val amplitudes = mutableListOf(0)

        fun hold(ms: Long, amplitude: Int) {
            timings += ms
            amplitudes += amplitude
        }

        fun pause(ms: Long) = hold(ms, 0)

        fun ramp(totalMs: Long, steps: Int, from: Int, to: Int) {
            val stepMs = totalMs / steps
            for (i in 0 until steps) hold(stepMs, from + (to - from) * i / (steps - 1))
        }
    }
}
