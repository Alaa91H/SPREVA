package com.spreva.feature.course.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreva.core.datastore.SettingsDataSource
import com.spreva.core.model.Course
import com.spreva.core.model.LessonProgress
import com.spreva.core.model.LessonStatus
import com.spreva.domain.curriculum.ObserveCourse
import com.spreva.domain.learning.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Learn tab: the CEFR path overview (plan section 47). */
@HiltViewModel
class LearnViewModel @Inject constructor(
    observeCourse: ObserveCourse,
    learningRepository: LearningRepository,
    settingsDataSource: SettingsDataSource,
) : ViewModel() {

    val courseState: StateFlow<Course?> = observeCourse()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiLanguage = settingsDataSource.settings
        .map { it.uiLanguage }
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.spreva.core.model.UiLanguage.ENGLISH)

    /** Progress per lesson id, as a live map for per-row completion state. */
    val progressByLesson: StateFlow<Map<String, LessonProgress>> =
        learningRepository.observeAllProgress()
            .map { list -> list.associateBy { it.lessonId.value } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

/** Level course screen state holder. */
@HiltViewModel
class CourseViewModel @Inject constructor(
    observeCourse: ObserveCourse,
    learningRepository: LearningRepository,
    settingsDataSource: SettingsDataSource,
) : ViewModel() {

    private val _levelId = MutableStateFlow("")
    val levelId: StateFlow<String> = _levelId.asStateFlow()

    val courseState: StateFlow<Course?> = observeCourse()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiLanguage = settingsDataSource.settings
        .map { it.uiLanguage }
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.spreva.core.model.UiLanguage.ENGLISH)

    val progressByLesson: StateFlow<Map<String, LessonProgress>> =
        learningRepository.observeAllProgress()
            .map { list -> list.associateBy { it.lessonId.value } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setLevel(id: String) {
        _levelId.value = id
    }
}
