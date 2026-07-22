package com.opencookie.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.opencookie.app.R
import kotlin.math.sin

enum class CookiePhase { IDLE, BREAKING, REVEALING, REVEALED }

/**
 * The interactive Fortune Cookie built from the real 3D cookie assets.
 *
 *  - IDLE: the intact cookie breathes, floats and drifts, lifted by a soft amber glow.
 *  - BREAKING → REVEALED: the intact cookie is swapped for its two matching halves (which
 *    overlap into the same silhouette at zero separation, so there is no crossfade), then
 *    they compress, shake, tilt and slide apart while a warm light escapes from inside.
 *
 * The fortune [paper] is slotted into the center lane between the halves, while the
 * halves themselves stay in front only at the outer edges. The whole stage is the tap
 * target — there is no button.
 */
@Composable
fun FortuneCookieStage(
    phase: CookiePhase,
    tappable: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    paper: @Composable () -> Unit = {},
) {
    val breakTarget = when (phase) {
        CookiePhase.IDLE -> 0f
        CookiePhase.BREAKING -> 0.4f
        CookiePhase.REVEALING, CookiePhase.REVEALED -> 1f
    }
    val breakProgress by animateFloatAsState(
        targetValue = breakTarget,
        animationSpec = tween(
            durationMillis = if (phase == CookiePhase.BREAKING) 520 else 780,
            easing = FastOutSlowInEasing,
        ),
        label = "break_progress",
    )

    val idle = rememberInfiniteTransition(label = "cookie_idle")
    val idleScale by idle.animateFloat(
        initialValue = 0.99f, targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "idle_scale",
    )
    val idleRotation by idle.animateFloat(
        initialValue = -1.8f, targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(4600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "idle_rotation",
    )
    val idleFloat by idle.animateFloat(
        initialValue = -7f, targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(3800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "idle_float",
    )
    val particleTime by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart),
        label = "particle_time",
    )
    val glowPulse by idle.animateFloat(
        initialValue = 0.85f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_pulse",
    )

    val shake = remember { Animatable(0f) }
    LaunchedEffect(phase) {
        if (phase == CookiePhase.BREAKING) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 440
                    0f at 0
                    1f at 55
                    -0.8f at 125
                    0.55f at 205
                    -0.3f at 300
                    0f at 440
                },
            )
        } else {
            shake.snapTo(0f)
        }
    }

    val isIdle = phase == CookiePhase.IDLE
    val showHalves = phase != CookiePhase.IDLE

    Box(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = tappable,
            onClick = onTap,
        ),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient glowing dust.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAmbientParticles(particleTime, glow = if (isIdle) 1f else 0.5f)
        }

        // Soft floating glow beneath the cookie + warm light escaping while it opens.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = size.minDimension
            val underR = base * 0.42f * (if (isIdle) glowPulse else 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4DE9A24E), Color(0x00E9A24E)),
                    center = Offset(cx, cy + base * 0.26f),
                    radius = underR,
                ),
                radius = underR,
                center = Offset(cx, cy + base * 0.26f),
            )
            if (breakProgress > 0.05f) {
                val innerR = base * 0.34f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF1CE).copy(alpha = 0.9f * breakProgress),
                            Color(0x00FFF1CE),
                        ),
                        center = Offset(cx, cy),
                        radius = innerR,
                    ),
                    radius = innerR,
                    center = Offset(cx, cy),
                )
            }
        }

        if (showHalves) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = (size.height * 0.018f) + (1f - breakProgress) * 18f * density
                    },
                contentAlignment = Alignment.Center,
            ) {
                paper()
            }
        }

        if (!showHalves) {
            Image(
                painter = painterResource(R.drawable.intact_cookie),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = idleScale
                        scaleY = idleScale
                        rotationZ = idleRotation
                        translationY = idleFloat * density
                    },
            )
        } else {
            val squash = 1f - 0.06f * (1f - (breakProgress / 0.2f).coerceIn(0f, 1f))
            Image(
                painter = painterResource(R.drawable.cookie_left_half),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = (-breakProgress * size.width * 0.245f) + shake.value * 10f * density
                        translationY = breakProgress * size.height * 0.028f
                        rotationZ = -breakProgress * 9f
                        scaleX = squash
                        scaleY = squash
                    },
            )
            Image(
                painter = painterResource(R.drawable.cookie_right_half),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = (breakProgress * size.width * 0.245f) + shake.value * 10f * density
                        translationY = breakProgress * size.height * 0.028f
                        rotationZ = breakProgress * 9f
                        scaleX = squash
                        scaleY = squash
                    },
            )
        }

        if (showHalves) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawBreakCrumbs(breakProgress)
            }
        }

    }
}

