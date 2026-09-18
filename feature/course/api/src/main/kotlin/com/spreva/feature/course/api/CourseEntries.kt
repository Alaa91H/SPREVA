package com.spreva.feature.course.api

import androidx.compose.runtime.Composable

/** Entry contract for the Learn tab (composable slot, see ADR-0003). */
class LearnEntry(
    val content: @Composable (onOpenCourse: (levelId: String) -> Unit) -> Unit,
)

/** Entry contract for a single level course screen. */
class CourseEntry(
    val content: @Composable (
        levelId: String,
        onOpenLesson: (lessonId: String) -> Unit,
    ) -> Unit,
)
