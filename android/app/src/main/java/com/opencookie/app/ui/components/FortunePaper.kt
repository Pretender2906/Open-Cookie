package com.opencookie.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencookie.app.R
import com.opencookie.app.ui.theme.PaperInk

private const val PaperCropLeft = 127
private const val PaperCropTop = 810
private const val PaperCropWidth = 783
private const val PaperCropHeight = 356
private const val PaperAspect = PaperCropWidth.toFloat() / PaperCropHeight.toFloat()

/**
 * The revealed fortune: the visible paper strip is cropped out of the transparent source
 * asset, then used as the real text container so the message stays printed on the paper.
 */
@Composable
fun FortunePaper(
    message: String,
    modifier: Modifier = Modifier,
    textAlpha: Float = 1f,
) {
    BoxWithConstraints(
        modifier = modifier.aspectRatio(PaperAspect),
        contentAlignment = Alignment.Center,
    ) {
        val paper = ImageBitmap.imageResource(R.drawable.fortune_paper)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = paper,
                srcOffset = IntOffset(PaperCropLeft, PaperCropTop),
                srcSize = IntSize(PaperCropWidth, PaperCropHeight),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            )
        }

        val compactText = message.length > 84 || maxWidth < 360.dp
        val mediumText = message.length > 52 || maxWidth < 410.dp
        val fontSize = when {
            compactText -> 12.sp
            mediumText -> 14.sp
            else -> 16.sp
        }
        val lineHeight = when {
            compactText -> 16.sp
            mediumText -> 19.sp
            else -> 21.sp
        }

        // Keep the text inside the physically visible center span of the paper so the
        // cookie halves can overlap the rolled outer edges without ever crossing the words.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .fillMaxHeight(0.56f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message,
                modifier = Modifier.alpha(textAlpha),
                textAlign = TextAlign.Center,
                maxLines = 3,
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
