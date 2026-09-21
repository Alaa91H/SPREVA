package com.spreva.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.spreva.core.model.CefrLevel
import com.spreva.core.model.LearningGoal
import com.spreva.core.model.ThemeMode
import com.spreva.core.model.UiLanguage
import com.spreva.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "spreva_settings")

/**
 * Preferences DataStore backed persistence for [UserSettings]
 * (plan section 87: Preferences is acceptable for Phase 3).
 */
class SettingsDataSource(private val context: Context) {

    private val cachedReviewRetention = AtomicReference(0.90)

    private object Keys {
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val LEARNING_GOAL = stringPreferencesKey("learning_goal")
        val RECOMMENDED_LEVEL = stringPreferencesKey("recommended_level")
        val REVIEW_RETENTION_TARGET = doublePreferencesKey("review_retention_target")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            uiLanguage = prefs[Keys.UI_LANGUAGE]?.let { runCatching { UiLanguage.valueOf(it) }.getOrNull() }
                ?: UiLanguage.ENGLISH,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
            learningGoal = prefs[Keys.LEARNING_GOAL]?.let { runCatching { LearningGoal.valueOf(it) }.getOrNull() },
            recommendedLevel = prefs[Keys.RECOMMENDED_LEVEL]?.let {
                runCatching { CefrLevel.valueOf(it) }.getOrNull()
            },
            reviewRetentionTarget = (prefs[Keys.REVIEW_RETENTION_TARGET] ?: 0.90)
                .coerceIn(0.85, 0.95),
        )
    }.onEach { settings ->
        cachedReviewRetention.set(settings.reviewRetentionTarget)
    }

    suspend fun setUiLanguage(language: UiLanguage) {
        context.settingsDataStore.edit { it[Keys.UI_LANGUAGE] = language.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setLearningGoal(goal: LearningGoal) {
        context.settingsDataStore.edit { it[Keys.LEARNING_GOAL] = goal.name }
    }

    suspend fun setRecommendedLevel(level: CefrLevel) {
        context.settingsDataStore.edit { it[Keys.RECOMMENDED_LEVEL] = level.name }
    }

    suspend fun setReviewRetentionTarget(target: Double) {
        val safe = target.coerceIn(0.85, 0.95)
        cachedReviewRetention.set(safe)
        context.settingsDataStore.edit { it[Keys.REVIEW_RETENTION_TARGET] = safe }
    }

    /** Fast synchronous snapshot used by the pure scheduler's interval calculation. */
    fun currentReviewRetentionTarget(): Double = cachedReviewRetention.get()
}
