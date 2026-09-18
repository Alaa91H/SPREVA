package com.spreva.core.model

/**
 * Stable, language-independent learner goal selected during onboarding
 * (audit §6: persist the goal as a domain type, never a free string and
 * never a localized label). UI labels resolve via string resources; this
 * enum is the persistence + recommendation key.
 */
enum class LearningGoal {
    /** Full structured path from zero to C1. */
    ZERO_TO_C1,

    /** Everyday life in Germany focus. */
    DAILY_LIFE,

    /** Conversation-first focus. */
    CONVERSATION,

    /** Professional/work German focus. */
    WORK,

    /** Exam preparation focus (Goethe/telc/DTZ). */
    EXAM,
}
