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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.opencookie.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class CookiePhase { IDLE, BREAKING, WAITING_FOR_TRANSACTION, REVEALED }

private const val CrackMomentMs = 720
private const val HalfTakeoverOffsetX = 0.01f
private const val RevealedFinalHalfOffsetX = 0.172f
private const val RevealedOpenY = 0.048f
private const val RevealedHalfRotation = 8.4f
private const val PaperHiddenScale = 0.39f
private const val PaperFocusedScale = 1.02f
private const val PaperHiddenOffsetY = 0.03f
private const val PaperFocusedOffsetY = -0.14f
private const val PaperArcPeakOffsetY = -0.24f
private const val PaperFrontLayerThreshold = 0.58f

@Composable
fun FortuneCookieStage(
    phase: CookiePhase,
    tappable: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    paperFocusProgress: Float = 0f,
    paper: @Composable (Modifier) -> Unit = {},
) {
    val idle = rememberInfiniteTransition(label = "cookie_idle")
    val idleScale by idle.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "idle_scale",
    )
    val idleRotation by idle.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(4600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "idle_rotation",
    )
    val idleFloat by idle.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(3800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "idle_float",
    )
    val ambientTime by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart),
        label = "ambient_time",
    )
    val glowPulse by idle.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_pulse",
    )
    val brokenMotion = rememberInfiniteTransition(label = "cookie_broken_motion")
    val settleBob by brokenMotion.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "settle_bob",
    )
    val settleTilt by brokenMotion.animateFloat(
        initialValue = -0.65f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "settle_tilt",
    )
    val paperDrift by brokenMotion.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "paper_drift",
    )

    val initialBreakProgress = remember { Animatable(0f) }
    val burstProgress = remember { Animatable(0f) }
    val fallProgress = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }
    val impactShake = remember { Animatable(0f) }

    LaunchedEffect(phase) {
        when (phase) {
            CookiePhase.IDLE -> {
                initialBreakProgress.snapTo(0f)
                burstProgress.snapTo(0f)
                fallProgress.snapTo(0f)
                particleProgress.snapTo(0f)
                impactShake.snapTo(0f)
            }
            CookiePhase.BREAKING -> {
                initialBreakProgress.snapTo(0f)
                burstProgress.snapTo(0f)
                fallProgress.snapTo(0f)
                particleProgress.snapTo(0f)
                impactShake.snapTo(0f)

                coroutineScope {
                    launch {
                        impactShake.animateTo(
                            targetValue = 0f,
                            animationSpec = keyframes {
                                durationMillis = 880
                                0f at 0
                                0.1f at 70
                                -0.08f at 180
                                0.14f at 310
                                -0.12f at 450
                                0.18f at 580
                                -0.14f at 660
                                0.72f at CrackMomentMs
                                -0.18f at 800
                                0f at 880
                            },
                        )
                    }
                    launch {
                        initialBreakProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = keyframes {
                                durationMillis = 1180
                                0f at 0
                                0.06f at 80
                                0.14f at 220
                                0.2f at 380
                                0.26f at 540
                                0.34f at 660
                                0.36f at CrackMomentMs
                                0.58f at 830
                                0.76f at 960
                                1f at 1180
                            },
                        )
                    }
                    launch {
                        delay(CrackMomentMs.toLong())
                        burstProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(520, easing = FastOutSlowInEasing),
                        )
                    }
                    launch {
                        delay((CrackMomentMs + 70).toLong())
                        fallProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(1140, easing = LinearEasing),
                        )
                    }
                    launch {
                        delay((CrackMomentMs + 20).toLong())
                        particleProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(900, easing = LinearEasing),
                        )
                    }
                }
            }
            CookiePhase.WAITING_FOR_TRANSACTION,
            CookiePhase.REVEALED,
            -> {
                if (initialBreakProgress.value < 1f) initialBreakProgress.snapTo(1f)
                if (burstProgress.value < 1f) burstProgress.snapTo(1f)
                if (fallProgress.value < 1f) fallProgress.snapTo(1f)
                if (particleProgress.value < 1f) particleProgress.snapTo(1f)
                impactShake.snapTo(0f)
            }
        }
    }

    val revealedOpenProgress by animateFloatAsState(
        targetValue = if (phase == CookiePhase.REVEALED) 1f else 0f,
        animationSpec = tween(760, easing = FastOutSlowInEasing),
        label = "revealed_open_progress",
    )

    val initialBreakCurve = initialBreakProgress.value.coerceIn(0f, 1f)
    val pressCurve = when {
        phase != CookiePhase.BREAKING -> 0f
        initialBreakCurve < 0.18f -> easeOutCubic(initialBreakCurve / 0.18f)
        initialBreakCurve < 0.34f -> 1f - easeOutCubic((initialBreakCurve - 0.18f) / 0.16f)
        else -> 0f
    }
    val intactAlpha = when {
        phase == CookiePhase.IDLE -> 1f
        phase == CookiePhase.BREAKING -> (1f - ((initialBreakCurve - 0.34f) / 0.14f)).coerceIn(0f, 1f)
        else -> 0f
    }
    val brokenClosedAlpha = when (phase) {
        CookiePhase.IDLE -> 0f
        CookiePhase.BREAKING -> ((initialBreakCurve - 0.36f) / 0.16f).coerceIn(0f, 1f)
        CookiePhase.WAITING_FOR_TRANSACTION -> 1f
        CookiePhase.REVEALED -> (1f - revealedOpenProgress / 0.24f).coerceIn(0f, 1f)
    }
    val separatedHalvesAlpha = if (phase == CookiePhase.REVEALED) {
        ((revealedOpenProgress - 0.02f) / 0.16f).coerceIn(0f, 1f)
    } else {
        0f
    }
    val burstCurve = burstProgress.value.coerceIn(0f, 1f)
    val fallCurve = fallProgress.value.coerceIn(0f, 1f)
    val particleCurve = particleProgress.value.coerceIn(0f, 1f)
    val broken = phase != CookiePhase.IDLE
    val calmMotion = when (phase) {
        CookiePhase.BREAKING -> 0f
        CookiePhase.WAITING_FOR_TRANSACTION -> 0.5f
        CookiePhase.REVEALED -> 0.28f
        CookiePhase.IDLE -> 0f
    }
    val paperInScene = phase == CookiePhase.WAITING_FOR_TRANSACTION || phase == CookiePhase.REVEALED
    val focusedPaper = paperFocusProgress.coerceIn(0f, 1f)
    val paperScale = lerp(PaperHiddenScale, PaperFocusedScale, easeOutCubic(focusedPaper))
    val paperOffsetY = if (focusedPaper < PaperFrontLayerThreshold) {
        val liftProgress = easeOutCubic(focusedPaper / PaperFrontLayerThreshold)
        lerp(PaperHiddenOffsetY, PaperArcPeakOffsetY, liftProgress)
    } else {
        val settleProgress = easeInOutCubic(
            (focusedPaper - PaperFrontLayerThreshold) / (1f - PaperFrontLayerThreshold),
        )
        lerp(PaperArcPeakOffsetY, PaperFocusedOffsetY, settleProgress)
    }
    val paperShouldBeInFront = phase == CookiePhase.REVEALED && focusedPaper >= PaperFrontLayerThreshold

    Box(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = tappable,
            onClick = onTap,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAmbientParticles(ambientTime, glow = if (phase == CookiePhase.IDLE) 1f else 0.45f)
            drawStageGlow(
                idleGlow = glowPulse,
                breakCurve = initialBreakCurve,
                revealedFocus = revealedOpenProgress,
            )
        }

        if (paperInScene && !paperShouldBeInFront) {
            paper(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = size.height * paperOffsetY + paperDrift * density * 0.18f * calmMotion
                        scaleX = paperScale
                        scaleY = paperScale
                    },
            )
        }

        if (phase == CookiePhase.IDLE || intactAlpha > 0.01f) {
            Image(
                painter = painterResource(R.drawable.intact_cookie),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pressScaleX = 1f + 0.012f * pressCurve
                        val pressScaleY = 1f - 0.035f * pressCurve
                        alpha = intactAlpha
                        scaleX = idleScale * pressScaleX
                        scaleY = idleScale * pressScaleY
                        rotationZ = idleRotation + impactShake.value * 1.05f
                        translationX = impactShake.value * 2.2f * density
                        translationY = idleFloat * density + pressCurve * 3.5f * density
                    },
            )
        }

        if (broken && separatedHalvesAlpha > 0.01f) {
            val openAmount = easeInOutCubic(revealedOpenProgress)
            val halfOffsetX = lerp(HalfTakeoverOffsetX, RevealedFinalHalfOffsetX, openAmount)
            val separationY = RevealedOpenY * openAmount
            val halfRotation = RevealedHalfRotation * openAmount
            Image(
                painter = painterResource(R.drawable.cookie_left_half),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = separatedHalvesAlpha
                        translationX =
                            size.width * -halfOffsetX +
                                impactShake.value * 4f * density +
                                settleTilt * density * 1.5f * calmMotion
                        translationY = size.height * separationY + settleBob * density * 0.18f * calmMotion
                        rotationZ = -halfRotation + settleTilt * 0.8f * calmMotion
                    },
            )

            Image(
                painter = painterResource(R.drawable.cookie_right_half),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = separatedHalvesAlpha
                        translationX =
                            size.width * halfOffsetX +
                                impactShake.value * 4f * density -
                                settleTilt * density * 1.5f * calmMotion
                        translationY = size.height * separationY - settleBob * density * 0.18f * calmMotion
                        rotationZ = halfRotation - settleTilt * 0.8f * calmMotion
                    },
            )
        }

        if (broken && brokenClosedAlpha > 0.01f) {
            Image(
                painter = painterResource(R.drawable.cookie_broken_closed),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pressScaleX = 1f + 0.012f * pressCurve
                        val pressScaleY = 1f - 0.035f * pressCurve
                        val closedSettle = easeOutCubic(((initialBreakCurve - 0.42f) / 0.58f).coerceIn(0f, 1f))
                        val bridgeScale = if (phase == CookiePhase.BREAKING) lerp(idleScale, 1f, closedSettle) else 1f
                        val bridgeRotation = if (phase == CookiePhase.BREAKING) lerp(idleRotation, 0f, closedSettle) else 0f
                        val bridgeFloat = if (phase == CookiePhase.BREAKING) lerp(idleFloat, 0f, closedSettle) else 0f
                        alpha = brokenClosedAlpha
                        scaleX = bridgeScale * pressScaleX
                        scaleY = bridgeScale * pressScaleY
                        rotationZ = bridgeRotation + impactShake.value * 1.05f + settleTilt * 0.28f * calmMotion
                        translationX = impactShake.value * 2.2f * density
                        translationY =
                            bridgeFloat * density +
                                pressCurve * 3.5f * density +
                                settleBob * density * 0.12f * calmMotion
                    },
            )
        }

        if (paperInScene && paperShouldBeInFront) {
            paper(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = size.height * paperOffsetY + paperDrift * density * 0.18f * calmMotion
                        scaleX = paperScale
                        scaleY = paperScale
                    },
            )
        }

        if (broken) {
            val burstAppear = ((burstCurve - 0.08f) / 0.18f).coerceIn(0f, 1f)
            val burstFade = ((burstCurve - 0.56f) / 0.44f).coerceIn(0f, 1f)
            val burstAlpha = burstAppear * (1f - burstFade)
            Image(
                painter = painterResource(R.drawable.crumb_burst),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = burstAlpha
                        translationY = size.height * (0.008f + 0.045f * easeOutCubic(burstCurve))
                        rotationZ = -2.2f + burstCurve * 2.4f
                    },
            )

            val fallAppear = ((fallCurve - 0.08f) / 0.18f).coerceIn(0f, 1f)
            val activeFallFade = ((fallCurve - 0.64f) / 0.24f).coerceIn(0f, 1f)
            val settledCrumbs = ((fallCurve - 0.72f) / 0.28f).coerceIn(0f, 1f)
            val fallAlpha = fallAppear * (1f - activeFallFade) + settledCrumbs * 0.42f
            Image(
                painter = painterResource(R.drawable.crumb_fall),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = fallAlpha.coerceIn(0f, 1f)
                        translationY = size.height * (-0.026f + 0.145f * easeInOutCubic(fallCurve))
                        translationX = size.width * 0.006f * sin(fallCurve * 4.8f)
                        rotationZ = -1.2f + fallCurve * 3.4f
                    },
            )

            val particleAppear = ((particleCurve - 0.02f) / 0.12f).coerceIn(0f, 1f)
            val particleFade = ((particleCurve - 0.56f) / 0.44f).coerceIn(0f, 1f)
            Image(
                painter = painterResource(R.drawable.crumb_particles),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = particleAppear * (1f - particleFade)
                        translationY = size.height * (-0.018f + 0.08f * easeOutCubic(particleCurve))
                        translationX = size.width * 0.008f * sin(particleCurve * 6.4f + 0.6f)
                    },
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStageGlow(
    idleGlow: Float,
    breakCurve: Float,
    revealedFocus: Float,
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val base = size.minDimension
    val underRadius = base * (0.34f + 0.07f * idleGlow - 0.05f * revealedFocus)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x44E9A24E), Color(0x00E9A24E)),
            center = Offset(cx, cy + base * 0.22f),
            radius = underRadius,
        ),
        radius = underRadius,
        center = Offset(cx, cy + base * 0.22f),
    )

    if (breakCurve > 0.05f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF3D7).copy(alpha = 0.55f * breakCurve),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = base * 0.2f,
            ),
            radius = base * 0.2f,
            center = Offset(cx, cy),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAmbientParticles(
    time: Float,
    glow: Float,
) {
    val motes = listOf(
        Triple(0.24f, 0.82f, 0.9f),
        Triple(0.76f, 0.88f, 1.28f),
        Triple(0.5f, 0.94f, 0.7f),
        Triple(0.36f, 0.8f, 1.04f),
        Triple(0.64f, 0.78f, 0.82f),
    )
    motes.forEachIndexed { index, (baseX, startY, speed) ->
        val local = (time * speed + index * 0.17f) % 1f
        val y = size.height * (startY - local * 0.62f)
        val x = size.width * baseX + sin((local + index) * 6.28f) * size.width * 0.016f
        val fade = sin(local * 3.14f).coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFFE7C17F).copy(alpha = 0.12f * fade * glow),
            radius = size.minDimension * (0.004f + 0.0026f * (index % 3)),
            center = Offset(x, y),
        )
    }
}

private fun easeOutCubic(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    val p = 1f - t
    return 1f - p * p * p
}

private fun easeInOutCubic(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return if (t < 0.5f) {
        4f * t * t * t
    } else {
        val p = -2f * t + 2f
        1f - (p * p * p) / 2f
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)
