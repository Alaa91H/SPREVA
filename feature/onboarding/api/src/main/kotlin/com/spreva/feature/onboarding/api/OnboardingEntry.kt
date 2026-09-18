package com.spreva.feature.onboarding.api

import androidx.compose.runtime.Composable

/**
 * Entry contract for the onboarding flow. Function-typed property (not a
 * fun interface with a @Composable member): composable interface members
 * compile but crash at runtime with NoSuchMethodError, while @Composable
 * function types are the officially supported dispatch (ADR-0003).
 */
class OnboardingEntry(
    val content: @Composable (onFinished: () -> Unit) -> Unit,
)
