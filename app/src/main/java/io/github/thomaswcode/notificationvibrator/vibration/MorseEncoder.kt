package io.github.thomaswcode.notificationvibrator.vibration

/**
 * Converts text to a Morse code vibration waveform.
 *
 * Standard Morse timing: dot = 1 unit, dash = 3 units, gap inside a letter = 1 unit,
 * gap between letters = 3 units, gap between words = 7 units. Unsupported characters are ignored.
 */
object MorseEncoder {
    const val DEFAULT_UNIT_MS = 90L
    const val MIN_UNIT_MS = 60L
    const val MAX_UNIT_MS = 150L

    val CODES: Map<Char, String> = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
    )

    /** Returns the waveform timings ([0, on, off, on, ...]), or an empty array if nothing is encodable. */
    fun encode(text: String, unitMs: Long = DEFAULT_UNIT_MS): LongArray {
        require(unitMs > 0) { "unitMs must be positive" }
        val out = mutableListOf(0L)
        var nextGap = 0L
        for (word in text.trim().split(Regex("\\s+"))) {
            var wordHadLetters = false
            for (ch in word) {
                val code = CODES[ch.uppercaseChar()] ?: continue
                code.forEachIndexed { index, symbol ->
                    if (out.size > 1) out += if (index == 0) nextGap else unitMs
                    out += if (symbol == '.') unitMs else 3 * unitMs
                }
                nextGap = 3 * unitMs
                wordHadLetters = true
            }
            if (wordHadLetters) nextGap = 7 * unitMs
        }
        return if (out.size > 1) out.toLongArray() else LongArray(0)
    }

    /** Human-readable Morse, letters separated by spaces and words by " / ". */
    fun toMorseString(text: String): String =
        text.trim().split(Regex("\\s+"))
            .map { word -> word.mapNotNull { CODES[it.uppercaseChar()] }.joinToString(" ") }
            .filter { it.isNotEmpty() }
            .joinToString(" / ")
}
