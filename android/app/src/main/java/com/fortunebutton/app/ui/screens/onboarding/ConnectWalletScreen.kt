package com.fortunebutton.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortunebutton.app.R
import com.fortunebutton.app.data.AppReadiness
import com.fortunebutton.app.data.DataRefreshCoordinator
import com.fortunebutton.app.data.cluster.ClusterManager
import com.fortunebutton.app.data.session.AppSession
import com.fortunebutton.app.data.wallet.ActivityResultSenderRegistry
import com.fortunebutton.app.data.wallet.WalletConnectionManager
import com.fortunebutton.app.domain.model.AppError
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

    val infiniteTransition = rememberInfiniteTransition(label = "title")
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "title_scale",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(64.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🔮", fontSize = 80.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer {
                    scaleX = titleScale
                    scaleY = titleScale
                },
            )
            Text(
                text = stringResource(R.string.onboarding_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        val isLoading = uiState.isConnecting || uiState.isSyncing || uiState.isComplete

        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = if (isLoading) "loading" else "connect",
                transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
                label = "onboarding_state",
            ) { state ->
                when (state) {
                    "loading" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = when {
                                    uiState.isConnecting -> stringResource(R.string.onboarding_connecting)
                                    uiState.isSyncing -> stringResource(R.string.onboarding_syncing)
                                    else -> stringResource(R.string.onboarding_syncing)
                                },
                            )
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = { viewModel.connectWallet() },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                Text(
                                    stringResource(R.string.connect_wallet),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                            Text(
                                text = stringResource(R.string.onboarding_connect_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
        }

        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = { viewModel.dismissError() }) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
