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

/**
 * Lesson completion persistence state machine (audit §8): navigation must
 * wait for [LessonCompletionState.Success] so the ViewModel scope cannot
 * cancel the write mid-flight when the destination leaves the back stack.
 */
enum class LessonCompletionState {
    Idle,
    Saving,
    Success,
    Error,
}

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
    /** Completion persistence lifecycle (audit §8). */
    val completionState: LessonCompletionState = LessonCompletionState.Idle,
    /** German voice availability — gates speaker buttons (Phase 4.1). */
    val ttsStatus: TtsStatus = TtsStatus.NOT_READY,
    /** Phase 4.3 shadowing: recording in progress. */
    val isRecording: Boolean = false,
    /** True once the learner has recorded themselves this activity. */
    val hasRecording: Boolean = false,
    /**
     * Phase 4.3 automatic waveform similarity (0f..1f) between the learner's
     * take and the native model. Null = no automatic score available
     * (decoding failed); UI then keeps compare-by-ear guidance only.
     */
    val similarityScore: Float? = null,
    /** Audit §11: non-null when MediaRecorder failed to start (no crash). */
    val recordingError: String? = null,
    /** Duration of the latest free-speaking/shadowing take. */
    val recordingDurationMs: Long? = null,
) {
    /** Whether the Check button should be enabled for the current activity. */
    val canCheck: Boolean
        get() {
            val activity = lesson?.activities?.getOrNull(currentIndex) ?: return false
            return when (activity) {
                is com.spreva.core.model.LearningActivity.MultipleChoice -> selectedOptionId != null
                is com.spreva.core.model.LearningActivity.ListeningChoice -> selectedOptionId != null
                is com.spreva.core.model.LearningActivity.Cloze -> typedAnswer.isNotBlank()
                is com.spreva.core.model.LearningActivity.Dictation -> typedAnswer.isNotBlank()
                is com.spreva.core.model.LearningActivity.FreeWrite ->
                    typedAnswer.trim().split(Regex("\\s+")).count { it.isNotBlank() } >= activity.minWords
                is com.spreva.core.model.LearningActivity.SpeakingPrompt -> hasRecording
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
