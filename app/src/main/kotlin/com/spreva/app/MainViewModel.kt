package com.spreva.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreva.core.model.UserSettings
import com.spreva.core.datastore.SettingsDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes user settings to the app shell. Locale changes go through the
 * official per-app locales API (plan section 213).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsDataSource: SettingsDataSource,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsDataSource.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserSettings(),
        )

    fun applyUiLanguage(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
