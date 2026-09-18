package com.spreva.feature.lesson.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreva.core.audio.SpeechText
import com.spreva.core.audio.TtsStatus
import com.spreva.core.audio.TtsProvider
import com.spreva.core.common.AppClock
import com.spreva.core.model.ActivityAttempt
import com.spreva.core.model.Lesson
import com.spreva.core.model.LessonId
import com.spreva.domain.curriculum.GetLesson
import com.spreva.domain.learning.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lesson player ViewModel: loads content, grades answers, records attempts
 * and completes the lesson (progress persists across restarts — plan
 * sections 9/243-247). Grading of cloze answers is case-insensitive and
 * trims whitespace/punctuation.
 *
 * Phase 4.1: owns TTS playback for German content (plan sections 31-34).
 * The provider is injected behind an interface; features never touch the
 * engine directly.
 */
@HiltViewModel
class LessonViewModel @Inject constructor(
    private val getLesson: GetLesson,
    private val learningRepository: LearningRepository,
    private val clock: AppClock,
    private val ttsProvider: TtsProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    val ttsStatus: StateFlow<TtsStatus> = ttsProvider.status

    private var attemptCounter = mutableMapOf<String, Int>()
    private var currentActivityShownAtMs = 0L

    init {
        // Expose German voice availability to the lesson UI.
        viewModelScope.launch {
            ttsProvider.status.collect { availability ->
                _uiState.value = _uiState.value.copy(ttsStatus = availability)
            }
        }
    }

    override fun onCleared() {
        ttsProvider.stop()
        super.onCleared()
    }

    /** Plays the German prompt of the current activity via TTS. */
    fun speakCurrent() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        val activity = lesson.activities.getOrNull(state.currentIndex) ?: return
        when (activity) {
            is com.spreva.core.model.LearningActivity.TextIntro ->
                ttsProvider.speakGerman(article = null, text = activity.title.de)

            is com.spreva.core.model.LearningActivity.VocabularyIntro ->
                ttsProvider.speakGerman(article = null, text = SpeechText.clean(activity.words.joinToString(" ") { it.german }))

            is com.spreva.core.model.LearningActivity.MultipleChoice ->
                ttsProvider.speakGerman(article = null, text = activity.question.de)

            is com.spreva.core.model.LearningActivity.Cloze ->
                ttsProvider.speakGerman(
                    article = null,
                    text = SpeechText.forSentence(
                        template = activity.sentenceTemplate.de,
                        answerPlaceholder = activity.answerPlaceholder,
                        answer = null,
                    ),
                )

            is com.spreva.core.model.LearningActivity.LessonSummaryActivity -> Unit
        }
    }

    /** Plays a single vocabulary word (article + noun) from the vocab card. */
    fun speakWord(article: String?, german: String) {
        ttsProvider.speakGerman(article = article, text = german)
    }

    fun load(lessonId: String) {
        viewModelScope.launch {
            _uiState.value = LessonUiState(isLoading = true)
            try {
                val lesson = getLesson(LessonId(lessonId))
                if (lesson == null) {
                    _uiState.value = LessonUiState(isLoading = false, error = "Lesson not found")
                    return@launch
                }
                learningRepository.ensureStarted(lesson.id, lesson.activities.size)
                attemptCounter.clear()
                currentActivityShownAtMs = clock.now().toEpochMilli()
                _uiState.value = LessonUiState(isLoading = false, lesson = lesson)
            } catch (t: Throwable) {
                _uiState.value = LessonUiState(isLoading = false, error = t.message ?: "Load failed")
            }
        }
    }

    fun onOptionSelected(optionId: String) {
        _uiState.value = _uiState.value.copy(selectedOptionId = optionId)
    }

    fun onAnswerChanged(answer: String) {
        _uiState.value = _uiState.value.copy(typedAnswer = answer)
    }

    /** Grades the current activity and records the attempt. */
    fun check() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        val activity = lesson.activities.getOrNull(state.currentIndex) ?: return

        val answerState = when (activity) {
            is com.spreva.core.model.LearningActivity.MultipleChoice -> {
                val selected = state.selectedOptionId
                AnswerState(
                    correct = selected == activity.correctOptionId,
                    solution = activity.options
                        .firstOrNull { it.id == activity.correctOptionId }
                        ?.text?.de,
                )
            }

            is com.spreva.core.model.LearningActivity.Cloze -> {
                val normalized = normalize(state.typedAnswer)
                AnswerState(
                    correct = activity.acceptedAnswers.any { normalize(it) == normalized },
                    solution = activity.acceptedAnswers.firstOrNull(),
                )
            }

            else -> AnswerState(correct = true)
        }

        recordAttempt(
            lesson = lesson,
            activityId = activity.id,
            correct = answerState.correct,
        )

        _uiState.value = state.copy(
            answerState = answerState,
            completedCount = if (answerState.correct) state.completedCount + 1 else state.completedCount,
        )
    }

    /** Moves to the next activity (or stays on the summary). */
    fun next() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        val nextIndex = (state.currentIndex + 1).coerceAtMost(lesson.activities.lastIndex)
        currentActivityShownAtMs = clock.now().toEpochMilli()
        _uiState.value = state.copy(
            currentIndex = nextIndex,
            selectedOptionId = null,
            typedAnswer = "",
            answerState = null,
        )
    }

    /** Marks the lesson complete: progress + event + review cards. */
    fun finish() {
        val lesson = _uiState.value.lesson ?: return
        viewModelScope.launch {
            learningRepository.completeLesson(lesson.id, lesson.activities.size)
        }
    }

    private fun recordAttempt(lesson: Lesson, activityId: com.spreva.core.model.ActivityId, correct: Boolean) {
        val attemptNumber = (attemptCounter[activityId.value] ?: 0) + 1
        attemptCounter[activityId.value] = attemptNumber
        val now = clock.now()
        val attempt = ActivityAttempt(
            id = 0,
            lessonId = lesson.id,
            activityId = activityId,
            attemptNumber = attemptNumber,
            correct = correct,
            usedHint = false,
            responseTimeMs = (now.toEpochMilli() - currentActivityShownAtMs).coerceAtLeast(0),
            createdAt = now,
        )
        viewModelScope.launch {
            learningRepository.recordAttempt(attempt)
        }
    }

    private fun normalize(answer: String): String = answer
        .trim()
        .lowercase()
        .trimEnd('.', '!', '?', ',')
}
