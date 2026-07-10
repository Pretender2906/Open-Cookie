package com.opencookie.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CookieOrange,
    onPrimary = Color.Black,
    primaryContainer = AdminLightPrimaryContainer,
    onPrimaryContainer = AdminLightOnPrimaryContainer,
    secondary = CookieOrangeDark,
    onSecondary = Color.White,
    secondaryContainer = AdminLightSecondaryContainer,
    onSecondaryContainer = AdminLightOnSecondaryContainer,
    tertiary = CookieOrangeLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = AdminLightSurfaceVariant,
    onSurfaceVariant = AdminLightOnSurfaceVariant,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    error = ErrorRed,
    onError = Color.Black,
    errorContainer = AdminLightErrorContainer,
    onErrorContainer = AdminLightOnErrorContainer,
    outline = AdminLightOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = CookieOrange,
    onPrimary = Color.Black,
    primaryContainer = AdminDarkPrimaryContainer,
    onPrimaryContainer = AdminDarkOnPrimaryContainer,
    secondary = CookieOrangeDark,
    onSecondary = Color.White,
    secondaryContainer = AdminDarkSecondaryContainer,
    onSecondaryContainer = AdminDarkOnSecondaryContainer,
    tertiary = CookieOrangeLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = AdminDarkSurfaceVariant,
    onSurfaceVariant = AdminDarkOnSurfaceVariant,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    error = ErrorRed,
    onError = Color.Black,
    errorContainer = AdminDarkErrorContainer,
    onErrorContainer = AdminDarkOnErrorContainer,
    outline = AdminDarkOutline,
)

@Composable
fun AdminTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AdminTypography,
        shapes = AdminShapes,
        content = content,
    )
}
