package com.spreva.app

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.spreva.app.navigation.CourseKey
import com.spreva.app.navigation.LessonKey
import com.spreva.app.navigation.ReviewSessionKey
import com.spreva.app.navigation.PlacementKey
import com.spreva.app.navigation.WelcomeKey
import com.spreva.core.navigation.HomeKey
import com.spreva.core.navigation.LearnKey
import com.spreva.core.navigation.PracticeKey
import com.spreva.core.navigation.SettingsKey

/**
 * Builds the Navigation 3 entry provider. Each feature exposes a
 * composable-slot contract (class with a `content` lambda) from its api
 * module; the app binds nav keys to contracts here so feature impls never
 * depend on each other (plan sections 7/37, ADR-0003). Contracts resolve
 * from Hilt through [EntryHolderViewModel].
 */
internal fun sprevaEntryProvider(
    onOpenLearn: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenPlacement: () -> Unit,
    onOpenCourse: (String) -> Unit,
    onOpenLesson: (String) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
) = entryProvider<NavKey> {
    entry<WelcomeKey> {
        val holder = hiltViewModel<EntryHolderViewModel>()
        holder.onboardingEntry.content { onGoHome() }
    }

    entry<HomeKey> {
        val holder = hiltViewModel<EntryHolderViewModel>()
        holder.homeEntry.content(onOpenLearn, onOpenReview)
    }

    entry<LearnKey> {
        val holder = hiltViewModel<EntryHolderViewModel>()
        holder.learnEntry.content(onOpenCourse)
    }

    entry<PracticeKey> {
        val holder = hiltViewModel<EntryHolderViewModel>()
        holder.practiceEntry.content(
            onOpenReview,
            onOpenPlacement,
            onOpenLesson,
        )
    }

    entry<SettingsKey> {
        val holder = hiltViewModel<EntryHolderViewModel>()
        holder.settingsEntry.content()
    }

    entry<CourseKey> { key ->
        val holder = hiltViewModel<EntryHolderViewModel>()
        holder.courseEntry.content(key.levelId, onOpenLesson)
    }

    entry<LessonKey> { key ->
        val holder = hiltViewModel<EntryHolderViewModel>()
        holder.lessonEntry.content(key.lessonId, onBack)
    }

    entry<ReviewSessionKey> {
        val holder = hiltViewModel<EntryHolderViewModel>()
        holder.reviewEntry.content(onBack)
    }

    entry<PlacementKey> {
        val holder = hiltViewModel<EntryHolderViewModel>()
        holder.placementEntry.content(
            onOpenCourse,
            onBack,
        )
    }
}
