package com.spreva.feature.lesson.impl

import com.spreva.core.audio.TtsStatus
import com.spreva.core.model.ActivityId
import com.spreva.core.model.Lesson

/** Grading result of the current activity attempt. */
data class AnswerState(
    val correct: Boolean,
    /** Shown when incorrect, e.g. the expected cloze answer. */
    val solution: String? = null,
)

/** Lesson player UiState (small, immutable, plan section 15). */
data class LessonUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val lesson: Lesson? = null,
    val currentIndex: Int = 0,
    val selectedOptionId: String? = null,
    val typedAnswer: String = "",
    val answerState: AnswerState? = null,
    val completedCount: Int = 0,
    val finished: Boolean = false,
    /** German voice availability — gates speaker buttons (Phase 4.1). */
    val ttsStatus: TtsStatus = TtsStatus.NOT_READY,
    /** Phase 4.3 shadowing: recording in progress. */
    val isRecording: Boolean = false,
    /** True once the learner has recorded themselves this activity. */
    val hasRecording: Boolean = false,
) {
    /** Whether the Check button should be enabled for the current activity. */
    val canCheck: Boolean
        get() {
            val activity = lesson?.activities?.getOrNull(currentIndex) ?: return false
            return when (activity) {
                is com.spreva.core.model.LearningActivity.MultipleChoice -> selectedOptionId != null
                is com.spreva.core.model.LearningActivity.ListeningChoice -> selectedOptionId != null
                is com.spreva.core.model.LearningActivity.Cloze -> typedAnswer.isNotBlank()
                else -> true
            }
        }
}

/** Graded outcome used for persistence. */
internal data class ActivityOutcome(
    val activityId: ActivityId,
    val attemptNumber: Int,
    val correct: Boolean,
    val usedHint: Boolean,
    val responseTimeMs: Long,
)
