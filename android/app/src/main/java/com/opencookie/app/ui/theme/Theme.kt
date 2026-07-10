package com.opencookie.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CookieOrange = Color(0xFFFF8C00)
private val CookieOrangeDark = Color(0xFFE67E00)
private val BackgroundDark = Color(0xFF12100E)
private val SurfaceDark = Color(0xFF1E1A17)
private val OnSurfaceDark = Color(0xFFF5EDE6)

private val DarkColorScheme = darkColorScheme(
    primary = CookieOrange,
    onPrimary = Color.Black,
    primaryContainer = CookieOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFFB347),
    onSecondary = Color.Black,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF2A2420),
    onSurfaceVariant = OnSurfaceDark.copy(alpha = 0.72f),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    error = Color(0xFFCF6679),
    onError = Color.Black,
)

@Composable
fun OpenCookieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
