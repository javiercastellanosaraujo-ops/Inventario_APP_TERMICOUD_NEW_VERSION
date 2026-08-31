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

// Dark Sky Blue & Slate Obsidian Palette
val DarkTermicoudColors = TermicoudColors(
    primary = Color(0xFF38BDF8), // Vibrant Sky Blue 400
    primaryDim = Color(0xFF0284C7), // Deep Sky Blue 600
    primaryContainer = Color(0xFF0C2B42), // Deep Sky Slate Container
    onPrimaryContainer = Color(0xFFBAE6FD), // Sky Blue 200
    onPrimary = Color(0xFF04192B), // Deep Slate Navy for high contrast on Sky 400
    secondary = Color(0xFF7DD3FC), // Sky Blue 300 Accent
    background = Color(0xFF0B111A), // Deep Slate Obsidian Canvas
    surface = Color(0xFF131D2D), // Sleek Modern Slate Card Surface
    surfaceVariant = Color(0xFF1B273C), // Elevated Container / Inputs / Chips
    border = Color(0xFF2B3C54), // Crisp Slate Border (Visible, framed)
    borderFocused = Color(0xFF38BDF8),
    textPrimary = Color(0xFFF8FAFC), // Slate 50 (Crisp, High Legibility)
    textSecondary = Color(0xFF94A3B8), // Cool Slate 400
    textMuted = Color(0xFF64748B), // Slate 500
    statusAgotado = Color(0xFFFB7185), // Rose 400
    statusAgotadoBg = Color(0xFF38141F),
    statusBajo = Color(0xFFFBBF24), // Amber 400
    statusBajoBg = Color(0xFF38290E),
    statusOk = Color(0xFF34D399), // Emerald 400
    statusOkBg = Color(0xFF063324),
    warningAmber = Color(0xFFFBBF24),
    warningAmberBg = Color(0xFF38290E),
    dangerRed = Color(0xFFFB7185),
    alertRed = Color(0xFFFB7185),
    cardHighlight = Color(0xFF17263C),
    isDark = true
)

// Light Sky Blue & Modern Slate Palette
val LightTermicoudColors = TermicoudColors(
    primary = Color(0xFF0284C7), // Vibrant Sky Blue 600
    primaryDim = Color(0xFF0369A1), // Deep Ocean Sky 700
    primaryContainer = Color(0xFFE0F2FE), // Ice Sky 100
    onPrimaryContainer = Color(0xFF0369A1), // Sky 700
    onPrimary = Color(0xFFFFFFFF), // Pure White on Sky Blue 600
    secondary = Color(0xFF0EA5E9), // Sky Blue 500
    background = Color(0xFFF1F5F9), // Slate 100 Canvas (clear contrast against white cards)
    surface = Color(0xFFFFFFFF), // Pure White Cards & Modals
    surfaceVariant = Color(0xFFF8FAFC), // Soft Slate 50 Containers / Inputs
    border = Color(0xFFCBD5E1), // Crisp Slate 300 Border (Crisp framed boundaries)
    borderFocused = Color(0xFF0284C7),
    textPrimary = Color(0xFF0F172A), // Slate 900 (High Contrast)
    textSecondary = Color(0xFF334155), // Slate 700
    textMuted = Color(0xFF64748B), // Slate 500
    statusAgotado = Color(0xFFDC2626), // Ruby Red 600
    statusAgotadoBg = Color(0xFFFEE2E2), // Soft Pink Red 100
    statusBajo = Color(0xFFD97706), // Amber Gold 600
    statusBajoBg = Color(0xFFFEF3C7), // Soft Amber Gold 100
    statusOk = Color(0xFF059669), // Emerald Green 600
    statusOkBg = Color(0xFFECFDF5), // Soft Emerald Pastel 50
    warningAmber = Color(0xFFD97706),
    warningAmberBg = Color(0xFFFEF3C7),
    dangerRed = Color(0xFFDC2626),
    alertRed = Color(0xFFDC2626),
    cardHighlight = Color(0xFFF0F9FF), // Sky 50
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

