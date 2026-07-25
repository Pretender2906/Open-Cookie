package com.opencookie.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.opencookie.app.R
import com.opencookie.app.ui.theme.OpenCookieBackground
import com.opencookie.app.ui.theme.OpenCookieTheme

@Preview(
    name = "Onboarding on cookie stage",
    showBackground = true,
    backgroundColor = 0xFF130D0B,
    widthDp = 412,
    heightDp = 892,
)
@Composable
private fun FirstLaunchOnboardingStagePreview() {
    OpenCookieTheme {
        OpenCookieBackground {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(0.9f),
                ) {
                    Image(
                        painter = painterResource(R.drawable.intact_cookie),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    FirstLaunchOnboarding(
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Onboarding overlay only",
    showBackground = true,
    backgroundColor = 0xFF130D0B,
    widthDp = 360,
    heightDp = 400,
)
@Composable
private fun FirstLaunchOnboardingOverlayPreview() {
    OpenCookieTheme {
        OpenCookieBackground {
            FirstLaunchOnboarding(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(
    name = "Hand hint",
    showBackground = true,
    backgroundColor = 0xFF130D0B,
)
@Composable
private fun TapHintHandPreview() {
    OpenCookieTheme {
        OpenCookieBackground {
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                TapHintHand(modifier = Modifier.size(140.dp))
            }
        }
    }
}
