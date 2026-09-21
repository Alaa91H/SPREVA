package com.spreva.core.model

/** High-level language dimensions used by adaptive practice analytics. */
enum class SkillArea {
    GRAMMAR,
    READING,
    LISTENING,
    WRITING,
    SPEAKING,
    PRONUNCIATION,
}

enum class MistakePattern {
    REPEATED_ERROR,
    REPAIRED_AFTER_ERROR,
    RUSHED_GUESS,
    SLOW_RECALL,
    PRODUCTIVE_INCOMPLETE,
}

data class SkillMastery(
    val skill: SkillArea,
    /** Objective correctness score. Null for skills with completion-only evidence. */
    val scorePercent: Int?,
    val confidencePercent: Int,
    val objectiveCorrect: Int,
    val objectiveAttempts: Int,
    val productiveCompleted: Int,
    val productiveAttempts: Int,
)

data class TopicMastery(
    val lessonId: LessonId,
    val lessonTitle: LocalizedText,
    val skill: SkillArea,
    val scorePercent: Int,
    val confidencePercent: Int,
    val objectiveCorrect: Int,
    val objectiveAttempts: Int,
)

data class MistakeInsight(
    val lessonId: LessonId,
    val activityId: ActivityId,
    val skill: SkillArea,
    val pattern: MistakePattern,
    val severityPercent: Int,
    val incorrectAttempts: Int,
    val totalAttempts: Int,
)

data class FocusRecommendation(
    val lessonId: LessonId,
    val lessonTitle: LocalizedText,
    val skill: SkillArea,
    val priorityPercent: Int,
    val reason: MistakePattern,
)

data class LearningProfile(
    val mastery: List<SkillMastery> = emptyList(),
    val topics: List<TopicMastery> = emptyList(),
    val mistakes: List<MistakeInsight> = emptyList(),
    val focus: List<FocusRecommendation> = emptyList(),
    val totalAttempts: Int = 0,
)
