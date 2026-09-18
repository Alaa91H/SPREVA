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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * App bootstrap state (audit §5): the shell must not build its start
 * destination from a default [UserSettings] while DataStore is still
 * loading — that is the race that sent returning users back to onboarding.
 */
sealed interface AppBootstrapState {
    data object Loading : AppBootstrapState
    data class Ready(val settings: UserSettings) : AppBootstrapState
}

/**
 * Exposes user settings to the app shell. Locale changes go through the
 * official per-app locales API (plan section 213).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsDataSource: SettingsDataSource,
) : ViewModel() {

    /** First real settings value gates the shell (null while loading). */
    val bootstrap: StateFlow<AppBootstrapState> = settingsDataSource.settings
        .map { AppBootstrapState.Ready(it) as AppBootstrapState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppBootstrapState.Loading,
        )

    fun applyUiLanguage(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
