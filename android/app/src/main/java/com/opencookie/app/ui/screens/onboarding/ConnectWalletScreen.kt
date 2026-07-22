package com.opencookie.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencookie.app.R
import com.opencookie.app.data.AppReadiness
import com.opencookie.app.data.DataRefreshCoordinator
import com.opencookie.app.data.cluster.ClusterManager
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.data.wallet.ActivityResultSenderRegistry
import com.opencookie.app.data.wallet.WalletConnectionManager
import com.opencookie.app.domain.model.AppError
import com.opencookie.app.ui.theme.OpenCookieBackground
import com.opencookie.app.ui.theme.OpenCookieWordmark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectWalletUiState(
    val isConnecting: Boolean = false,
    val isSyncing: Boolean = false,
    val connectedAddress: String? = null,
    val connectedAccountLabel: String? = null,
    val error: String? = null,
    val isComplete: Boolean = false,
)

@HiltViewModel
class ConnectWalletViewModel @Inject constructor(
    private val walletManager: WalletConnectionManager,
    private val appSession: AppSession,
    private val clusterManager: ClusterManager,
    private val dataRefreshCoordinator: DataRefreshCoordinator,
    private val appReadiness: AppReadiness,
    private val activityResultSenderRegistry: ActivityResultSenderRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectWalletUiState())
    val uiState: StateFlow<ConnectWalletUiState> = _uiState.asStateFlow()

    fun connectWallet() {
        if (_uiState.value.isConnecting || _uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)
            appReadiness.awaitReady()
            clusterManager.applyClusterToRpc()

            if (activityResultSenderRegistry.current() == null) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = AppError.WalletActivityUnavailable.userMessage,
                )
                return@launch
            }

            val connectResult = walletManager.connect(forceAuthorize = false)
            if (connectResult.isFailure) {
                val err = connectResult.exceptionOrNull()
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = (err as? AppError)?.userMessage ?: "Connection failed",
                )
                return@launch
            }

            val connected = connectResult.getOrThrow()
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                isSyncing = true,
                connectedAddress = connected.publicKey.toBase58(),
                connectedAccountLabel = connected.accountLabel,
            )

            try {
                dataRefreshCoordinator.refreshAfterConnect()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = "Failed to sync with blockchain.",
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSyncing = false, isComplete = true)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

@Composable
fun ConnectWalletScreen(
    onConnected: () -> Unit,
    viewModel: ConnectWalletViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onConnected()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "onboarding")
    val cookieFloat by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(3600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "cookie_float",
    )
    val cookieScale by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(3200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "cookie_scale",
    )

    OpenCookieBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.intact_cookie),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(196.dp)
                        .graphicsLayer {
                            translationY = cookieFloat * density
                            scaleX = cookieScale
                            scaleY = cookieScale
                        },
                )
                OpenCookieWordmark(fontSize = 32, letterSpacing = 7.0)
                Text(
                    text = stringResource(R.string.onboarding_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            val isLoading = uiState.isConnecting || uiState.isSyncing || uiState.isComplete

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(174.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = if (isLoading) "loading" else "connect",
                    transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
                    label = "onboarding_state",
                ) { state ->
                    when (state) {
                        "loading" -> LoadingPanel(
                            text = when {
                                uiState.isConnecting -> stringResource(R.string.onboarding_connecting)
                                else -> stringResource(R.string.onboarding_syncing)
                            },
                        )
                        else -> ConnectPanel(onConnect = { viewModel.connectWallet() })
                    }
                }
            }

            uiState.error?.let { error ->
                ErrorPanel(
                    error = error,
                    onDismiss = { viewModel.dismissError() },
                )
            } ?: Spacer(Modifier.height(58.dp))
        }
    }
}

@Composable
private fun ConnectPanel(onConnect: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.connect_wallet).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                ),
            )
        }
        Text(
            text = stringResource(R.string.onboarding_connect_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun LoadingPanel(text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.5.dp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun ErrorPanel(
    error: String,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
