package com.quizbowl.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Extended colors accessible anywhere in the app
data class QuizBowlColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val primary: Color,
    val accentTeal: Color,
    val accentRose: Color,
    val accentAmber: Color,
    val green: Color,
    val red: Color,
    val textMuted: Color,
    val greenDim: Color,
    val redDim: Color,
    val amberDim: Color,
    val primaryGlow: Color,
)

val DarkQuizBowlColors = QuizBowlColors(
    bg = BgDark,
    surface = SurfaceDark,
    surface2 = Surface2Dark,
    border = BorderDark,
    primary = Primary,
    accentTeal = AccentTeal,
    accentRose = AccentRose,
    accentAmber = AccentAmber,
    green = GreenDark,
    red = RedDark,
    textMuted = TextMutedDark,
    greenDim = Color(0x1F34D399),
    redDim = Color(0x1FF87171),
    amberDim = Color(0x1FFBBF24),
    primaryGlow = PrimaryGlow,
)

val LightQuizBowlColors = QuizBowlColors(
    bg = BgLight,
    surface = SurfaceLight,
    surface2 = Surface2Light,
    border = BorderLight,
    primary = Primary,
    accentTeal = Color(0xFF0D9488),
    accentRose = AccentRose,
    accentAmber = AccentAmber,
    green = GreenLight,
    red = RedLight,
    textMuted = TextMutedLight,
    greenDim = Color(0x1A10B981),
    redDim = Color(0x1AEF4444),
    amberDim = Color(0x1AF59E0B),
    primaryGlow = Color(0x1A646CFF),
)

val LocalQuizBowlColors = staticCompositionLocalOf { DarkQuizBowlColors }

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    background = BgDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    background = BgLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
)

@Composable
fun QuizBowlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val qbColors = if (darkTheme) DarkQuizBowlColors else LightQuizBowlColors

    CompositionLocalProvider(LocalQuizBowlColors provides qbColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QuizBowlTypography,
            content = content,
        )
    }
}

// Convenience accessor
val MaterialTheme.qbColors: QuizBowlColors
    @Composable get() = LocalQuizBowlColors.current
