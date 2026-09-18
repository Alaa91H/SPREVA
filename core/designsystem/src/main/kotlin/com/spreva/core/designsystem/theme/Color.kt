package com.spreva.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Spreva brand color direction (Phase 2 blueprint section 32):
 * primary deep indigo, secondary fresh mint, tertiary warm amber.
 * Tonal values tuned for contrast; not final accessibility tokens.
 */
object SprevaColors {
    val PrimarySeed = Color(0xFF5457F6)
    val SecondarySeed = Color(0xFF35C9A5)
    val TertiarySeed = Color(0xFFF1B64A)

    // Semantic states (plan section 302: semantic tokens, not raw colors in UI logic)
    val Success = Color(0xFF2E7D32)
    val SuccessContainer = Color(0xFFB7EFC0)
    val Warning = Color(0xFF9A6B00)
    val Info = Color(0xFF1565C0)
    val Critical = Color(0xFFB3261E)
}

val SprevaLightColors = lightColorScheme(
    primary = Color(0xFF3E43C8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE1E1FF),
    onPrimaryContainer = Color(0xFF0D0F6B),
    secondary = Color(0xFF18705D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFA9F2DD),
    onSecondaryContainer = Color(0xFF00382C),
    tertiary = Color(0xFF7A5719),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF291800),
    error = SprevaColors.Critical,
    background = Color(0xFFFCF8F8),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFCF8F8),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
)

val SprevaDarkColors = darkColorScheme(
    primary = Color(0xFFBCC2FF),
    onPrimary = Color(0xFF232F9E),
    primaryContainer = Color(0xFF3A41B5),
    onPrimaryContainer = Color(0xFFE1E1FF),
    secondary = Color(0xFF8DD5C1),
    onSecondary = Color(0xFF00382C),
    secondaryContainer = Color(0xFF005143),
    onSecondaryContainer = Color(0xFFA9F2DD),
    tertiary = Color(0xFFEDC26C),
    onTertiary = Color(0xFF432C00),
    tertiaryContainer = Color(0xFF5F4000),
    onTertiaryContainer = Color(0xFFFFDDB0),
    error = Color(0xFFF2B8B5),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
)
