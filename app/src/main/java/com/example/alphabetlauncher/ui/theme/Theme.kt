package com.example.alphabetlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = BlackBackground,
    background = BlackBackground,
    onBackground = WhiteText,
    surface = BlackSurface,
    onSurface = WhiteText,
    surfaceVariant = BlackSurfaceVariant,
    onSurfaceVariant = WhiteText
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = WhiteBackground,
    background = WhiteBackground,
    onBackground = DarkText,
    surface = WhiteSurface,
    onSurface = DarkText,
    surfaceVariant = WhiteSurfaceVariant,
    onSurfaceVariant = DarkText
)

@Composable
fun AlphabetLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}