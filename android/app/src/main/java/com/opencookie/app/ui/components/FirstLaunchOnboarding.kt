package com.opencookie.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencookie.app.R
import com.opencookie.app.ui.theme.CookieCream
import com.opencookie.app.ui.theme.CookieCreamDim

@Composable
fun FirstLaunchOnboarding(
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val titleTopPadding = maxHeight * 0.06f
        val handSize = maxWidth * 0.42f * 3f
        val handOffsetX = maxWidth * 0.16f
        val handOffsetY = maxHeight * 0.02f

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.84f)
                .padding(top = titleTopPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.first_launch_onboarding_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.4.sp,
                ),
                color = CookieCream,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.first_launch_onboarding_message),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = CookieCreamDim.copy(alpha = 0.96f),
                textAlign = TextAlign.Center,
            )
        }

        TapHintHand(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = handOffsetX, y = handOffsetY)
                .size(handSize),
        )
    }
}
