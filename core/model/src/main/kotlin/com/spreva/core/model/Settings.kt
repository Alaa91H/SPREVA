package com.spreva.core.model

/** UI language of the app chrome (not the learning content). */
enum class UiLanguage {
    ENGLISH,
    ARABIC,
}

/** Theme mode preference. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** User-level settings persisted in DataStore. */
data class UserSettings(
    val uiLanguage: UiLanguage = UiLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val onboardingComplete: Boolean = false,
    /** Learner goal from onboarding (audit §6). Null until chosen. */
    val learningGoal: LearningGoal? = null,
    /** Latest adaptive diagnostic recommendation; not a CEFR certificate. */
    val recommendedLevel: CefrLevel? = null,
)
