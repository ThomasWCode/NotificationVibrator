package io.github.thomaswcode.notificationvibrator.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments")
    fun observeAll(): Flow<List<AssignmentEntity>>

    @Upsert
    suspend fun upsert(assignment: AssignmentEntity)

    @Query("DELETE FROM assignments WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM assignments WHERE patternId = :patternId")
    suspend fun deleteByPattern(patternId: String)

    @Query("SELECT COUNT(*) FROM assignments WHERE patternId = :patternId")
    suspend fun countByPattern(patternId: String): Int
}

@Dao
interface CustomPatternDao {
    @Query("SELECT * FROM custom_patterns ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CustomPatternEntity>>

    @Insert
    suspend fun insert(pattern: CustomPatternEntity): Long

    @Query("DELETE FROM custom_patterns WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
abstract class SeenAppDao {
    @Query("SELECT * FROM seen_apps ORDER BY lastNotifiedAt DESC")
    abstract fun observeAll(): Flow<List<SeenAppEntity>>

    @Query("UPDATE seen_apps SET lastNotifiedAt = :time, count = count + 1 WHERE packageName = :packageName")
    protected abstract suspend fun touch(packageName: String, time: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insert(seenApp: SeenAppEntity)

    @Transaction
    open suspend fun record(packageName: String, time: Long) {
        if (touch(packageName, time) == 0) insert(SeenAppEntity(packageName, time, 1))
    }
}

@Dao
interface EventLogDao {
    @Query("SELECT * FROM event_log ORDER BY id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<EventLogEntity>>

    @Insert
    suspend fun insert(entry: EventLogEntity)

    @Query("DELETE FROM event_log WHERE id NOT IN (SELECT id FROM event_log ORDER BY id DESC LIMIT :keep)")
    suspend fun trim(keep: Int)

    @Query("DELETE FROM event_log")
    suspend fun clear()
}
