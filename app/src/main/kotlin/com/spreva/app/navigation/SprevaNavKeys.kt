package com.spreva.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Destination keys assembled at the app layer. Feature api modules define
 * their parameter contracts; the app maps keys to entry providers, which
 * keeps feature impls decoupled from each other (plan sections 7/37).
 */
@Serializable
data object WelcomeKey : NavKey

@Serializable
data class CourseKey(val levelId: String) : NavKey

@Serializable
data class LessonKey(val lessonId: String) : NavKey

@Serializable
data object ReviewSessionKey : NavKey


@Serializable
data object PlacementKey : NavKey
