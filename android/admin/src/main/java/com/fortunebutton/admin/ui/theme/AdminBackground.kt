package com.fortunebutton.admin.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

@Composable
fun adminBackgroundBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    val top = scheme.surface
    val bottom = if (scheme.surface == SurfaceLight) {
        AdminLightGradientMid
    } else {
        AdminDarkGradientMid
    }
    return Brush.verticalGradient(
        colors = listOf(top, bottom),
    )
}

@Composable
fun AdminBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(adminBackgroundBrush()),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}
