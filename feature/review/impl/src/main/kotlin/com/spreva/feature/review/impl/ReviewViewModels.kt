package com.spreva.feature.review.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreva.core.audio.CourseAudioPlayer
import com.spreva.core.common.AppClock
import com.spreva.core.model.ReviewCard
import com.spreva.core.model.ReviewRating
import com.spreva.domain.review.GetDueReviews
import com.spreva.domain.review.GradeReview
import com.spreva.domain.review.ReviewQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Practice tab state: shows due-review summary and CTA. */
@HiltViewModel
class PracticeViewModel @Inject constructor(
    reviewRepository: com.spreva.domain.review.ReviewRepository,
    clock: AppClock,
) : ViewModel() {

    private val nowEpochMs: Long = clock.now().toEpochMilli()

    val dueCount: StateFlow<Int> = reviewRepository.observeDueCount(nowEpochMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

/**
 * Review session: due cards in order, reveal then self-grade. Audio cards
 * (Phase 4.3) play the recording first and ask the learner to type the
 * German phrase — the typed answer is normalized and self-checked before
 * the rating buttons appear (plan section 17 review modalities).
 * Grading persists through [GradeReview] (offline-first, plan section 67).
 */
@HiltViewModel
class ReviewSessionViewModel @Inject constructor(
    getDueReviews: GetDueReviews,
    private val gradeReview: GradeReview,
    private val clock: AppClock,
    private val courseAudioPlayer: CourseAudioPlayer,
) : ViewModel() {

    private val nowEpochMs: Long = clock.now().toEpochMilli()

    val queue: StateFlow<ReviewQueue> = getDueReviews(nowEpochMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewQueue(emptyList()))

    private val _revealed = MutableStateFlow(false)
    val revealed: StateFlow<Boolean> = _revealed.asStateFlow()

    private val _typedAnswer = MutableStateFlow("")
    val typedAnswer: StateFlow<String> = _typedAnswer.asStateFlow()

    private val _audioCheck = MutableStateFlow<Boolean?>(null)
    /** null = not checked yet; true/false = typed answer was correct/incorrect. */
    val audioCheck: StateFlow<Boolean?> = _audioCheck.asStateFlow()

    private val _gradedCount = MutableStateFlow(0)
    val gradedCount: StateFlow<Int> = _gradedCount.asStateFlow()

    private var cardShownAtMs = clock.now().toEpochMilli()

    fun onAnswerChanged(answer: String) {
        _typedAnswer.value = answer
    }

    /** Plays the audio-card recording through the course audio player. */
    fun playCardAudio(card: ReviewCard) {
        card.audioPath?.let { courseAudioPlayer.play(it) }
    }

    /** Normalizes and checks the typed answer of an audio card. */
    fun checkAudioAnswer(card: ReviewCard): Boolean {
        val normalizedTyped = normalize(_typedAnswer.value)
        val normalizedExpected = normalize(card.answer)
        val correct = normalizedTyped == normalizedExpected
        _audioCheck.value = correct
        return correct
    }

    fun resetAudioState() {
        _typedAnswer.value = ""
        _audioCheck.value = null
        _revealed.value = false
    }

    private fun normalize(answer: String): String = answer
        .trim()
        .lowercase()
        .trimEnd('.', '!', '?', ',', ':')
        .replace(Regex("\\s+"), " ")

    fun reveal() {
        _revealed.value = true
    }

    /** Grades the front card; the queue flow drops it once dueAt moves forward. */
    fun grade(rating: ReviewRating) {
        val card = queue.value.dueCards.firstOrNull() ?: return
        val now = clock.now()
        val durationMs = (now.toEpochMilli() - cardShownAtMs).coerceAtLeast(0)
        _revealed.value = false
        _typedAnswer.value = ""
        _audioCheck.value = null
        cardShownAtMs = now.toEpochMilli()
        _gradedCount.value += 1
        viewModelScope.launch {
            gradeReview(card, rating, now.toEpochMilli(), durationMs)
        }
    }

    override fun onCleared() {
        courseAudioPlayer.stop()
        super.onCleared()
    }
}
