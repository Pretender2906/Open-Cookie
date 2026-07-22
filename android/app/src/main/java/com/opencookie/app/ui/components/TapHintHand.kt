package com.opencookie.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

private val HandFill = Color(0xFFF1E6D2)
private val HandEdge = Color(0xFFFBF3E1)
private val Ripple = Color(0xFFE9C07A)

/**
 * A quiet, premium tap hint: a soft translucent hand rises toward the cookie, presses,
 * emits a warm ripple at the contact point, then lifts and repeats. Drawn as a minimal
 * silhouette (no emoji, no cartoon) so it belongs to the atmosphere. Shown only until the
 * user has tapped once.
 */
@Composable
fun TapHintHand(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "tap_hint")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "tap_hint_cycle",
    )

    Canvas(modifier = modifier.size(96.dp)) {
        val approach = (cycle / 0.32f).coerceIn(0f, 1f)
        val riseAmount = size.height * 0.18f
        val shiftY = lerp(riseAmount, 0f, easeOut(approach))

        val pressScale = when {
            cycle < 0.34f -> 1f
            cycle < 0.5f -> lerp(1f, 0.93f, (cycle - 0.34f) / 0.16f)
            cycle < 0.62f -> lerp(0.93f, 1f, (cycle - 0.5f) / 0.12f)
            else -> 1f
        }

        val alpha = when {
            cycle < 0.12f -> cycle / 0.12f
            cycle > 0.82f -> lerp(1f, 0f, (cycle - 0.82f) / 0.18f)
            else -> 1f
        }

        val rippleProgress = ((cycle - 0.44f) / 0.4f).coerceIn(0f, 1f)

        val s = size.minDimension
        val cx = size.width / 2f
        val tipY = size.height * 0.20f
        val contact = Offset(cx, tipY + shiftY)

        // Warm ripple at the contact point.
        if (rippleProgress > 0f) {
            val maxR = s * 0.34f
            drawCircle(
                color = Ripple.copy(alpha = (1f - rippleProgress) * 0.6f * alpha),
                radius = maxR * rippleProgress,
                center = contact,
                style = Stroke(width = s * 0.02f),
            )
            drawCircle(
                color = Ripple.copy(alpha = (1f - rippleProgress) * 0.2f * alpha),
                radius = maxR * rippleProgress,
                center = contact,
            )
        }

        withTransform({
            translate(top = shiftY)
            scale(pressScale, pressScale, pivot = Offset(cx, tipY))
        }) {
            drawHand(cx, tipY, s, alpha)
        }
    }
}

private fun DrawScope.drawHand(cx: Float, tipY: Float, s: Float, alpha: Float) {
    val fingerW = s * 0.15f
    val fingerLen = s * 0.34f
    val fistW = s * 0.34f
    val fistH = s * 0.32f
    val fistTop = tipY + fingerLen * 0.55f

    // Soft glow behind the hand.
    drawCircle(
        color = HandFill.copy(alpha = 0.06f * alpha),
        radius = s * 0.3f,
        center = Offset(cx, fistTop + fistH * 0.35f),
    )

    // Fist / knuckles.
    drawRoundRect(
        color = HandFill.copy(alpha = 0.5f * alpha),
        topLeft = Offset(cx - fistW / 2f, fistTop),
        size = Size(fistW, fistH),
        cornerRadius = CornerRadius(s * 0.14f, s * 0.14f),
    )
    // Thumb.
    drawRoundRect(
        color = HandFill.copy(alpha = 0.5f * alpha),
        topLeft = Offset(cx - fistW * 0.62f, fistTop + fistH * 0.18f),
        size = Size(fistW * 0.32f, fistH * 0.5f),
        cornerRadius = CornerRadius(s * 0.08f, s * 0.08f),
    )
    // Index finger.
    drawRoundRect(
        color = HandFill.copy(alpha = 0.52f * alpha),
        topLeft = Offset(cx - fingerW / 2f, tipY),
        size = Size(fingerW, fingerLen),
        cornerRadius = CornerRadius(fingerW / 2f, fingerW / 2f),
    )
    // Refined edge highlight on the finger.
    drawRoundRect(
        color = HandEdge.copy(alpha = 0.5f * alpha),
        topLeft = Offset(cx - fingerW / 2f, tipY),
        size = Size(fingerW, fingerLen),
        cornerRadius = CornerRadius(fingerW / 2f, fingerW / 2f),
        style = Stroke(width = s * 0.01f),
    )
}

private fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction

private fun easeOut(t: Float) = 1f - (1f - t) * (1f - t)
