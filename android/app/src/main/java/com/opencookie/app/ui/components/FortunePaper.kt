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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
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

@Composable
fun FortunePaper(
    message: String?,
    modifier: Modifier = Modifier,
    textAlpha: Float = 1f,
    revealProgress: Float = 1f,
) {
    val paperImage = ImageBitmap.imageResource(R.drawable.fortune_paper)

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val reveal = revealProgress.coerceIn(0f, 1f)
        val fullPaperWidth = maxWidth * 0.82f
        val fullPaperHeight = fullPaperWidth / PaperVisibleAspect
        val clipWidth = fullPaperWidth * lerp(0.26f, 1f, reveal)
        val clipHeight = fullPaperHeight * lerp(0.24f, 1f, reveal)

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
                .width(clipWidth)
                .height(clipHeight)
                .graphicsLayer { clip = true },
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
                        .fillMaxWidth(0.58f)
                        .fillMaxHeight(0.38f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (content.isNotEmpty() && textAlpha > 0.01f) {
                        Text(
                            text = content,
                            modifier = Modifier.alpha(textAlpha),
                            textAlign = TextAlign.Center,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
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

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)
