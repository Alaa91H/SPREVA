package com.spreva.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreva.core.model.ThemeMode
import com.spreva.core.model.UiLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val attributions by viewModel.attributions.collectAsStateWithLifecycle()
    var showAttributions by remember { mutableStateOf(false) }

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

        HorizontalDivider()

        // About / attribution (plan sections 99-100, 253): CC BY-SA requires
        // in-app attribution — served dynamically from the content registry.
        Text(stringResource(R.string.spreva_settings_about), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = {
            viewModel.loadAttributions()
            showAttributions = true
        }) {
            Text(stringResource(R.string.spreva_settings_media_attributions))
        }
    }

    if (showAttributions) {
        ModalBottomSheet(onDismissRequest = { showAttributions = false }) {
            MediaAttributionsSheet(
                attributions = attributions,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun MediaAttributionsSheet(
    attributions: List<com.spreva.domain.curriculum.MediaAttribution>,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items = attributions, key = { it.path }) { record ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = record.path.substringAfterLast('/'),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = record.attribution,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = record.license,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                androidx.compose.material3.TextButton(
                    onClick = { uriHandler.openUri(record.sourceUrl) },
                ) {
                    Text(stringResource(R.string.spreva_settings_view_source))
                }
            }
            HorizontalDivider()
        }
        item {
            Text(
                text = stringResource(R.string.spreva_settings_attribution_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}
