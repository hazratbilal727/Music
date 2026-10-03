package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode {
    SYSTEM,
    DARK,
    AMOLED,
    LIGHT
}

enum class AccentChoice(val label: String, val color: Color) {
    VIOLET("Electric Violet", ElectricViolet),
    CYAN("Neon Cyan", ElectricCyan),
    CORAL("Coral Glow", NeonCoral),
    EMERALD("Emerald Wave", EmeraldWave),
    AMBER("Sunset Gold", SunsetAmber)
}

fun createDarkColorScheme(accent: Color = ElectricViolet) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = Color.Black,
    secondaryContainer = ElectricCyan.copy(alpha = 0.2f),
    onSecondaryContainer = Color.White,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = DarkSurfaceContainer,
    outline = DarkCardBorder
)

fun createAmoledColorScheme(accent: Color = ElectricViolet) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = Color.Black,
    secondaryContainer = ElectricCyan.copy(alpha = 0.2f),
    onSecondaryContainer = Color.White,
    background = AmoledBackground,
    onBackground = TextPrimaryDark,
    surface = AmoledSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = AmoledSurfaceContainer,
    outline = AmoledCardBorder
)

fun createLightColorScheme(accent: Color = ElectricVioletVariant) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.15f),
    onPrimaryContainer = accent,
    secondary = ElectricCyan,
    onSecondary = Color.White,
    secondaryContainer = ElectricCyan.copy(alpha = 0.15f),
    onSecondaryContainer = ElectricCyan,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = LightSurfaceContainer,
    outline = LightCardBorder
)

@Composable
fun MusicPlayerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentChoice: AccentChoice = AccentChoice.VIOLET,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == ThemeMode.AMOLED -> createAmoledColorScheme(accentChoice.color)
        isDark -> createDarkColorScheme(accentChoice.color)
        else -> createLightColorScheme(accentChoice.color)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
