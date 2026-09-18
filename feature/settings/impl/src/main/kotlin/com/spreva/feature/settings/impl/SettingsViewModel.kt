package com.spreva.feature.settings.impl

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreva.core.datastore.SettingsDataSource
import com.spreva.core.model.ThemeMode
import com.spreva.core.model.UiLanguage
import com.spreva.domain.curriculum.GetMediaAttributions
import com.spreva.domain.curriculum.MediaAttribution
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val uiLanguage: UiLanguage = UiLanguage.ENGLISH,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataSource: SettingsDataSource,
    private val getMediaAttributions: GetMediaAttributions,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsDataSource.settings
        .map { settings ->
            SettingsUiState(
                themeMode = settings.themeMode,
                useDynamicColor = settings.useDynamicColor,
                uiLanguage = settings.uiLanguage,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _attributions = MutableStateFlow<List<MediaAttribution>>(emptyList())

    /** Lazy: loaded when the attribution sheet opens, not at startup. */
    val attributions: StateFlow<List<MediaAttribution>> = _attributions.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataSource.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsDataSource.setDynamicColor(enabled) }
    }

    /** Applies the official per-app locales API (plan section 213). */
    fun setUiLanguage(language: UiLanguage) {
        viewModelScope.launch {
            settingsDataSource.setUiLanguage(language)
            val tag = when (language) {
                UiLanguage.ENGLISH -> "en"
                UiLanguage.ARABIC -> "ar"
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    fun loadAttributions() {
        if (_attributions.value.isNotEmpty()) return
        viewModelScope.launch {
            _attributions.value = runCatching { getMediaAttributions() }.getOrDefault(emptyList())
        }
    }
}
