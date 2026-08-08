package com.opencookie.app.ui.screens.cookie

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencookie.app.R
import com.opencookie.app.domain.model.ProfilePresence
import com.opencookie.app.domain.model.TransactionOrigin
import com.opencookie.app.domain.model.TransactionState
import com.opencookie.app.ui.components.CookiePhase
import com.opencookie.app.ui.components.FirstLaunchOnboarding
import com.opencookie.app.ui.components.FortuneCookieStage
import com.opencookie.app.ui.components.FortunePaper
import com.opencookie.app.ui.components.ProfileCreationDialog
import com.opencookie.app.ui.theme.CookieCreamDim
import com.opencookie.app.ui.theme.OpenCookieBackground
import com.opencookie.app.ui.theme.OpenCookieWordmark
import com.opencookie.app.util.UiText
import kotlinx.coroutines.delay

private const val BreakAnimationMs = 1440L
private const val CrackHapticDelayMs = 720L
private const val CrackHapticTailDelayMs = 64L
private const val CookieHalvesOpenMs = 760L
private const val PaperZoomLeadMs = 400L
private const val PaperZoomMs = 2160
private const val PaperTextDelayMs = 1L
private const val PaperTextFadeMs = 560
private const val ResetUnlockDelayMs = 1000L
private const val ErrorAutoDismissMs = 20000L

