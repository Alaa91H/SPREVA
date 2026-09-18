package com.spreva.feature.home.api

import androidx.compose.runtime.Composable

/** Entry contract for the Home tab (composable slot, see ADR-0003). */
class HomeEntry(
    val content: @Composable (
        onOpenLearn: () -> Unit,
        onStartReview: () -> Unit,
    ) -> Unit,
)
