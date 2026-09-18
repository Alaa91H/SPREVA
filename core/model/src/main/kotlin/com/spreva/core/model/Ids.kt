package com.spreva.core.model

/**
 * Type-safe identifiers used across the Spreva domain.
 *
 * Kept as value classes so serialization stays simple while retaining
 * compile-time distinction between different ID kinds.
 */
@JvmInline
value class CourseId(val value: String)

@JvmInline
value class LevelId(val value: String)

@JvmInline
value class UnitId(val value: String)

@JvmInline
value class LessonId(val value: String)

@JvmInline
value class ActivityId(val value: String)

@JvmInline
value class CanDoId(val value: String)

@JvmInline
value class ReviewCardId(val value: String)

@JvmInline
value class KnowledgeItemId(val value: String)

@JvmInline
value class LearningEventId(val value: String)
