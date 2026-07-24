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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.opencookie.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

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
    val crumbProgress = remember { Animatable(0f) }
    val impactShake = remember { Animatable(0f) }
    val crumbScenarioIndex = remember { mutableStateOf(-1) }
    val crumbScenarioPresets = remember { allCrumbScenarios() }
    val crumbs = crumbScenarioPresets[crumbScenarioIndex.value.coerceAtLeast(0).coerceAtMost(crumbScenarioPresets.lastIndex)]
    val crumbTimelineDurationMs = remember(crumbs) { crumbs.maxOf(CrumbSpec::totalDurationMs) }

    LaunchedEffect(phase) {
        when (phase) {
            CookiePhase.IDLE -> {
                initialBreakProgress.snapTo(0f)
                crumbProgress.snapTo(0f)
                impactShake.snapTo(0f)
            }
            CookiePhase.BREAKING -> {
                val nextScenarioIndex = pickRandomCrumbScenarioIndex(
                    currentIndex = crumbScenarioIndex.value,
                    scenarioCount = crumbScenarioPresets.size,
                )
                crumbScenarioIndex.value = nextScenarioIndex
                val selectedCrumbs = crumbScenarioPresets[nextScenarioIndex]
                val selectedCrumbTimelineDurationMs = selectedCrumbs.maxOf(CrumbSpec::totalDurationMs)
                initialBreakProgress.snapTo(0f)
                crumbProgress.snapTo(0f)
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
                        delay((CrackMomentMs + 18).toLong())
                        crumbProgress.animateTo(
                            targetValue = selectedCrumbTimelineDurationMs.toFloat(),
                            animationSpec = tween(selectedCrumbTimelineDurationMs, easing = LinearEasing),
                        )
                    }
                }
            }
            CookiePhase.WAITING_FOR_TRANSACTION,
            CookiePhase.REVEALED,
            -> {
                if (initialBreakProgress.value < 1f) initialBreakProgress.snapTo(1f)
                if (crumbProgress.value < crumbTimelineDurationMs) crumbProgress.snapTo(crumbTimelineDurationMs.toFloat())
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
    val crumbElapsedMs = crumbProgress.value.coerceIn(0f, crumbTimelineDurationMs.toFloat())
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

    BoxWithConstraints(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = tappable,
            onClick = onTap,
        ),
        contentAlignment = Alignment.Center,
    ) {
        val localDensity = LocalDensity.current
        val stageWidthPx = with(localDensity) { maxWidth.toPx() }
        val stageHeightPx = with(localDensity) { maxHeight.toPx() }
        val stageMin = if (maxWidth < maxHeight) maxWidth else maxHeight

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

        if (broken) {
            crumbs.forEach { crumb ->
                val pose = crumb.poseAt(crumbElapsedMs)
                Image(
                    painter = painterResource(crumb.drawableRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(stageMin * crumb.size)
                        .graphicsLayer {
                            alpha = pose.alpha
                            translationX = stageWidthPx * pose.x
                            translationY = stageHeightPx * pose.y
                            rotationZ = pose.rotation
                            scaleX = pose.scale
                            scaleY = pose.scale
                        },
                )
            }
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
    }
}

private fun allCrumbScenarios(): List<List<CrumbSpec>> =
    listOf(
        balancedCrumbScenario(),
        leftHeavyCrumbScenario(),
        rightHeavyCrumbScenario(),
        compactCenterCrumbScenario(),
        wideAsymmetricCrumbScenario(),
        chunkyCrumbScenario(),
    )

private fun balancedCrumbScenario(): List<CrumbSpec> =
    listOf(
        CrumbSpec(
            drawableRes = R.drawable.crumb_01,
            startX = -0.028f,
            startY = -0.012f,
            launchX = -0.13f,
            launchY = -0.13f,
            touchX = -0.26f,
            touchY = 0.21f,
            size = 0.058f,
            baseScale = 1.18f,
            startRotation = -10f,
            travelRotation = -84f,
            finalRotation = -44f,
            startDelayMs = 0,
            flightDurationMs = 650,
            bounceHeight1 = 0.018f,
            bounceDuration1Ms = 110,
            bounceHeight2 = 0.008f,
            bounceDuration2Ms = 70,
            slideX = -0.018f,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_04,
            startX = -0.05f,
            startY = 0.01f,
            launchX = -0.08f,
            launchY = -0.04f,
            touchX = -0.238f,
            touchY = 0.255f,
            size = 0.043f,
            baseScale = 0.92f,
            startRotation = -20f,
            travelRotation = -56f,
            finalRotation = -32f,
            startDelayMs = 8,
            flightDurationMs = 520,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_10,
            startX = -0.018f,
            startY = 0f,
            launchX = -0.11f,
            launchY = -0.02f,
            touchX = -0.3f,
            touchY = 0.21f,
            size = 0.03f,
            baseScale = 0.74f,
            startRotation = -12f,
            travelRotation = -28f,
            finalRotation = -18f,
            startDelayMs = 24,
            flightDurationMs = 500,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_06,
            startX = -0.008f,
            startY = -0.02f,
            launchX = -0.06f,
            launchY = -0.12f,
            touchX = -0.188f,
            touchY = 0.292f,
            size = 0.036f,
            baseScale = 0.82f,
            startRotation = -18f,
            travelRotation = -102f,
            finalRotation = -74f,
            startDelayMs = 0,
            flightDurationMs = 560,
            bounceHeight1 = 0.012f,
            bounceDuration1Ms = 86,
            slideX = -0.012f,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_12,
            startX = -0.014f,
            startY = -0.004f,
            launchX = -0.034f,
            launchY = -0.07f,
            touchX = -0.12f,
            touchY = 0.3f,
            size = 0.024f,
            baseScale = 0.72f,
            startRotation = -8f,
            travelRotation = -22f,
            finalRotation = -14f,
            startDelayMs = 42,
            flightDurationMs = 540,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_03,
            startX = -0.006f,
            startY = 0.004f,
            launchX = -0.02f,
            launchY = -0.09f,
            touchX = -0.058f,
            touchY = 0.245f,
            size = 0.044f,
            baseScale = 0.98f,
            startRotation = -6f,
            travelRotation = 18f,
            finalRotation = 2f,
            startDelayMs = 30,
            flightDurationMs = 520,
            bounceHeight1 = 0.009f,
            bounceDuration1Ms = 72,
            slideX = -0.006f,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_01,
            startX = -0.032f,
            startY = -0.01f,
            launchX = -0.09f,
            launchY = -0.05f,
            touchX = -0.228f,
            touchY = 0.236f,
            size = 0.032f,
            baseScale = 0.78f,
            startRotation = -8f,
            travelRotation = -34f,
            finalRotation = -20f,
            startDelayMs = 18,
            flightDurationMs = 430,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_02,
            startX = 0.006f,
            startY = -0.008f,
            launchX = 0.14f,
            launchY = -0.05f,
            touchX = 0.25f,
            touchY = 0.22f,
            size = 0.056f,
            baseScale = 1.12f,
            startRotation = 8f,
            travelRotation = 38f,
            finalRotation = 22f,
            startDelayMs = 14,
            flightDurationMs = 600,
            bounceHeight1 = 0.014f,
            bounceDuration1Ms = 96,
            slideX = 0.016f,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_05,
            startX = 0.04f,
            startY = 0.012f,
            launchX = 0.1f,
            launchY = 0.004f,
            touchX = 0.236f,
            touchY = 0.255f,
            size = 0.048f,
            baseScale = 0.98f,
            startRotation = 18f,
            travelRotation = 66f,
            finalRotation = 40f,
            startDelayMs = 26,
            flightDurationMs = 530,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_09,
            startX = 0.03f,
            startY = -0.014f,
            launchX = 0.06f,
            launchY = -0.1f,
            touchX = 0.178f,
            touchY = 0.245f,
            size = 0.032f,
            baseScale = 0.8f,
            startRotation = 22f,
            travelRotation = 120f,
            finalRotation = 78f,
            startDelayMs = 10,
            flightDurationMs = 560,
            bounceHeight1 = 0.012f,
            bounceDuration1Ms = 90,
            bounceHeight2 = 0.005f,
            bounceDuration2Ms = 60,
            slideX = 0.012f,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_07,
            startX = 0.018f,
            startY = 0.03f,
            launchX = 0.085f,
            launchY = -0.02f,
            touchX = 0.198f,
            touchY = 0.302f,
            size = 0.044f,
            baseScale = 0.94f,
            startRotation = 10f,
            travelRotation = 48f,
            finalRotation = 26f,
            startDelayMs = 20,
            flightDurationMs = 580,
            bounceHeight1 = 0.01f,
            bounceDuration1Ms = 78,
            slideX = 0.01f,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_13,
            startX = 0.014f,
            startY = 0f,
            launchX = 0.04f,
            launchY = -0.055f,
            touchX = 0.11f,
            touchY = 0.27f,
            size = 0.026f,
            baseScale = 0.74f,
            startRotation = 12f,
            travelRotation = 28f,
            finalRotation = 14f,
            startDelayMs = 36,
            flightDurationMs = 500,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_11,
            startX = 0.01f,
            startY = 0.018f,
            launchX = 0.028f,
            launchY = -0.034f,
            touchX = 0.048f,
            touchY = 0.275f,
            size = 0.047f,
            baseScale = 1f,
            startRotation = 4f,
            travelRotation = 20f,
            finalRotation = 12f,
            startDelayMs = 50,
            flightDurationMs = 610,
            bounceHeight1 = 0.008f,
            bounceDuration1Ms = 72,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_05,
            startX = 0.002f,
            startY = 0.01f,
            launchX = 0.018f,
            launchY = -0.05f,
            touchX = -0.006f,
            touchY = 0.232f,
            size = 0.042f,
            baseScale = 0.96f,
            startRotation = 10f,
            travelRotation = 34f,
            finalRotation = 18f,
            startDelayMs = 34,
            flightDurationMs = 470,
            bounceHeight1 = 0.007f,
            bounceDuration1Ms = 54,
            slideX = -0.004f,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_02,
            startX = 0.02f,
            startY = -0.01f,
            launchX = 0.15f,
            launchY = -0.02f,
            touchX = 0.29f,
            touchY = 0.2f,
            size = 0.028f,
            baseScale = 0.72f,
            startRotation = 6f,
            travelRotation = 30f,
            finalRotation = 18f,
            startDelayMs = 6,
            flightDurationMs = 460,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_05,
            startX = 0.034f,
            startY = 0.006f,
            launchX = 0.12f,
            launchY = -0.01f,
            touchX = 0.256f,
            touchY = 0.214f,
            size = 0.03f,
            baseScale = 0.76f,
            startRotation = 18f,
            travelRotation = 56f,
            finalRotation = 30f,
            startDelayMs = 0,
            flightDurationMs = 420,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_08,
            startX = 0.002f,
            startY = -0.012f,
            launchX = -0.002f,
            launchY = -0.052f,
            touchX = 0f,
            touchY = 0.31f,
            size = 0.02f,
            baseScale = 0.66f,
            startRotation = -8f,
            travelRotation = 10f,
            finalRotation = 0f,
            startDelayMs = 58,
            flightDurationMs = 520,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_03,
            startX = -0.002f,
            startY = 0.002f,
            launchX = -0.008f,
            launchY = -0.06f,
            touchX = -0.03f,
            touchY = 0.29f,
            size = 0.024f,
            baseScale = 0.7f,
            startRotation = -4f,
            travelRotation = 14f,
            finalRotation = 4f,
            startDelayMs = 46,
            flightDurationMs = 500,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_13,
            startX = 0.006f,
            startY = 0.002f,
            launchX = 0.016f,
            launchY = -0.058f,
            touchX = 0.026f,
            touchY = 0.258f,
            size = 0.034f,
            baseScale = 0.84f,
            startRotation = 8f,
            travelRotation = 24f,
            finalRotation = 12f,
            startDelayMs = 54,
            flightDurationMs = 510,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_08,
            startX = -0.006f,
            startY = -0.006f,
            launchX = -0.004f,
            launchY = -0.044f,
            touchX = -0.01f,
            touchY = 0.24f,
            size = 0.018f,
            baseScale = 0.6f,
            startRotation = -8f,
            travelRotation = 6f,
            finalRotation = -2f,
            startDelayMs = 32,
            flightDurationMs = 430,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_12,
            startX = 0.004f,
            startY = -0.004f,
            launchX = 0.01f,
            launchY = -0.046f,
            touchX = 0.01f,
            touchY = 0.25f,
            size = 0.019f,
            baseScale = 0.62f,
            startRotation = 6f,
            travelRotation = 16f,
            finalRotation = 8f,
            startDelayMs = 40,
            flightDurationMs = 450,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_07,
            startX = -0.01f,
            startY = 0.026f,
            launchX = -0.07f,
            launchY = -0.03f,
            touchX = -0.172f,
            touchY = 0.244f,
            size = 0.041f,
            baseScale = 0.9f,
            startRotation = 10f,
            travelRotation = -22f,
            finalRotation = -12f,
            startDelayMs = 12,
            flightDurationMs = 480,
        ),
        CrumbSpec(
            drawableRes = R.drawable.crumb_09,
            startX = 0.022f,
            startY = -0.012f,
            launchX = 0.078f,
            launchY = -0.07f,
            touchX = 0.218f,
            touchY = 0.236f,
            size = 0.034f,
            baseScale = 0.82f,
            startRotation = 20f,
            travelRotation = 88f,
            finalRotation = 54f,
            startDelayMs = 16,
            flightDurationMs = 520,
            bounceHeight1 = 0.008f,
            bounceDuration1Ms = 70,
            slideX = 0.01f,
        ),
    )

private fun leftHeavyCrumbScenario(): List<CrumbSpec> {
    val base = balancedCrumbScenario()
    return base.mapIndexed { index, crumb ->
        val leftSide = crumb.touchX <= 0f
        val yShift = when (index % 4) {
            0 -> -0.012f
            1 -> 0.006f
            2 -> 0.016f
            else -> 0f
        }
        when (index) {
            0 -> crumb.copy(touchX = -0.31f, touchY = 0.206f, size = 0.064f, baseScale = 1.28f)
            3 -> crumb.copy(touchX = -0.224f, touchY = 0.284f, size = 0.042f, baseScale = 0.9f)
            7 -> crumb.copy(touchX = 0.188f, touchY = 0.236f, size = 0.048f, baseScale = 0.98f)
            12 -> crumb.copy(touchX = 0.026f, touchY = 0.262f, size = 0.042f, baseScale = 0.92f)
            else -> crumb.copy(
                startX = crumb.startX - 0.008f,
                launchX = crumb.launchX * if (leftSide) 1.16f else 0.74f,
                touchX = if (leftSide) crumb.touchX * 1.18f - 0.024f else crumb.touchX * 0.84f - 0.012f,
                touchY = (crumb.touchY + yShift).coerceIn(0.2f, 0.33f),
                size = crumb.size * if (leftSide && index % 2 == 0) 1.08f else 0.94f,
                baseScale = crumb.baseScale * if (leftSide) 1.06f else 0.9f,
                flightDurationMs = (crumb.flightDurationMs * if (leftSide) 0.9f else 0.82f).toInt(),
                bounceHeight1 = crumb.bounceHeight1 * if (leftSide) 0.95f else 0.7f,
                bounceHeight2 = crumb.bounceHeight2 * 0.7f,
            )
        }
    }
}

private fun rightHeavyCrumbScenario(): List<CrumbSpec> {
    val base = balancedCrumbScenario()
    return base.mapIndexed { index, crumb ->
        val rightSide = crumb.touchX >= 0f
        val yShift = when (index % 5) {
            0 -> -0.01f
            1 -> 0.012f
            2 -> 0f
            3 -> 0.018f
            else -> -0.004f
        }
        when (index) {
            8 -> crumb.copy(touchX = 0.284f, touchY = 0.214f, size = 0.06f, baseScale = 1.2f)
            9 -> crumb.copy(touchX = 0.214f, touchY = 0.238f, size = 0.04f, baseScale = 0.92f)
            10 -> crumb.copy(touchX = 0.238f, touchY = 0.286f, size = 0.05f, baseScale = 1.06f)
            16 -> crumb.copy(touchX = -0.018f, touchY = 0.274f, size = 0.028f, baseScale = 0.8f)
            else -> crumb.copy(
                startX = crumb.startX + 0.008f,
                launchX = crumb.launchX * if (rightSide) 1.18f else 0.76f,
                touchX = if (rightSide) crumb.touchX * 1.2f + 0.024f else crumb.touchX * 0.82f + 0.012f,
                touchY = (crumb.touchY + yShift).coerceIn(0.2f, 0.33f),
                size = crumb.size * if (rightSide && index % 2 == 1) 1.08f else 0.94f,
                baseScale = crumb.baseScale * if (rightSide) 1.08f else 0.9f,
                flightDurationMs = (crumb.flightDurationMs * if (rightSide) 0.92f else 0.82f).toInt(),
                bounceHeight1 = crumb.bounceHeight1 * if (rightSide) 0.96f else 0.68f,
                bounceHeight2 = crumb.bounceHeight2 * 0.7f,
            )
        }
    }
}

private fun compactCenterCrumbScenario(): List<CrumbSpec> {
    val base = balancedCrumbScenario()
    val chunkyCenterIndices = setOf(5, 12, 13, 16, 17, 18)
    return base.mapIndexed { index, crumb ->
        val horizontalOffset = when (index % 5) {
            0 -> -0.032f
            1 -> 0.024f
            2 -> -0.012f
            3 -> 0.014f
            else -> 0f
        }
        val verticalOffset = when (index % 4) {
            0 -> -0.012f
            1 -> 0.004f
            2 -> 0.016f
            else -> -0.002f
        }
        crumb.copy(
            startX = crumb.startX * 0.72f,
            launchX = crumb.launchX * 0.74f,
            touchX = crumb.touchX * 0.48f + horizontalOffset,
            touchY = (crumb.touchY * 0.9f + 0.03f + verticalOffset).coerceIn(0.21f, 0.32f),
            size = crumb.size * if (index in chunkyCenterIndices) 1.2f else 0.92f,
            baseScale = crumb.baseScale * if (index in chunkyCenterIndices) 1.12f else 0.95f,
            flightDurationMs = (crumb.flightDurationMs * 0.82f).toInt(),
            bounceHeight1 = crumb.bounceHeight1 * 0.72f,
            bounceHeight2 = crumb.bounceHeight2 * 0.48f,
        )
    }
}

private fun wideAsymmetricCrumbScenario(): List<CrumbSpec> {
    val base = balancedCrumbScenario()
    return base.mapIndexed { index, crumb ->
        val toRight = crumb.touchX > 0f
        val spread = if (toRight) 1.42f else 1.18f
        val horizontalBias = when (index % 3) {
            0 -> if (toRight) 0.026f else -0.014f
            1 -> if (toRight) 0.014f else -0.026f
            else -> if (toRight) 0.008f else -0.01f
        }
        val verticalOffset = when (index % 4) {
            0 -> 0.018f
            1 -> -0.008f
            2 -> 0.012f
            else -> 0f
        }
        crumb.copy(
            startX = crumb.startX + if (toRight) 0.004f else -0.004f,
            launchX = crumb.launchX * if (toRight) 1.14f else 1.02f,
            touchX = crumb.touchX * spread + horizontalBias,
            touchY = (crumb.touchY + verticalOffset).coerceIn(0.19f, 0.34f),
            size = crumb.size * if (index % 5 == 0) 1.1f else 0.96f,
            baseScale = crumb.baseScale * if (index % 5 == 0) 1.08f else 0.96f,
            flightDurationMs = (crumb.flightDurationMs * if (toRight) 0.9f else 0.86f).toInt(),
            bounceHeight1 = crumb.bounceHeight1 * 0.78f,
            bounceHeight2 = crumb.bounceHeight2 * 0.54f,
        )
    }
}

private fun chunkyCrumbScenario(): List<CrumbSpec> {
    val base = balancedCrumbScenario()
    val bigIndices = setOf(0, 5, 8, 10, 12, 13, 20)
    val mediumIndices = setOf(1, 3, 6, 9, 15, 21)
    return base.mapIndexed { index, crumb ->
        val verticalOffset = when {
            index in bigIndices -> -0.012f
            index in mediumIndices -> 0.006f
            else -> 0.014f
        }
        crumb.copy(
            touchX = crumb.touchX * if (index in bigIndices) 1.04f else 0.96f,
            touchY = (crumb.touchY + verticalOffset).coerceIn(0.2f, 0.33f),
            size = when {
                index in bigIndices -> crumb.size * 1.28f
                index in mediumIndices -> crumb.size * 1.12f
                else -> crumb.size * 0.82f
            },
            baseScale = when {
                index in bigIndices -> crumb.baseScale * 1.16f
                index in mediumIndices -> crumb.baseScale * 1.06f
                else -> crumb.baseScale * 0.84f
            },
            flightDurationMs = (crumb.flightDurationMs * if (index in bigIndices) 0.88f else 0.8f).toInt(),
            bounceHeight1 = crumb.bounceHeight1 * if (index in bigIndices) 0.84f else 0.58f,
            bounceHeight2 = crumb.bounceHeight2 * 0.44f,
        )
    }
}

private fun pickRandomCrumbScenarioIndex(
    currentIndex: Int,
    scenarioCount: Int,
): Int {
    if (scenarioCount <= 1) return 0
    if (currentIndex !in 0 until scenarioCount) return Random.nextInt(scenarioCount)
    val next = Random.nextInt(scenarioCount - 1)
    return if (next >= currentIndex) next + 1 else next
}

private data class CrumbSpec(
    val drawableRes: Int,
    val startX: Float,
    val startY: Float,
    val launchX: Float,
    val launchY: Float,
    val touchX: Float,
    val touchY: Float,
    val size: Float,
    val baseScale: Float,
    val startRotation: Float,
    val travelRotation: Float,
    val finalRotation: Float,
    val startDelayMs: Int,
    val flightDurationMs: Int,
    val bounceHeight1: Float = 0f,
    val bounceDuration1Ms: Int = 0,
    val bounceHeight2: Float = 0f,
    val bounceDuration2Ms: Int = 0,
    val slideX: Float = 0f,
) {
    private val settledFlightDurationMs: Float
        get() = (flightDurationMs * 0.86f).coerceAtLeast(380f)

    private val settledBounceDuration1Ms: Float
        get() = if (bounceDuration1Ms == 0) 0f else (bounceDuration1Ms * 0.72f).coerceAtLeast(40f)

    private val settledBounceDuration2Ms: Float
        get() = if (bounceDuration2Ms == 0) 0f else (bounceDuration2Ms * 0.58f).coerceAtLeast(28f)

    private val settledBounceHeight1: Float
        get() = bounceHeight1 * 0.68f

    private val settledBounceHeight2: Float
        get() = bounceHeight2 * 0.42f

    val totalDurationMs: Int
        get() = startDelayMs + settledFlightDurationMs.toInt() + settledBounceDuration1Ms.toInt() + settledBounceDuration2Ms.toInt()

    fun poseAt(elapsedMs: Float): CrumbPose {
        if (elapsedMs <= startDelayMs) {
            return CrumbPose(
                x = startX,
                y = startY,
                rotation = startRotation,
                scale = baseScale * 0.9f,
                alpha = 0f,
            )
        }

        val localMs = elapsedMs - startDelayMs
        val flightMs = settledFlightDurationMs
        if (localMs < flightMs) {
            val progress = (localMs / flightMs).coerceIn(0f, 1f)
            val alphaInMs = flightMs.coerceAtMost(90f)
            return CrumbPose(
                x = ballisticAxis(startX, launchX, touchX, progress),
                y = ballisticAxis(startY, launchY, touchY, progress),
                rotation = lerp(startRotation, travelRotation, easeOutCubic(progress)),
                scale = lerp(baseScale * 0.9f, baseScale, easeOutCubic((localMs / alphaInMs).coerceIn(0f, 1f))),
                alpha = easeOutCubic((localMs / alphaInMs).coerceIn(0f, 1f)),
            )
        }

        val firstBounceEndX = touchX + if (bounceDuration2Ms > 0) slideX * 0.62f else slideX
        val remainingAfterFlight = localMs - flightMs
        if (settledBounceDuration1Ms > 0f && remainingAfterFlight < settledBounceDuration1Ms) {
            val progress = (remainingAfterFlight / settledBounceDuration1Ms).coerceIn(0f, 1f)
            return CrumbPose(
                x = lerp(touchX, firstBounceEndX, easeOutCubic(progress)),
                y = touchY + bounceArc(progress, settledBounceHeight1),
                rotation = lerp(travelRotation, finalRotation, easeOutCubic(progress)),
                scale = baseScale,
                alpha = 1f,
            )
        }

        val secondBounceStartX = firstBounceEndX
        val finalX = touchX + slideX
        val remainingAfterFirstBounce = remainingAfterFlight - settledBounceDuration1Ms
        if (settledBounceDuration2Ms > 0f && remainingAfterFirstBounce < settledBounceDuration2Ms) {
            val progress = (remainingAfterFirstBounce / settledBounceDuration2Ms).coerceIn(0f, 1f)
            return CrumbPose(
                x = lerp(secondBounceStartX, finalX, easeOutCubic(progress)),
                y = touchY + bounceArc(progress, settledBounceHeight2),
                rotation = lerp(
                    lerp(travelRotation, finalRotation, 0.78f),
                    finalRotation,
                    easeOutCubic(progress),
                ),
                scale = baseScale,
                alpha = 1f,
            )
        }

        return CrumbPose(
            x = finalX,
            y = touchY,
            rotation = finalRotation,
            scale = baseScale,
            alpha = 1f,
        )
    }
}

private data class CrumbPose(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scale: Float,
    val alpha: Float,
)

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

private fun ballisticAxis(start: Float, launch: Float, end: Float, progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return start + launch * t + (end - start - launch) * t * t
}

private fun bounceArc(progress: Float, height: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return -height * 4f * t * (1f - t)
}
