package com.spreva.core.model

/** Productive skill evaluated through an explicit learner rubric. */
enum class ProductionMode {
    WRITING,
    SPEAKING,
}

/**
 * Four-point self-review scale. It is intentionally criterion-referenced
 * and never presented as an official CEFR score.
 */
enum class RubricRating(val points: Int) {
    NEEDS_REVISION(0),
    EMERGING(1),
    MOSTLY_EFFECTIVE(2),
    CONSISTENT(3),
}

data class ProductionRubricCriterion(
    val id: String,
    val title: LocalizedText,
    val guidance: LocalizedText,
)

data class ProductionRubric(
    val level: CefrLevel,
    val mode: ProductionMode,
    val criteria: List<ProductionRubricCriterion>,
)

data class ProductionSelfAssessment(
    val lessonId: LessonId,
    val activityId: ActivityId,
    val level: CefrLevel,
    val mode: ProductionMode,
    val ratings: Map<String, RubricRating>,
    /** 0..100, derived only from the learner's criterion ratings. */
    val selfScorePercent: Int,
    /** Metadata only; learner text/audio is not persisted in this event. */
    val wordCount: Int? = null,
    val durationMs: Long? = null,
)
