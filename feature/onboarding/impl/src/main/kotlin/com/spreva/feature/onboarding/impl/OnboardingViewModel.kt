package com.spreva.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spreva.core.datastore.SettingsDataSource
import com.spreva.core.model.LearningGoal
import com.spreva.core.model.ThemeMode
import com.spreva.core.model.UiLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, LANGUAGE, GOAL }

/**
 * Minimal onboarding state. Language and theme are applied immediately so
 * the rest of the app reflects the choice from the first frame.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsDataSource: SettingsDataSource,
) : ViewModel() {

    private val _step = MutableStateFlow(OnboardingStep.WELCOME)
    val step: StateFlow<OnboardingStep> = _step.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<UiLanguage?>(null)
    val selectedLanguage: StateFlow<UiLanguage?> = _selectedLanguage.asStateFlow()

    private val _selectedGoal = MutableStateFlow<LearningGoal?>(null)
    val selectedGoal: StateFlow<LearningGoal?> = _selectedGoal.asStateFlow()

    /** Whether the completion write has landed (success or failure). */
    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    fun selectLanguage(language: UiLanguage) {
        _selectedLanguage.value = language
    }

    fun selectGoal(goal: LearningGoal) {
        _selectedGoal.value = goal
    }

    fun next() {
        _step.value = when (_step.value) {
            OnboardingStep.WELCOME -> OnboardingStep.LANGUAGE
            OnboardingStep.LANGUAGE -> OnboardingStep.GOAL
            OnboardingStep.GOAL -> OnboardingStep.GOAL
        }
    }

    /**
     * Persists the chosen language + goal, then flips onboardingComplete.
     * Completion is awaited (suspend chain) so the navigation callback in
     * [OnboardingRoute] fires only after persistence succeeds (audit §5/§8:
     * never navigate before the write lands).
     */
    fun complete(onDone: () -> Unit) {
        val language = _selectedLanguage.value ?: UiLanguage.ENGLISH
        viewModelScope.launch {
            runCatching {
                settingsDataSource.setUiLanguage(language)
                settingsDataSource.setThemeMode(ThemeMode.SYSTEM)
                _selectedGoal.value?.let { settingsDataSource.setLearningGoal(it) }
                settingsDataSource.setOnboardingComplete(true)
            }.onSuccess {
                _completed.value = true
                onDone()
            }.onFailure {
                _completed.value = false
                onDone()
            }
        }
    }
}