@Composable
fun CookieScreen(
    onProfileClick: () -> Unit,
    viewModel: CookieViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var phase by rememberSaveable { mutableStateOf(CookiePhase.IDLE) }
    val latestCookieMessage by rememberUpdatedState(uiState.cookieMessage)
    val latestError by rememberUpdatedState(uiState.error)
    val hapticFeedback = LocalHapticFeedback.current
    var paperFocused by rememberSaveable { mutableStateOf(false) }
    var textVisible by rememberSaveable { mutableStateOf(false) }
    var resetEnabled by rememberSaveable { mutableStateOf(false) }
    var onboardingDismissedInSession by rememberSaveable { mutableStateOf(false) }
    var showProfileCreationDialog by rememberSaveable { mutableStateOf(false) }
    var previousProfilePresence by remember { mutableStateOf<ProfilePresence?>(null) }

    fun resetToIdleScreen() {
        viewModel.cancelTransaction()
        viewModel.dismissMessage()
        viewModel.dismissError()
        phase = CookiePhase.IDLE
        paperFocused = false
        textVisible = false
        resetEnabled = false
    }

    BackHandler(enabled = phase != CookiePhase.IDLE) {
        resetToIdleScreen()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        val isTransitioningToWallet = uiState.transactionState == TransactionState.Building ||
                uiState.transactionState == TransactionState.AwaitingSignature

        if (uiState.error != null || uiState.cookieMessage != null || !isTransitioningToWallet) {
            resetToIdleScreen()
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            delay(ErrorAutoDismissMs)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(uiState.profilePresence) {
        val previous = previousProfilePresence
        if (previous == ProfilePresence.Exists && uiState.profilePresence == ProfilePresence.NotExists) {
            onboardingDismissedInSession = false
            showProfileCreationDialog = false
        }
        previousProfilePresence = uiState.profilePresence
    }

    LaunchedEffect(
        uiState.cookieMessage,
        uiState.error,
        uiState.isCookieOpeningInProgress,
        uiState.isTransactionInProgress,
        uiState.transactionOrigin,
        uiState.transactionState,
    ) {
        when {
            uiState.error != null -> phase = CookiePhase.IDLE
            uiState.cookieMessage != null && phase != CookiePhase.BREAKING -> phase = CookiePhase.REVEALED
            uiState.hasActiveCookieTransaction() &&
                (
                    uiState.isCookieOpeningInProgress ||
                        uiState.transactionState == TransactionState.Building ||
                        uiState.transactionState == TransactionState.AwaitingSignature ||
                        uiState.transactionState is TransactionState.Confirming ||
                        uiState.transactionState == TransactionState.Retrying
                    ) &&
                phase == CookiePhase.IDLE -> {
                phase = CookiePhase.WAITING_FOR_TRANSACTION
            }
            !uiState.hasActiveCookieTransaction() &&
                uiState.cookieMessage == null &&
                (
                    phase == CookiePhase.BREAKING ||
                        phase == CookiePhase.WAITING_FOR_TRANSACTION
                    ) -> {
                phase = CookiePhase.IDLE
            }
        }
    }
    LaunchedEffect(phase) {
        if (phase == CookiePhase.BREAKING) {
            delay(CrackHapticDelayMs)
            if (phase == CookiePhase.BREAKING && latestError == null) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(50L)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            delay(CrackHapticTailDelayMs)
            if (phase == CookiePhase.BREAKING && latestError == null) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(50L)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            delay(BreakAnimationMs - CrackHapticDelayMs - CrackHapticTailDelayMs - 100L)
            if (phase == CookiePhase.BREAKING && latestError == null) {
                phase = if (latestCookieMessage != null) {
                    CookiePhase.REVEALED
                } else {
                    CookiePhase.WAITING_FOR_TRANSACTION
                }
            }
        }
    }
    LaunchedEffect(phase, uiState.cookieMessage) {
        if (phase == CookiePhase.REVEALED && uiState.cookieMessage != null) {
            paperFocused = false
            textVisible = false
            resetEnabled = false
            delay((CookieHalvesOpenMs - PaperZoomLeadMs).coerceAtLeast(0L))
            if (phase == CookiePhase.REVEALED && uiState.cookieMessage != null) {
                paperFocused = true
            }
            delay(PaperZoomMs.toLong() + PaperTextDelayMs)
            if (phase == CookiePhase.REVEALED && uiState.cookieMessage != null) {
                textVisible = true
            }
            delay(PaperTextFadeMs.toLong() + ResetUnlockDelayMs)
            if (phase == CookiePhase.REVEALED && uiState.cookieMessage != null) {
                resetEnabled = true
            }
        } else {
            paperFocused = false
            textVisible = false
            resetEnabled = false
        }
    }

    val paperFocusProgress by animateFloatAsState(
        targetValue = if (paperFocused) 1f else 0f,
        animationSpec = tween(PaperZoomMs, easing = LinearOutSlowInEasing),
        label = "paper_focus_progress",
    )
    val textReveal by animateFloatAsState(
        targetValue = if (textVisible && uiState.cookieMessage != null) 1f else 0f,
        animationSpec = tween(
            PaperTextFadeMs,
            easing = CubicBezierEasing(0.24f, 0.08f, 0.18f, 1f),
        ),
        label = "paper_text_reveal",
    )

    val status = when {
        uiState.profilePresence == ProfilePresence.Checking ||
            uiState.profilePresence == ProfilePresence.Unknown -> stringResource(R.string.chain_data_loading)
        uiState.transactionState == TransactionState.Building -> stringResource(R.string.tx_status_building)
        uiState.transactionState == TransactionState.AwaitingSignature -> stringResource(R.string.tx_status_wallet)
        uiState.transactionState is TransactionState.Confirming -> stringResource(R.string.tx_status_confirming)
        uiState.transactionState == TransactionState.Retrying -> stringResource(R.string.tx_status_retrying)
        !uiState.configLoaded -> stringResource(R.string.chain_data_loading)
        else -> null
    }

    val tappable = phase == CookiePhase.IDLE && uiState.buttonEnabled && uiState.error == null
    val showFirstLaunchOnboarding =
        phase == CookiePhase.IDLE &&
            tappable &&
            uiState.showFirstLaunchOnboarding &&
            !onboardingDismissedInSession

    val startCookieBreak = {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        phase = CookiePhase.BREAKING
        viewModel.breakCookie()
    }

    val resetOpenedCookie = {
        viewModel.dismissMessage()
        phase = CookiePhase.IDLE
    }
    val handleCookieTap = {
        when {
            uiState.error != null -> viewModel.dismissError()
            uiState.profilePresence == ProfilePresence.CheckFailed -> viewModel.retryProfileCheck()
            phase == CookiePhase.IDLE && uiState.buttonEnabled -> {
                when (uiState.profilePresence) {
                    ProfilePresence.NotExists -> {
                        onboardingDismissedInSession = true
                        showProfileCreationDialog = true
                    }

                    ProfilePresence.Exists -> {
                        startCookieBreak()
                    }

                    else -> Unit
                }
            }
        }
    }

    val stageModifier = Modifier
        .fillMaxWidth(0.92f)
        .aspectRatio(0.9f)

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
                IconButton(onClick = {
                    resetToIdleScreen()
                    onProfileClick()
                }) {
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
                BoxWithConstraints(modifier = stageModifier) {
                    FortuneCookieStage(
                        phase = phase,
                        tappable = tappable || uiState.error != null || uiState.profilePresence == ProfilePresence.CheckFailed,
                        paperFocusProgress = paperFocusProgress,
                        onTap = handleCookieTap,
                        modifier = Modifier.fillMaxSize(),
                        paper = { paperModifier ->
                            FortunePaper(
                                message = uiState.cookieMessage,
                                textRevealProgress = textReveal,
                                modifier = paperModifier,
                            )
                        },
                    )

                    if (phase == CookiePhase.REVEALED && uiState.cookieMessage != null && resetEnabled) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = resetOpenedCookie,
                                ),
                        )
                    }

                    if (showFirstLaunchOnboarding) {
                        FirstLaunchOnboarding(
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
            }

            BottomArea(
                phase = phase,
                status = status,
                tappable = tappable,
                showFirstLaunchOnboarding = showFirstLaunchOnboarding,
                profileCheckFailed = uiState.profilePresence == ProfilePresence.CheckFailed,
                costSol = uiState.costSol,
                error = uiState.error,
                isOffline = uiState.isOffline,
                callsToday = uiState.callsToday,
                maxCallsPerDay = uiState.maxCallsPerDay,
                totalCalls = uiState.totalCalls,
                configLoaded = uiState.configLoaded,
                resetEnabled = resetEnabled,
                onOpenAnother = resetOpenedCookie,
                onCookieTap = handleCookieTap,
            )
        }
    }

    if (showProfileCreationDialog) {
        ProfileCreationDialog(
            onConfirm = {
                showProfileCreationDialog = false
                startCookieBreak()
            },
            onDismiss = {
                showProfileCreationDialog = false
                onboardingDismissedInSession = true
            },
        )
    }
}

