package io.github.thomaswcode.notificationvibrator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.thomaswcode.notificationvibrator.vibration.PatternCategory
import io.github.thomaswcode.notificationvibrator.vibration.VibrationPattern

/** An app's chosen pattern. Apps without a row use the default pattern from [AppSettings]. */
@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val packageName: String,
    val patternId: String,
    val intensity: Float = 1f,
)

enum class CustomPatternSource { TAP, MORSE }

@Entity(tableName = "custom_patterns")
data class CustomPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timings: String,
    val amplitudes: String?,
    val source: String,
    val createdAt: Long,
) {
    fun toPattern(): VibrationPattern = VibrationPattern(
        id = VibrationPattern.CUSTOM_PREFIX + id,
        name = name,
        category = PatternCategory.CUSTOM,
        timings = PatternCodec.decodeTimings(timings),
        amplitudes = amplitudes?.let(PatternCodec::decodeAmplitudes),
    )
}

/** Apps the listener has seen post an alerting notification; feeds "Recently notified". */
@Entity(tableName = "seen_apps")
data class SeenAppEntity(
    @PrimaryKey val packageName: String,
    val lastNotifiedAt: Long,
    val count: Int,
)

@Entity(tableName = "event_log")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val packageName: String,
    val played: Boolean,
    val patternName: String?,
    val reason: String,
)

/** Stores waveforms as comma-separated text. */
object PatternCodec {
    fun encode(values: LongArray): String = values.joinToString(",")

    fun encode(values: IntArray): String = values.joinToString(",")

    fun decodeTimings(text: String): LongArray =
        if (text.isBlank()) LongArray(0) else text.split(',').map { it.trim().toLong() }.toLongArray()

    fun decodeAmplitudes(text: String): IntArray =
        if (text.isBlank()) IntArray(0) else text.split(',').map { it.trim().toInt() }.toIntArray()
}
