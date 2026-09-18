package com.spreva.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.sqlite.execSQL
import com.spreva.core.database.dao.ActivityAttemptDao
import com.spreva.core.database.dao.LearningEventDao
import com.spreva.core.database.dao.LessonProgressDao
import com.spreva.core.database.dao.ReviewCardDao
import com.spreva.core.database.dao.ReviewLogDao
import com.spreva.core.database.entity.ActivityAttemptEntity
import com.spreva.core.database.entity.LearningEventEntity
import com.spreva.core.database.entity.LessonProgressEntity
import com.spreva.core.database.entity.ReviewCardEntity
import com.spreva.core.database.entity.ReviewLogEntity

/**
 * Spreva local database (SSOT for progress and review state).
 * Version 1 — schema JSON is exported to `schemas/` and committed to Git.
 */
@Database(
    version = 2,
    exportSchema = true,
    entities = [
        LessonProgressEntity::class,
        ActivityAttemptEntity::class,
        LearningEventEntity::class,
        ReviewCardEntity::class,
        ReviewLogEntity::class,
    ],
)
abstract class SprevaDatabase : RoomDatabase() {
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun activityAttemptDao(): ActivityAttemptDao
    abstract fun learningEventDao(): LearningEventDao
    abstract fun reviewCardDao(): ReviewCardDao
    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        const val NAME = "spreva.db"

        /** v1→v2: review_cards gains the audio-recall column (Phase 4.3). */
        private val MIGRATION_1_2 = object : androidx.room3.migration.Migration(1, 2) {
            override suspend fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                connection.execSQL("ALTER TABLE review_cards ADD COLUMN audioPath TEXT")
            }
        }

        val MIGRATIONS: Array<androidx.room3.migration.Migration> = arrayOf(MIGRATION_1_2)
    }
}
