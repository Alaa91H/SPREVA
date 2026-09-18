package com.spreva.core.model

/**
 * A localized text value. German learning content is stored as data with
 * explicit per-language fields; UI languages map to [translations].
 */
data class LocalizedText(
    val de: String,
    val ar: String? = null,
    val en: String? = null,
) {
    /**
     * Instruction-language resolution (audit §9): [UiLanguage] is the app
     * chrome language; instruction translations follow it with graceful
     * fallback. German learning targets must NOT use this — they stay
     * German regardless of UI language (target vs instruction policy).
     */
    fun resolveFor(language: UiLanguage): String = when (language) {
        UiLanguage.ARABIC -> ar ?: en ?: de
        UiLanguage.ENGLISH -> en ?: de
    }
}
