package com.spreva.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.spreva.core.model.ThemeMode

/**
 * Spreva brand theme (default). Dynamic color is optional (plan section 33:
 * the app keeps its identity; device colors are opt-in).
 */
@Composable
fun SprevaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> SprevaDarkColors
        else -> SprevaLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SprevaTypography,
        content = content,
    )
}

/**
 * Subtle article color coding for German nouns (der/die/das): masculine
 * blue-ish, feminine warm, neuter green-ish — derived from the active
 * scheme so it adapts to dark mode (plan section 37).
 */
@Composable
fun sprevaArticleColor(article: String): Color = when (article.lowercase()) {
    "der" -> MaterialTheme.colorScheme.primary
    "die" -> MaterialTheme.colorScheme.tertiary
    "das" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.onSurface
}
