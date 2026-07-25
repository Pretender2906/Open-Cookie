package com.opencookie.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.opencookie.app.R
import kotlinx.coroutines.delay

private const val HintCycles = 3
private const val HintCycleDurationMs = 1770
private const val HintPauseDurationMs = 360L
private const val HintFadeInMs = 330
private const val HintRestingFadeMs = 630
private const val HintRestingAlpha = 0.34f

/** Lower on screen — hand rests clearly below the cookie before and after each tap hint. */
private const val RestTranslationY = 0.36f

/** Raised toward the cookie; kept below the old top so the fingertip stays on the cookie. */
private const val TapTranslationY = 0.11f

private const val MoveUpEnd = 0.38f
private const val PressEnd = 0.54f
private const val MoveDownEnd = 0.80f

/**
 * Tap hint: rest down → move up → press on cookie → return down → rest.
 */
@Composable
fun TapHintHand(modifier: Modifier = Modifier) {
    val cycle = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        cycle.snapTo(0f)
        alpha.snapTo(0f)
        alpha.animateTo(1f, animationSpec = tween(durationMillis = HintFadeInMs))

        repeat(HintCycles) { index ->
            cycle.snapTo(0f)
            cycle.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = HintCycleDurationMs, easing = LinearEasing),
            )
            if (index < HintCycles - 1) delay(HintPauseDurationMs)
        }

        cycle.snapTo(0f)
        alpha.animateTo(HintRestingAlpha, animationSpec = tween(durationMillis = HintRestingFadeMs))
    }

    val progress = cycle.value
    val travelProgress = when {
        progress < MoveUpEnd -> {
            val t = easeOutCubic(progress / MoveUpEnd)
            lerp(1f, 0f, t)
        }
        progress < PressEnd -> 0f
        progress < MoveDownEnd -> {
            val t = easeOutCubic((progress - PressEnd) / (MoveDownEnd - PressEnd))
            lerp(0f, 1f, t)
        }
        else -> 1f
    }
    val pressScale = when {
        progress < MoveUpEnd -> 1f
        progress < MoveUpEnd + 0.06f -> {
            val t = (progress - MoveUpEnd) / 0.06f
            lerp(1f, 0.92f, easeOutCubic(t))
        }
        progress < PressEnd -> {
            val t = (progress - (MoveUpEnd + 0.06f)) / (PressEnd - (MoveUpEnd + 0.06f))
            lerp(0.92f, 1f, easeOutCubic(t))
        }
        else -> 1f
    }

    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.hand_v4),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha.value
                    translationY = size.height * lerp(TapTranslationY, RestTranslationY, travelProgress)
                    scaleX = pressScale
                    scaleY = pressScale
                },
        )
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

private fun easeOutCubic(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    val p = 1f - t
    return 1f - p * p * p
}
