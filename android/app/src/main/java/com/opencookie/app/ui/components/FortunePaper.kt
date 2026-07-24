package com.opencookie.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencookie.app.R
import com.opencookie.app.ui.theme.PaperInk
import kotlin.math.roundToInt

private const val PaperSourceLeft = 91
private const val PaperSourceTop = 585
private const val PaperSourceWidth = 848
private const val PaperSourceHeight = 338
private const val PaperVisibleAspect = PaperSourceWidth.toFloat() / PaperSourceHeight.toFloat()
private const val TextRevealFeatherFraction = 0.14f

private val FortuneMessageFont = FontFamily(
    Font(
        resId = R.font.lora_italic_wght,
        weight = FontWeight.SemiBold,
        style = FontStyle.Italic,
    ),
)

@Composable
fun FortunePaper(
    message: String?,
    modifier: Modifier = Modifier,
    textRevealProgress: Float = 1f,
) {
    val paperImage = ImageBitmap.imageResource(R.drawable.fortune_paper)

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val fullPaperWidth = maxWidth * 0.88f
        val fullPaperHeight = fullPaperWidth / PaperVisibleAspect

        val content = message?.trim().orEmpty()
        val compactText = content.length > 78 || maxWidth < 320.dp
        val mediumText = content.length > 58 || maxWidth < 380.dp
        val fontSize = when {
            compactText -> 13.sp
            mediumText -> 15.sp
            else -> 17.sp
        }
        val lineHeight = when {
            compactText -> 17.sp
            mediumText -> 20.sp
            else -> 22.sp
        }
        val maxLines = if (content.length > 68) 4 else 3

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(fullPaperWidth)
                .height(fullPaperHeight),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(fullPaperWidth)
                    .height(fullPaperHeight),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawImage(
                        image = paperImage,
                        srcOffset = IntOffset(PaperSourceLeft, PaperSourceTop),
                        srcSize = IntSize(PaperSourceWidth, PaperSourceHeight),
                        dstSize = IntSize(
                            width = size.width.roundToInt().coerceAtLeast(1),
                            height = size.height.roundToInt().coerceAtLeast(1),
                        ),
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.64f)
                        .fillMaxHeight(0.74f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (content.isNotEmpty() && textRevealProgress > 0.001f) {
                        val reveal = textRevealProgress.coerceIn(0f, 1f)
                        val revealAlpha = when {
                            reveal <= 0.04f -> 0f
                            else -> 0.18f + 0.82f * easeOutCubic((reveal - 0.04f) / 0.96f)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = revealAlpha
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithCache {
                                    val featherWidth = size.width * TextRevealFeatherFraction
                                    onDrawWithContent {
                                        drawContent()

                                        if (reveal >= 1f) return@onDrawWithContent

                                        val revealEdge = size.width * reveal
                                        val solidStop = ((revealEdge - featherWidth) / size.width).coerceIn(0f, 1f)
                                        val featherStop = (revealEdge / size.width).coerceIn(0f, 1f)
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colorStops = arrayOf(
                                                    0f to Color.Black,
                                                    solidStop to Color.Black,
                                                    featherStop to Color.Transparent,
                                                    1f to Color.Transparent,
                                                ),
                                            ),
                                            blendMode = BlendMode.DstIn,
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = content,
                                textAlign = TextAlign.Center,
                                maxLines = maxLines,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    fontFamily = FortuneMessageFont,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = fontSize,
                                    lineHeight = lineHeight,
                                    color = PaperInk,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun easeOutCubic(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    val p = 1f - t
    return 1f - p * p * p
}
