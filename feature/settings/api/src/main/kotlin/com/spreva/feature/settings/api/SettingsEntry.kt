package com.spreva.feature.settings.api

import androidx.compose.runtime.Composable

/** Entry contract for the Settings tab (composable slot, see ADR-0003). */
class SettingsEntry(
    val content: @Composable () -> Unit,
)
