package com.spreva.core.database.mapper

import com.spreva.core.database.entity.LessonProgressEntity
import com.spreva.core.database.entity.ReviewCardEntity
import com.spreva.core.model.LessonId
import com.spreva.core.model.LessonProgress
import com.spreva.core.model.LessonStatus
import com.spreva.core.model.KnowledgeItemId
import com.spreva.core.model.ReviewCard
import com.spreva.core.model.ReviewCardId
import java.time.Instant

fun LessonProgressEntity.toModel() = LessonProgress(
    lessonId = LessonId(lessonId),
    status = LessonStatus.valueOf(status),
    completedActivities = completedActivities,
    totalActivities = totalActivities,
    startedAt = startedAtEpochMs?.let(Instant::ofEpochMilli),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMs),
    completedAt = completedAtEpochMs?.let(Instant::ofEpochMilli),
)

fun com.spreva.core.model.LessonProgress.toEntity() = LessonProgressEntity(
    rowId = 0,
    lessonId = lessonId.value,
    status = status.name,
    completedActivities = completedActivities,
    totalActivities = totalActivities,
    startedAtEpochMs = startedAt?.toEpochMilli(),
    updatedAtEpochMs = updatedAt.toEpochMilli(),
    completedAtEpochMs = completedAt?.toEpochMilli(),
)

fun ReviewCardEntity.toModel() = ReviewCard(
    id = ReviewCardId(cardId),
    knowledgeItemId = KnowledgeItemId(knowledgeItemId),
    prompt = prompt,
    answer = answer,
    dueAt = Instant.ofEpochMilli(dueAtEpochMs),
    reviewCount = reviewCount,
    lapseCount = lapseCount,
    schedulerVersion = schedulerVersion,
    schedulerState = schedulerState,
    audioPath = audioPath,
)

fun ReviewCard.toEntity() = ReviewCardEntity(
    cardId = id.value,
    knowledgeItemId = knowledgeItemId.value,
    prompt = prompt,
    answer = answer,
    dueAtEpochMs = dueAt.toEpochMilli(),
    reviewCount = reviewCount,
    lapseCount = lapseCount,
    schedulerVersion = schedulerVersion,
    schedulerState = schedulerState,
    audioPath = audioPath,
)