private val Crumbs = listOf(
    // dirX, dirY, sizeRel, gravity, rotation
    floatArrayOf(-0.56f, 0.24f, 0.028f, 0.72f, -0.7f),
    floatArrayOf(-0.42f, 0.36f, 0.022f, 0.92f, 0.45f),
    floatArrayOf(-0.26f, 0.46f, 0.018f, 1.06f, 1.1f),
    floatArrayOf(-0.08f, 0.50f, 0.014f, 0.88f, -0.2f),
    floatArrayOf(0.1f, 0.48f, 0.021f, 1.02f, -0.36f),
    floatArrayOf(0.28f, 0.42f, 0.017f, 0.82f, 0.86f),
    floatArrayOf(0.46f, 0.32f, 0.024f, 0.78f, -1.18f),
    floatArrayOf(0.6f, 0.22f, 0.029f, 0.88f, -0.92f),
    floatArrayOf(-0.18f, 0.58f, 0.013f, 1.16f, 1.6f),
    floatArrayOf(0.0f, 0.60f, 0.032f, 1.24f, 0.35f),
    floatArrayOf(0.2f, 0.56f, 0.015f, 1.0f, 2.1f),
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBreakCrumbs(progress: Float) {
    if (progress <= 0.02f) return
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension * 0.31f
    val appear = ((progress - 0.05f) / 0.2f).coerceIn(0f, 1f)
    val settle = progress.coerceIn(0f, 1f)
    Crumbs.forEachIndexed { i, c ->
        val dirX = c[0]
        val dirY = c[1]
        val crumbSize = r * c[2]
        val gravity = c[3]
        val rotation = c[4] + settle * (0.6f + i * 0.08f)
        val x = cx + dirX * r * (0.22f + 1.15f * settle)
        val y = cy + dirY * r * 0.38f + gravity * settle * settle * r * 0.76f - (1f - settle) * r * 0.05f
        val warm = when (i % 3) {
            0 -> Color(0xFFE4A45A)
            1 -> Color(0xFFC9853C)
            else -> Color(0xFFF0C88A)
        }
        drawCrumb(
            center = Offset(x, y),
            radius = crumbSize,
            rotation = rotation,
            color = warm.copy(alpha = 0.9f * appear),
            seed = i,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrumb(
    center: Offset,
    radius: Float,
    rotation: Float,
    color: Color,
    seed: Int,
) {
    withTransform({
        rotate(degrees = rotation * 57.2958f, pivot = center)
    }) {
        drawOval(
            color = color,
            topLeft = Offset(center.x - radius * 0.82f, center.y - radius * 0.52f),
            size = Size(radius * 1.68f, radius * 1.04f),
        )
        drawOval(
            color = color.copy(alpha = color.alpha * 0.96f),
            topLeft = Offset(center.x - radius * 0.46f, center.y - radius * 0.64f),
            size = Size(radius * 0.96f, radius * 0.92f),
        )
        drawOval(
            color = color.copy(alpha = color.alpha * 0.92f),
            topLeft = Offset(center.x + radius * 0.04f, center.y - radius * 0.38f),
            size = Size(radius * 0.78f, radius * 0.72f),
        )
        if (seed % 2 == 0) {
            drawOval(
                color = color.copy(alpha = color.alpha * 0.88f),
                topLeft = Offset(center.x - radius * 0.12f, center.y + radius * 0.02f),
                size = Size(radius * 0.62f, radius * 0.48f),
            )
        }
        drawOval(
            color = Color(0xFFFFD89E).copy(alpha = color.alpha * 0.34f),
            topLeft = Offset(center.x - radius * 0.34f, center.y - radius * 0.28f),
            size = Size(radius * 0.64f, radius * 0.24f),
        )
        drawOval(
            color = Color(0xFF8D541F).copy(alpha = color.alpha * 0.18f),
            topLeft = Offset(center.x - radius * 0.18f, center.y + radius * 0.12f),
            size = Size(radius * 0.7f, radius * 0.18f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAmbientParticles(
    time: Float,
    glow: Float,
) {
    val motes = listOf(
        Triple(0.2f, 0.85f, 0.9f),
        Triple(0.78f, 0.9f, 1.3f),
        Triple(0.5f, 0.95f, 0.7f),
        Triple(0.33f, 0.8f, 1.1f),
        Triple(0.66f, 0.82f, 0.8f),
        Triple(0.86f, 0.88f, 1.0f),
    )
    motes.forEachIndexed { i, (bx, startY, speed) ->
        val local = (time * speed + i * 0.17f) % 1f
        val y = size.height * (startY - local * 0.68f)
        val drift = sin((local + i) * 6.28f) * size.width * 0.018f
        val x = size.width * bx + drift
        val fade = sin(local * 3.14f).coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFFE9C07A).copy(alpha = 0.16f * fade * glow),
            radius = size.minDimension * (0.005f + 0.003f * (i % 3)),
            center = Offset(x, y),
        )
    }
}
