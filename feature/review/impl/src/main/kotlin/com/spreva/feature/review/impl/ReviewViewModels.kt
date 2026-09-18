package com.spreva.feature.review.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreva.core.common.AppClock
import com.spreva.domain.review.GetDueReviews
import com.spreva.domain.review.GradeReview
import com.spreva.domain.review.ReviewQueue
import com.spreva.core.model.ReviewRating
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
 * Review session: due cards in order, reveal then self-grade. Grading
 * persists through [GradeReview] (offline-first write, plan section 67).
 */
@HiltViewModel
class ReviewSessionViewModel @Inject constructor(
    getDueReviews: GetDueReviews,
    private val gradeReview: GradeReview,
    private val clock: AppClock,
) : ViewModel() {

    private val nowEpochMs: Long = clock.now().toEpochMilli()

    val queue: StateFlow<ReviewQueue> = getDueReviews(nowEpochMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewQueue(emptyList()))

    private val _revealed = MutableStateFlow(false)
    val revealed: StateFlow<Boolean> = _revealed.asStateFlow()

    private val _gradedCount = MutableStateFlow(0)
    val gradedCount: StateFlow<Int> = _gradedCount.asStateFlow()

    private var cardShownAtMs = clock.now().toEpochMilli()

    fun reveal() {
        _revealed.value = true
    }

    /** Grades the front card; the queue flow drops it once dueAt moves forward. */
    fun grade(rating: ReviewRating) {
        val card = queue.value.dueCards.firstOrNull() ?: return
        val now = clock.now()
        val durationMs = (now.toEpochMilli() - cardShownAtMs).coerceAtLeast(0)
        _revealed.value = false
        cardShownAtMs = now.toEpochMilli()
        _gradedCount.value += 1
        viewModelScope.launch {
            gradeReview(card, rating, now.toEpochMilli(), durationMs)
        }
    }
}
