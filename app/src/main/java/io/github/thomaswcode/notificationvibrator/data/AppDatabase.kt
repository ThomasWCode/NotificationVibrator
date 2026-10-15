package io.github.thomaswcode.notificationvibrator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AssignmentEntity::class, CustomPatternEntity::class, SeenAppEntity::class, EventLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assignmentDao(): AssignmentDao
    abstract fun customPatternDao(): CustomPatternDao
    abstract fun seenAppDao(): SeenAppDao
    abstract fun eventLogDao(): EventLogDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "notification_vibrator.db")
                .build()
    }
}
