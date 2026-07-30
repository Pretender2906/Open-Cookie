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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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
private const val PaperTextAreaWidthFraction = 0.72f // More space for long words
/** Matches the visible cream band on the paper crop, not the full strip height. */
private const val PaperTextAreaHeightFraction = 0.68f // Slightly taller for 4-line messages
/** Positions the text block on the paper; cream band sits below geometric center. */
private const val PaperTextAreaVerticalBias3Lines = 0.28f
private const val PaperTextAreaVerticalBias4Lines = 0.16f
private const val LineHeightSafety3Lines = 0.95f
private const val LineHeightSafety4Lines = 0.85f // Even more strict to guarantee 4 lines fit
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
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val fullPaperWidth = maxWidth * 0.88f
        val fullPaperHeight = fullPaperWidth / PaperVisibleAspect

        val content = message?.trim().orEmpty()
        val isCyrillic = content.any { it in '\u0400'..'\u04FF' }

        val textPaddingHorizontal = 8.dp
        val textPaddingTop = 4.dp
        val textPaddingBottom = 4.dp

        val fontFamily = if (isCyrillic) FortuneMessageFontCyrillic else FortuneMessageFont
        val fontWeight = if (isCyrillic) FontWeight.SemiBold else FontWeight.Medium

        // Dynamically determine the best font size and line count using TextMeasurer
        val (fontSize, lineHeight, maxLines) = remember(content, maxWidth, isCyrillic) {
            val textAreaWidthPx = with(density) {
                (maxWidth * 0.88f * PaperTextAreaWidthFraction - textPaddingHorizontal * 2).toPx()
            }.roundToInt()
            
            val textAreaHeightBase = maxWidth * 0.88f / PaperVisibleAspect * PaperTextAreaHeightFraction - textPaddingTop - textPaddingBottom
            
            val styleBase = TextStyle(
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            )

            // Priority: keep it readable and prefer 3 lines, fallback to 4 for very long phrases.
            val candidates = listOf(
                Triple(19.sp, 23.sp, 3), // Standard 3-line
                Triple(17.sp, 21.sp, 3), // Medium 3-line
                Triple(15.sp, 18.sp, 3), // Compact 3-line
                Triple(17.sp, 20.sp, 4), // 4-line medium
                Triple(15.sp, 18.sp, 4), // 4-line compact
                Triple(14.sp, 16.sp, 4), // 4-line very compact
                Triple(13.sp, 15.sp, 4), // 4-line extra compact (emergency)
            )

            var bestFit: Triple<androidx.compose.ui.unit.TextUnit, androidx.compose.ui.unit.TextUnit, Int>? = null

            for (candidate in candidates) {
                val (fs, lh, ml) = candidate
                val measurement = textMeasurer.measure(
                    text = content,
                    style = styleBase.copy(fontSize = fs, lineHeight = lh),
                    constraints = Constraints(maxWidth = textAreaWidthPx.coerceAtLeast(1)),
                    maxLines = ml
                )
                
                val safety = if (ml == 4) LineHeightSafety4Lines else LineHeightSafety3Lines
                val maxHeightPx = with(density) { (textAreaHeightBase * safety).toPx() }
                
                if (!measurement.hasVisualOverflow && measurement.size.height <= maxHeightPx) {
                    // Special rule for Cyrillic: don't allow the largest size (19sp) to occupy 3 lines.
                    // We prefer 2 lines of 17sp over 3 lines of 19sp for better visual balance.
                    if (isCyrillic && fs == 19.sp && measurement.lineCount == 3) {
                        val m17 = textMeasurer.measure(
                            text = content,
                            style = styleBase.copy(fontSize = 17.sp, lineHeight = 21.sp),
                            constraints = Constraints(maxWidth = textAreaWidthPx.coerceAtLeast(1)),
                            maxLines = 3
                        )
                        // If 17sp fits in 2 lines, it's the perfect match.
                        if (!m17.hasVisualOverflow && m17.lineCount <= 2) {
                            bestFit = Triple(17.sp, 21.sp, 3)
                            break
                        }
                        // Otherwise, we still skip 19sp for 3 lines and allow 17sp or smaller to take 3 lines.
                        continue 
                    }
                    
                    bestFit = candidate
                    break
                }
            }

            bestFit ?: candidates.last()
        }

        val paperTextAreaAlignment = BiasAlignment(
            horizontalBias = 0f,
            verticalBias = if (maxLines == 4) {
                PaperTextAreaVerticalBias4Lines
            } else {
                PaperTextAreaVerticalBias3Lines
            },
        )

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
                        .padding(start = textPaddingHorizontal, end = textPaddingHorizontal, top = textPaddingTop, bottom = textPaddingBottom),
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
                                    fontFamily = fontFamily,
                                    fontWeight = fontWeight,
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
