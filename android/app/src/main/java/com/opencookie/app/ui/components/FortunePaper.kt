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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
private const val PaperTextAreaWidthFraction = 0.66f // Slightly wider
/** Matches the visible cream band on the paper crop, not the full strip height. */
private const val PaperTextAreaHeightFraction = 0.65f // Slightly taller for 4-line messages
/** Positions the text block on the paper; cream band sits below geometric center. */
private const val PaperTextAreaVerticalBias3Lines = 0.28f
private const val PaperTextAreaVerticalBias4Lines = 0.16f
private const val LineHeightSafety3Lines = 0.95f
private const val LineHeightSafety4Lines = 0.88f // More strict for 4 lines
private const val TextRevealFeatherFraction = 0.14f

private val FortuneMessageFont = FontFamily(
    Font(R.font.kalam_regular, FontWeight.Normal),
    Font(R.font.kalam_bold, FontWeight.Bold),
)

private val FortuneMessageFontCyrillic = FontFamily(
    Font(R.font.lora_italic_wght, FontWeight.Normal),
    Font(R.font.lora_italic_wght, FontWeight.Medium),
    Font(R.font.lora_italic_wght, FontWeight.SemiBold),
    Font(R.font.lora_italic_wght, FontWeight.Bold),
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
        val isCyrillic = content.any { it in '\u0400'..'\u04FF' }
        val maxLines = if (content.length > 68) 4 else 3
        val textPaddingTop = 4.dp
        val textPaddingBottom = 4.dp
        val textAreaHeight = fullPaperHeight * PaperTextAreaHeightFraction - textPaddingTop - textPaddingBottom
        val lineHeightSafety = if (maxLines == 4) LineHeightSafety4Lines else LineHeightSafety3Lines
        val maxLineHeightDp = textAreaHeight / maxLines * lineHeightSafety
        val paperTextAreaAlignment = BiasAlignment(
            horizontalBias = 0f,
            verticalBias = if (maxLines == 4) {
                PaperTextAreaVerticalBias4Lines
            } else {
                PaperTextAreaVerticalBias3Lines
            },
        )

        val compactText = maxLines == 4 || content.length > 78 || maxWidth < 320.dp
        val mediumText = !compactText && (content.length > 58 || maxWidth < 380.dp)
        val baseFontSize = when {
            compactText -> 15.sp
            mediumText -> 17.sp
            else -> 19.sp
        }
        val baseLineHeight = when {
            compactText -> 18.sp
            mediumText -> 21.sp
            else -> 23.sp
        }

        // Apply scale factor BEFORE calculating constraints to avoid clipping
        // Lora is a refined serif, works well with moderate scaling and weight
        val cyrillicScale = if (isCyrillic) 1.1f else 1.0f
        val scaledFontSize = baseFontSize * cyrillicScale
        val scaledLineHeight = baseLineHeight * (if (isCyrillic) 1.08f else 1.0f)

        val lineHeight = minOf(scaledLineHeight.value, maxLineHeightDp.value).sp
        val finalScale = lineHeight.value / scaledLineHeight.value
        val fontSize = (scaledFontSize.value * finalScale).sp

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
                        .align(paperTextAreaAlignment)
                        .fillMaxWidth(PaperTextAreaWidthFraction)
                        .fillMaxHeight(PaperTextAreaHeightFraction)
                        .padding(start = 8.dp, end = 8.dp, top = textPaddingTop, bottom = textPaddingBottom),
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
                                    // Balanced physics: enough to see, safe enough to fit
                                    rotationZ = -1.2f
                                    rotationX = 8f 
                                    cameraDistance = 10f * density
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
                                    fontFamily = if (isCyrillic) FortuneMessageFontCyrillic else FortuneMessageFont,
                                    fontWeight = if (isCyrillic) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = fontSize,
                                    lineHeight = lineHeight,
                                    color = PaperInk,
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false,
                                    ),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both,
                                    ),
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
