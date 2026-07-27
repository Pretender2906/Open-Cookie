package com.opencookie.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// region Design tokens

/** Warm antique gold — the single accent of the product. Never a flat saturated orange. */
val CookieGold = Color(0xFFE4B876)
val CookieGoldDeep = Color(0xFFC79155)
val CookieBronze = Color(0xFF8A6236)

/** Text tones. */
val CookieCream = Color(0xFFF1E6D2)
val CookieCreamDim = Color(0xFFB9A88C)
val Parchment = Color(0xFFEFE2C4)
val PaperInk = Color(0xFF5A4034)

/** Deep warm backgrounds — near-black espresso. */
val EspressoTop = Color(0xFF1E1512)
val EspressoMid = Color(0xFF130D0B)
val EspressoDeep = Color(0xFF080605)
val CocoaSurface = Color(0xFF1C1512)
val CocoaSurfaceHi = Color(0xFF241A15)

// endregion

private val DarkColorScheme = darkColorScheme(
    primary = CookieGold,
    onPrimary = Color(0xFF2A1B0D),
    primaryContainer = Color(0xFF2C2018),
    onPrimaryContainer = CookieCream,
    secondary = CookieGoldDeep,
    onSecondary = Color(0xFF2A1B0D),
    secondaryContainer = Color(0xFF2A2019),
    onSecondaryContainer = CookieCream,
    tertiary = CookieBronze,
    surface = CocoaSurface,
    onSurface = CookieCream,
    surfaceVariant = CocoaSurfaceHi,
    onSurfaceVariant = CookieCreamDim,
    background = EspressoMid,
    onBackground = CookieCream,
    outline = Color(0xFF5A4636),
    outlineVariant = Color(0xFF3A2C21),
    error = Color(0xFFB64432),
    onError = Color(0xFF2A1512),
    errorContainer = Color(0xFF3A211E),
    onErrorContainer = Color(0xFFF0C9C4),
)

private val Serif = FontFamily.Serif

/**
 * Refined type scale: elegant serif for brand/editorial hierarchy (display + headline +
 * titleLarge), clean sans defaults for functional text so information stays readable.
 */
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Light,
        fontSize = 54.sp, lineHeight = 60.sp, letterSpacing = 2.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Normal,
        fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = 2.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Normal,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = 1.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Normal,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = 0.5.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Normal,
        fontSize = 26.sp, lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Medium,
        fontSize = 22.sp, lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Medium,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.3.sp,
    ),
)

@Composable
fun OpenCookieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
