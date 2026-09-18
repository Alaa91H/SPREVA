package com.spreva.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreva.core.model.ThemeMode
import com.spreva.core.model.UiLanguage

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.spreva_settings_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        // Theme row.
        Text(stringResource(R.string.spreva_settings_theme), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ThemeMode.SYSTEM to stringResource(R.string.spreva_settings_theme_system),
                ThemeMode.LIGHT to stringResource(R.string.spreva_settings_theme_light),
                ThemeMode.DARK to stringResource(R.string.spreva_settings_theme_dark),
            ).forEach { (mode, label) ->
                OutlinedButton(onClick = { viewModel.setThemeMode(mode) }) {
                    Text(
                        if (state.themeMode == mode) "● $label" else label,
                    )
                }
            }
        }

        HorizontalDivider()

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.spreva_settings_dynamic_color))
            Switch(
                checked = state.useDynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )
        }

        HorizontalDivider()

        Text(stringResource(R.string.spreva_settings_language), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.setUiLanguage(UiLanguage.ENGLISH) }) {
                Text(if (state.uiLanguage == UiLanguage.ENGLISH) "● English" else "English")
            }
            OutlinedButton(onClick = { viewModel.setUiLanguage(UiLanguage.ARABIC) }) {
                Text(if (state.uiLanguage == UiLanguage.ARABIC) "● العربية" else "العربية")
            }
        }
    }
}
