package com.spreva.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.spreva.feature.course.api.CourseEntry
import com.spreva.feature.course.api.LearnEntry
import com.spreva.feature.home.api.HomeEntry
import com.spreva.feature.lesson.api.LessonEntry
import com.spreva.feature.onboarding.api.OnboardingEntry
import com.spreva.feature.review.api.PracticeEntry
import com.spreva.feature.review.api.ReviewEntry
import com.spreva.feature.settings.api.SettingsEntry
import javax.inject.Inject

/**
 * Application-layer holder that exposes the Hilt-bound feature entry
 * contracts to the Navigation 3 entries. Feature impls stay decoupled from
 * each other; the app assembles them (plan sections 7/37).
 */
@HiltViewModel
class EntryHolderViewModel @Inject constructor(
    val onboardingEntry: OnboardingEntry,
    val homeEntry: HomeEntry,
    val learnEntry: LearnEntry,
    val courseEntry: CourseEntry,
    val lessonEntry: LessonEntry,
    val practiceEntry: PracticeEntry,
    val reviewEntry: ReviewEntry,
    val settingsEntry: SettingsEntry,
) : ViewModel()
