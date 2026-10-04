package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    DARK,
    LIGHT
}

// Dark Scheme: Pure Black Background, Dark Charcoal Surface, White Text, Vibrant Red Accent
fun createDarkColorScheme() = darkColorScheme(
    primary = VibrantRed,
    onPrimary = Color.White,
    primaryContainer = VibrantRed.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,
    secondary = VibrantRed,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Color.White,
    background = DarkBackground, // Pure Black (#000000)
    onBackground = TextPrimaryDark, // White (#FFFFFF)
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = DarkSurfaceContainer,
    outline = DarkCardBorder
)

// Light Scheme: Pure White Background, Clean Light Surface, Solid Black Text, Vibrant Red Accent
fun createLightColorScheme() = lightColorScheme(
    primary = VibrantRed,
    onPrimary = Color.White,
    primaryContainer = VibrantRed.copy(alpha = 0.15f),
    onPrimaryContainer = DarkVibrantRed,
    secondary = VibrantRed,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = Color.Black,
    background = LightBackground, // Pure White (#FFFFFF)
    onBackground = TextPrimaryLight, // Black (#000000)
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = LightSurfaceContainer,
    outline = LightCardBorder
)

@Composable
fun MusicPlayerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = themeMode != ThemeMode.LIGHT

    val colorScheme = if (isDark) {
        createDarkColorScheme()
    } else {
        createLightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

