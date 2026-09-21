package com.spreva.feature.review.api

import androidx.compose.runtime.Composable

/** Entry contract for the Practice tab (composable slot, see ADR-0003). */
class PracticeEntry(
    val content: @Composable (
        onStartReview: () -> Unit,
        onStartPlacement: () -> Unit,
        onOpenLesson: (String) -> Unit,
    ) -> Unit,
)

/** Entry contract for a review session. */
class ReviewEntry(
    val content: @Composable (onFinished: () -> Unit) -> Unit,
)


/** Entry contract for the adaptive placement diagnostic. */
class PlacementEntry(
    val content: @Composable (
        onOpenCourse: (String) -> Unit,
        onFinished: () -> Unit,
    ) -> Unit,
)
