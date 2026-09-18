package com.spreva.core.model

/**
 * A localized text value. German learning content is stored as data with
 * explicit per-language fields; UI languages map to [translations].
 */
data class LocalizedText(
    val de: String,
    val ar: String? = null,
    val en: String? = null,
)
