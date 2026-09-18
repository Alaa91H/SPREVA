package com.spreva.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreva.core.common.AppClock
import com.spreva.domain.curriculum.GetNextLesson
import com.spreva.domain.learning.LearningRepository
import com.spreva.domain.review.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Home UiState (plan section 15: small, immutable). */
data class HomeUiState(
    val nextLessonId: String? = null,
    val dueReviews: Int = 0,
    val completedLessons: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getNextLesson: GetNextLesson,
    reviewRepository: ReviewRepository,
    private val learningRepository: LearningRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Reacts to progress changes so completed-lessons count stays live. */
    val allProgress = learningRepository.observeAllProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val lesson = getNextLesson()
            _uiState.value = _uiState.value.copy(
                nextLessonId = lesson?.id?.value,
                isLoading = false,
            )
        }
        viewModelScope.launch {
            reviewRepository.observeDueCount(clock.now().toEpochMilli()).collect { due ->
                _uiState.value = _uiState.value.copy(dueReviews = due)
            }
        }
    }
}
