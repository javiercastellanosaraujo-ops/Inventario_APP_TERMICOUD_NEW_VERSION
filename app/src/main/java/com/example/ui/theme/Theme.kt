package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricLime,
    onPrimary = Color(0xFF141414),
    primaryContainer = ElectricLimeContainer,
    onPrimaryContainer = OnElectricLimeContainer,
    secondary = Color(0xFFD0D0D0),
    onSecondary = Color(0xFF141414),
    background = GraphiteBackground,
    onBackground = TextPrimary,
    surface = GraphiteSurface,
    onSurface = TextPrimary,
    surfaceVariant = GraphiteSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GraphiteBorder,
    outlineVariant = Color(0xFF2C2C2C),
    error = DangerRed,
    onError = Color.White
)

@Composable
fun TermicoudTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = TermicoudTypography,
        content = content
    )
}
