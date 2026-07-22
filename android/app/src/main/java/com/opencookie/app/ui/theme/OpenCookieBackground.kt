package com.opencookie.app.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign

/**
 * The single, shared atmospheric backdrop for the whole app: a deep warm espresso gradient
 * with a soft amber glow high on the screen. Every top-level screen sits on this so the
 * product feels like one continuous space.
 */
@Composable
fun OpenCookieBackground(
    modifier: Modifier = Modifier,
    glowCenterFraction: Float = 0.34f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(EspressoTop, EspressoMid, EspressoDeep),
                        startY = 0f,
                        endY = size.height,
                    ),
                )
                val glowCenter = Offset(size.width * 0.5f, size.height * glowCenterFraction)
                val glowRadius = size.maxDimension * 0.62f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2EE0A24E), Color(0x00E0A24E)),
                        center = glowCenter,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = glowCenter,
                )
            },
        content = content,
    )
}

/**
 * The brand wordmark. Elegant uppercase serif with generous tracking and a restrained warm
 * gold gradient — understated, premium, never a bold saturated-orange Material title.
 */
@Composable
fun OpenCookieWordmark(
    modifier: Modifier = Modifier,
    fontSize: Int = 26,
    letterSpacing: Double = 6.0,
) {
    Text(
        text = "OPEN COOKIE",
        modifier = modifier,
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize.sp,
            letterSpacing = letterSpacing.sp,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFF3D9A6), CookieGold, CookieGoldDeep),
            ),
        ),
    )
}
