package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkM3ColorScheme = darkColorScheme(
    primary = DarkTermicoudColors.primary,
    onPrimary = DarkTermicoudColors.onPrimary,
    primaryContainer = DarkTermicoudColors.primaryContainer,
    onPrimaryContainer = DarkTermicoudColors.onPrimaryContainer,
    secondary = DarkTermicoudColors.secondary,
    onSecondary = DarkTermicoudColors.onPrimary,
    background = DarkTermicoudColors.background,
    onBackground = DarkTermicoudColors.textPrimary,
    surface = DarkTermicoudColors.surface,
    onSurface = DarkTermicoudColors.textPrimary,
    surfaceVariant = DarkTermicoudColors.surfaceVariant,
    onSurfaceVariant = DarkTermicoudColors.textSecondary,
    outline = DarkTermicoudColors.border,
    outlineVariant = DarkTermicoudColors.surfaceVariant,
    error = DarkTermicoudColors.dangerRed,
    onError = Color.White
)

private val LightM3ColorScheme = lightColorScheme(
    primary = LightTermicoudColors.primary,
    onPrimary = LightTermicoudColors.onPrimary,
    primaryContainer = LightTermicoudColors.primaryContainer,
    onPrimaryContainer = LightTermicoudColors.onPrimaryContainer,
    secondary = LightTermicoudColors.secondary,
    onSecondary = Color.White,
    background = LightTermicoudColors.background,
    onBackground = LightTermicoudColors.textPrimary,
    surface = LightTermicoudColors.surface,
    onSurface = LightTermicoudColors.textPrimary,
    surfaceVariant = LightTermicoudColors.surfaceVariant,
    onSurfaceVariant = LightTermicoudColors.textSecondary,
    outline = LightTermicoudColors.border,
    outlineVariant = LightTermicoudColors.surfaceVariant,
    error = LightTermicoudColors.dangerRed,
    onError = Color.White
)

@Composable
fun TermicoudTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkTermicoudColors else LightTermicoudColors
    val m3ColorScheme = if (darkTheme) DarkM3ColorScheme else LightM3ColorScheme

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = TermicoudTypography,
            content = content
        )
    }
}