private fun CookieUiState.hasActiveCookieTransaction(): Boolean =
    isCookieOpeningInProgress ||
        (
            transactionOrigin == TransactionOrigin.BreakCookie &&
                (
                    isTransactionInProgress ||
                        transactionState == TransactionState.Building ||
                        transactionState == TransactionState.AwaitingSignature ||
                        transactionState is TransactionState.Confirming ||
                        transactionState == TransactionState.Retrying
                    )
            )

@Composable
private fun BottomArea(
    phase: CookiePhase,
    status: String?,
    tappable: Boolean,
    showFirstLaunchOnboarding: Boolean,
    profileCheckFailed: Boolean,
    costSol: String?,
    error: UiText?,
    isOffline: Boolean,
    callsToday: Int,
    maxCallsPerDay: Int,
    totalCalls: Long,
    configLoaded: Boolean,
    resetEnabled: Boolean,
    onOpenAnother: () -> Unit,
    onCookieTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp) // Увеличили общую высоту контейнера
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.height(110.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                phase == CookiePhase.IDLE && (tappable || error != null || profileCheckFailed) -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCookieTap,
                        ),
                    ) {
                        if (error != null) {
                            StatusPill(text = error.asString(), isError = true)
                            SubtleAction(text = stringResource(R.string.retry), onClick = onCookieTap)
                        } else if (profileCheckFailed) {
                            StatusPill(text = stringResource(R.string.profile_check_failed), isError = true)
                            SubtleAction(text = stringResource(R.string.retry), onClick = onCookieTap)
                        } else {
                            if (!showFirstLaunchOnboarding) {
                                Text(
                                    text = stringResource(R.string.tap_the_cookie),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Normal,
                                        letterSpacing = 0.5.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                                    textAlign = TextAlign.Center,
                                )
                            }
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
                }

                phase == CookiePhase.REVEALED -> {
                    SubtleAction(
                        text = stringResource(R.string.open_another),
                        enabled = resetEnabled,
                        onClick = onOpenAnother,
                    )
                }

                status != null -> StatusPill(text = status)
            }
        }

        if (isOffline) {
            StatusPill(text = stringResource(R.string.offline_banner), isError = true)
        }

        if (configLoaded) {
            // Используем alpha вместо удаления из макета, чтобы избежать изменения высоты Column
            Text(
                text = stringResource(R.string.cookie_stats_line, callsToday, maxCallsPerDay, totalCalls),
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 18.sp),
                color = CookieCreamDim.copy(alpha = if (error == null && !profileCheckFailed) 0.5f else 0f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SubtleAction(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.5.sp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.85f else 0.38f),
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
