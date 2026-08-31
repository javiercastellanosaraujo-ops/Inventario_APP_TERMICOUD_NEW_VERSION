package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Semantic Theme Colors Holder for Termicoud
data class TermicoudColors(
    val primary: Color,
    val primaryDim: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val onPrimary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderFocused: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val statusAgotado: Color,
    val statusAgotadoBg: Color,
    val statusBajo: Color,
    val statusBajoBg: Color,
    val statusOk: Color,
    val statusOkBg: Color,
    val warningAmber: Color,
    val warningAmberBg: Color,
    val dangerRed: Color,
    val alertRed: Color,
    val cardHighlight: Color,
    val isDark: Boolean
)

// Dark Graphite & Blue Palette
val DarkTermicoudColors = TermicoudColors(
    primary = Color(0xFF3DA9FF),
    primaryDim = Color(0xFF1E7FD9),
    primaryContainer = Color(0xFF0D2A44),
    onPrimaryContainer = Color(0xFFB8E0FF),
    onPrimary = Color(0xFF04182A),
    secondary = Color(0xFF7DD3FC),
    background = Color(0xFF141414),        // Grafito casi negro
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF282828),
    border = Color(0xFF383838),
    borderFocused = Color(0xFF3DA9FF),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFB0B0B0),
    textMuted = Color(0xFF808080),
    statusAgotado = Color(0xFFFB7185),
    statusAgotadoBg = Color(0xFF38141F),
    statusBajo = Color(0xFFFBBF24),
    statusBajoBg = Color(0xFF38290E),
    statusOk = Color(0xFF34D399),
    statusOkBg = Color(0xFF063324),
    warningAmber = Color(0xFFFBBF24),
    warningAmberBg = Color(0xFF38290E),
    dangerRed = Color(0xFFFB7185),
    alertRed = Color(0xFFFB7185),
    cardHighlight = Color(0xFF102A40),
    isDark = true
)

// Light Warm Neutral & Blue Palette
val LightTermicoudColors = TermicoudColors(
    primary = Color(0xFF0B6FB8),
    primaryDim = Color(0xFF085688),
    primaryContainer = Color(0xFF3DA9FF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF1E88C7),
    background = Color(0xFFE7E7E3),        // Gris suave, ya no blanco
    surface = Color(0xFFF1F1ED),           // Un poco más claro que el fondo, para que las tarjetas se distingan
    surfaceVariant = Color(0xFFDDDDD7),
    border = Color(0xFFAAAAA2),            // Bastante más oscuro/visible que antes
    borderFocused = Color(0xFF0B6FB8),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF4A4A4A),
    textMuted = Color(0xFF757575),
    statusAgotado = Color(0xFFDC2626),
    statusAgotadoBg = Color(0xFFFEE2E2),
    statusBajo = Color(0xFFD97706),
    statusBajoBg = Color(0xFFFEF3C7),
    statusOk = Color(0xFF059669),
    statusOkBg = Color(0xFFECFDF5),
    warningAmber = Color(0xFFD97706),
    warningAmberBg = Color(0xFFFEF3C7),
    dangerRed = Color(0xFFDC2626),
    alertRed = Color(0xFFDC2626),
    cardHighlight = Color(0xFFDCEEFC),
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { DarkTermicoudColors }

// Dynamic properties resolving automatically with the active theme in Composable scopes
val ElectricLime: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primary
val ElectricLimeDim: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryDim
val ElectricLimeContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryContainer
val OnElectricLimeContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onPrimaryContainer
val OnElectricLime: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onPrimary

val GraphiteBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.background
val GraphiteSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface
val GraphiteSurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceVariant
val GraphiteBorder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.border
val GraphiteBorderFocused: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.borderFocused

val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textSecondary
val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textMuted

val StatusAgotado: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusAgotado
val StatusAgotadoBg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusAgotadoBg
val StatusBajo: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusBajo
val StatusBajoBg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusBajoBg
val StatusOk: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusOk
val StatusOkBg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusOkBg

val DangerRed: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.dangerRed
val AlertRed: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.alertRed
val WarningAmber: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.warningAmber
val WarningAmberBg: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.warningAmberBg

val CardHighlightColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.cardHighlight
val isAppDarkTheme: Boolean @Composable @ReadOnlyComposable get() = LocalAppColors.current.isDark

