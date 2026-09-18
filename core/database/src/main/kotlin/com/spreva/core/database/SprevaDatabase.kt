package com.spreva.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
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
    version = 1,
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
    }
}
