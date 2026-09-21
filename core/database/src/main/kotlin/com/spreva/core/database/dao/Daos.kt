package com.spreva.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.spreva.core.database.entity.ActivityAttemptEntity
import com.spreva.core.database.entity.LearningEventEntity
import com.spreva.core.database.entity.LessonProgressEntity
import com.spreva.core.database.entity.ReviewCardEntity
import com.spreva.core.database.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonProgressDao {

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId LIMIT 1")
    fun observeByLesson(lessonId: String): Flow<LessonProgressEntity?>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getByLesson(lessonId: String): LessonProgressEntity?

    @Query("SELECT * FROM lesson_progress ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE status = 'COMPLETED'")
    suspend fun getCompleted(): List<LessonProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LessonProgressEntity)

    @Query("DELETE FROM lesson_progress")
    suspend fun clear()
}

@Dao
interface ActivityAttemptDao {

    @Insert
    suspend fun insert(attempt: ActivityAttemptEntity)

    @Query("SELECT COUNT(*) FROM activity_attempts WHERE lessonId = :lessonId")
    suspend fun countForLesson(lessonId: String): Int

    @Query("SELECT * FROM activity_attempts ORDER BY createdAtEpochMs ASC")
    fun observeAll(): Flow<List<ActivityAttemptEntity>>

    @Query("SELECT * FROM activity_attempts ORDER BY createdAtEpochMs ASC")
    suspend fun getAll(): List<ActivityAttemptEntity>

    @Query("DELETE FROM activity_attempts")
    suspend fun clear()
}

@Dao
interface LearningEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: LearningEventEntity)

    @Query("SELECT COUNT(*) FROM learning_events WHERE eventId = :eventId")
    suspend fun exists(eventId: String): Int

    @Query("DELETE FROM learning_events")
    suspend fun clear()
}

@Dao
interface ReviewCardDao {

    @Query("SELECT * FROM review_cards WHERE dueAtEpochMs <= :nowEpochMs ORDER BY dueAtEpochMs ASC")
    fun observeDue(nowEpochMs: Long): Flow<List<ReviewCardEntity>>

    @Query("SELECT * FROM review_cards WHERE dueAtEpochMs <= :nowEpochMs ORDER BY dueAtEpochMs ASC")
    suspend fun getDue(nowEpochMs: Long): List<ReviewCardEntity>

    @Query("SELECT COUNT(*) FROM review_cards WHERE dueAtEpochMs <= :nowEpochMs")
    fun observeDueCount(nowEpochMs: Long): Flow<Int>

    @Query("SELECT * FROM review_cards WHERE cardId = :cardId LIMIT 1")
    suspend fun getById(cardId: String): ReviewCardEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<ReviewCardEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM review_cards WHERE knowledgeItemId IN (:knowledgeItemIds)")
    suspend fun countForKnowledgeItems(knowledgeItemIds: List<String>): Int

    @Update
    suspend fun update(card: ReviewCardEntity)

    @Query("DELETE FROM review_cards")
    suspend fun clear()
}

@Dao
interface ReviewLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: ReviewLogEntity)

    @Query("DELETE FROM review_logs")
    suspend fun clear()
}
