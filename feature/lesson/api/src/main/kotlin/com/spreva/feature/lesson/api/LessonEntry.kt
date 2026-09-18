package com.spreva.feature.lesson.api

import androidx.compose.runtime.Composable

/** Entry contract for the lesson player (composable slot, see ADR-0003). */
class LessonEntry(
    val content: @Composable (
        lessonId: String,
        onFinished: () -> Unit,
    ) -> Unit,
)
