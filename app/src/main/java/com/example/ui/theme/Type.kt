package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Monospace font family for numbers, prices, stock quantities, IDs, rates & tabular figures
val MonoFontFamily = FontFamily.Monospace

val TermicoudTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.25.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.25.sp,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = TextSecondary
    )
)

// Dedicated Monospace text styles for data, prices, and quantities
val MonoDataLarge = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    letterSpacing = 0.sp,
    color = ElectricLime
)

val MonoDataMedium = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    letterSpacing = 0.sp,
    color = TextPrimary
)

val MonoDataSmall = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.sp,
    color = TextSecondary
)
