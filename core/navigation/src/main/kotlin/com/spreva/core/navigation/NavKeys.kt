package com.spreva.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Top-level navigation keys shared across feature api modules.
 * Feature-specific keys (LessonKey, ...) live in their own feature api.
 */
@Serializable
data object OnboardingKey : NavKey

@Serializable
data object HomeKey : NavKey

@Serializable
data object LearnKey : NavKey

@Serializable
data object PracticeKey : NavKey

@Serializable
data object ProgressKey : NavKey

@Serializable
data object SettingsKey : NavKey
