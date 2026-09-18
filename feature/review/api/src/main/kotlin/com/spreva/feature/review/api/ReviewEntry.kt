package com.spreva.feature.review.api

import androidx.compose.runtime.Composable

/** Entry contract for the Practice tab (composable slot, see ADR-0003). */
class PracticeEntry(
    val content: @Composable (onStartReview: () -> Unit) -> Unit,
)

/** Entry contract for a review session. */
class ReviewEntry(
    val content: @Composable (onFinished: () -> Unit) -> Unit,
)
