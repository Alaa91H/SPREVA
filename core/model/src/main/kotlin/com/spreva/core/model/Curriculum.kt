package com.spreva.core.model

/**
 * Curriculum hierarchy models. Content comes from the content layer
 * (bundled JSON now, downloadable packages later) — never hardcoded in UI.
 */
data class Course(
    val id: CourseId,
    val title: LocalizedText,
    val levels: List<Level>,
)

data class Level(
    val id: LevelId,
    val cefr: CefrLevel,
    val title: LocalizedText,
    val units: List<Unit>,
)

data class Unit(
    val id: UnitId,
    val title: LocalizedText,
    val lessons: List<LessonSummary>,
)

data class LessonSummary(
    val id: LessonId,
    val title: LocalizedText,
    val activityCount: Int,
)

data class Lesson(
    val id: LessonId,
    val unitId: UnitId,
    val title: LocalizedText,
    val canDo: List<CanDoId>,
    val activities: List<LearningActivity>,
)
