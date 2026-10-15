package io.github.thomaswcode.notificationvibrator.data

import io.github.thomaswcode.notificationvibrator.vibration.PatternLibrary
import io.github.thomaswcode.notificationvibrator.vibration.VibrationPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AssignmentRepository(private val dao: AssignmentDao) {
    val assignments: Flow<List<AssignmentEntity>> = dao.observeAll()

    suspend fun assign(packageName: String, patternId: String, intensity: Float) =
        dao.upsert(AssignmentEntity(packageName, patternId, intensity))

    /** Removes the assignment so the app falls back to the default pattern. */
    suspend fun reset(packageName: String) = dao.delete(packageName)
}

class PatternRepository(
    private val customDao: CustomPatternDao,
    private val assignmentDao: AssignmentDao,
    private val settingsStore: SettingsStore,
) {
    val customPatterns: Flow<List<VibrationPattern>> =
        customDao.observeAll().map { rows -> rows.map(CustomPatternEntity::toPattern) }

    /** Built-in library followed by custom patterns. */
    val patterns: Flow<List<VibrationPattern>> = customPatterns.map { PatternLibrary.all + it }

    suspend fun saveCustom(name: String, timings: LongArray, amplitudes: IntArray?, source: CustomPatternSource): Long =
        customDao.insert(
            CustomPatternEntity(
                name = name.trim(),
                timings = PatternCodec.encode(timings),
                amplitudes = amplitudes?.let(PatternCodec::encode),
                source = source.name,
                createdAt = System.currentTimeMillis(),
            ),
        )

    /** Number of apps using the pattern, plus one if it is the default pattern. */
    suspend fun usageCount(pattern: VibrationPattern): Int {
        val isDefault = settingsStore.settings.first().defaultPatternId == pattern.id
        return assignmentDao.countByPattern(pattern.id) + if (isDefault) 1 else 0
    }

    /** Deletes a custom pattern; apps using it fall back to the default, and a deleted default resets. */
    suspend fun deleteCustom(pattern: VibrationPattern) {
        require(pattern.isCustom) { "Only custom patterns can be deleted" }
        assignmentDao.deleteByPattern(pattern.id)
        val settings = settingsStore.settings.first()
        if (settings.defaultPatternId == pattern.id) {
            settingsStore.setDefaultPattern(PatternLibrary.DEFAULT_PATTERN_ID, settings.defaultIntensity)
        }
        customDao.delete(pattern.id.removePrefix(VibrationPattern.CUSTOM_PREFIX).toLong())
    }
}

class ActivityRepository(
    private val seenAppDao: SeenAppDao,
    private val eventLogDao: EventLogDao,
) {
    val seenApps: Flow<List<SeenAppEntity>> = seenAppDao.observeAll()

    val recentEvents: Flow<List<EventLogEntity>> = eventLogDao.observeRecent(MAX_LOG_ENTRIES)

    suspend fun recordSeen(packageName: String, time: Long) = seenAppDao.record(packageName, time)

    suspend fun log(entry: EventLogEntity) {
        eventLogDao.insert(entry)
        eventLogDao.trim(MAX_LOG_ENTRIES)
    }

    suspend fun clearLog() = eventLogDao.clear()

    companion object {
        const val MAX_LOG_ENTRIES = 200
    }
}
