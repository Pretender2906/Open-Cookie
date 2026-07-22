package com.opencookie.app.ui.screens.cookie

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencookie.app.R
import com.opencookie.app.domain.model.TransactionState
import com.opencookie.app.ui.components.CookiePhase
import com.opencookie.app.ui.components.FortuneCookieStage
import com.opencookie.app.ui.components.FortunePaper
import com.opencookie.app.ui.components.TapHintHand
import com.opencookie.app.ui.theme.CookieCreamDim
import com.opencookie.app.ui.theme.OpenCookieBackground
import com.opencookie.app.ui.theme.OpenCookieWordmark
import kotlinx.coroutines.delay

@Composable
fun CookieScreen(
    onProfileClick: () -> Unit,
    viewModel: CookieViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var phase by remember { mutableStateOf(CookiePhase.IDLE) }

    LaunchedEffect(uiState.cookieMessage) {
        if (uiState.cookieMessage != null && phase == CookiePhase.BREAKING) {
            phase = CookiePhase.REVEALING
        }
    }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null && phase == CookiePhase.BREAKING) {
            phase = CookiePhase.IDLE
        }
    }
    LaunchedEffect(phase) {
        if (phase == CookiePhase.REVEALING) {
            delay(840)
            phase = CookiePhase.REVEALED
        }
    }

    val status = when (uiState.transactionState) {
        TransactionState.Building -> stringResource(R.string.tx_status_building)
        TransactionState.AwaitingSignature -> stringResource(R.string.tx_status_wallet)
        is TransactionState.Confirming -> stringResource(R.string.tx_status_confirming)
        TransactionState.Retrying -> stringResource(R.string.tx_status_retrying)
        else -> if (!uiState.configLoaded) stringResource(R.string.chain_data_loading) else null
    }

    val tappable = phase == CookiePhase.IDLE && uiState.buttonEnabled

    OpenCookieBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 8.dp, top = 24.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.size(30.dp))
                OpenCookieWordmark(modifier = Modifier.weight(1f))
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(R.string.profile_title),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                FortuneCookieStage(
                    phase = phase,
                    tappable = tappable,
                    onTap = {
                        if (tappable) {
                            viewModel.markTapHintSeen()
                            phase = CookiePhase.BREAKING
                            viewModel.breakCookie()
                        }
                    },
                    modifier = Modifier.size(520.dp),
                    paper = {
                        val message = uiState.cookieMessage
                        val paperVisible = phase == CookiePhase.REVEALING || phase == CookiePhase.REVEALED
                        val reveal by animateFloatAsState(
                            targetValue = if (paperVisible) 1f else 0f,
                            animationSpec = tween(560, easing = FastOutSlowInEasing),
                            label = "paper_reveal",
                        )
                        // Text only fades in once the paper has settled into place.
                        val textReveal by animateFloatAsState(
                            targetValue = if (phase == CookiePhase.REVEALED) 1f else 0f,
                            animationSpec = tween(420, easing = FastOutSlowInEasing),
                            label = "paper_text_reveal",
                        )
                        if (message != null && reveal > 0.01f) {
                            FortunePaper(
                                message = message,
                                textAlpha = textReveal,
                                modifier = Modifier
                                    .fillMaxWidth(0.98f)
                                    .graphicsLayer {
                                        alpha = reveal
                                        scaleX = 0.82f + 0.18f * reveal
                                        scaleY = 0.82f + 0.18f * reveal
                                        translationY = ((1f - reveal) * 34f + 6f) * density
                                    },
                            )
                        }
                    },
                )

                if (phase == CookiePhase.IDLE && uiState.showTapHint && tappable) {
                    TapHintHand(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 250.dp),
                    )
                }
            }

            BottomArea(
                phase = phase,
                status = status,
                tappable = tappable,
                costSol = uiState.costSol,
                error = uiState.error,
                isOffline = uiState.isOffline,
                callsToday = uiState.callsToday,
                maxCallsPerDay = uiState.maxCallsPerDay,
                totalCalls = uiState.totalCalls,
                configLoaded = uiState.configLoaded,
                onOpenAnother = {
                    viewModel.dismissMessage()
                    phase = CookiePhase.IDLE
                },
                onDismissError = { viewModel.dismissError() },
            )
        }
    }
}

@Composable
private fun BottomArea(
    phase: CookiePhase,
    status: String?,
    tappable: Boolean,
    costSol: String?,
    error: String?,
    isOffline: Boolean,
    callsToday: Int,
    maxCallsPerDay: Int,
    totalCalls: Long,
    configLoaded: Boolean,
    onOpenAnother: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.height(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                phase == CookiePhase.IDLE && tappable -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tap_the_cookie),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center,
                        )
                        costSol?.let {
                            Text(
                                text = stringResource(R.string.cookie_cost, it),
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                phase == CookiePhase.REVEALED -> {
                    SubtleAction(
                        text = stringResource(R.string.open_another),
                        onClick = onOpenAnother,
                    )
                }

                status != null -> StatusPill(text = status)
            }
        }

        error?.let {
            StatusPill(text = it, isError = true)
            SubtleAction(text = stringResource(R.string.retry), onClick = onDismissError)
        }

        if (isOffline) {
            StatusPill(text = stringResource(R.string.offline_banner), isError = true)
        }

        if (configLoaded && error == null) {
            Text(
                text = stringResource(R.string.cookie_stats_line, callsToday, maxCallsPerDay, totalCalls),
                style = MaterialTheme.typography.labelSmall,
                color = CookieCreamDim.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SubtleAction(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.5.sp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StatusPill(
    text: String,
    isError: Boolean = false,
) {
    val background = if (isError) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val foreground = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
            color = foreground,
            textAlign = TextAlign.Center,
        )
    }
}
