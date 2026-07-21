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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "title_scale",
    )
    val cookieFloat by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "cookie_float",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF21140F),
                        Color(0xFF120F0D),
                        Color(0xFF090807),
                    ),
                ),
            ),
    ) {
        OnboardingCrumbField(Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(18.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OnboardingCookieHero(
                    modifier = Modifier
                        .size(188.dp)
                        .graphicsLayer { translationY = cookieFloat },
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
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
                                uiState.isSyncing -> stringResource(R.string.onboarding_syncing)
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Text(
                stringResource(R.string.connect_wallet),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        ) {
            Text(
                text = stringResource(R.string.onboarding_connect_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun LoadingPanel(text: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(46.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ErrorPanel(
    error: String,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun OnboardingCookieHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.33f
            drawCircle(
                color = Color(0xFFFFA928).copy(alpha = 0.26f),
                radius = radius * 1.65f,
                center = center,
            )
            drawCircle(
                color = Color(0xFFFFB84D),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = Color(0xFF8F4B1F),
                radius = radius * 0.1f,
                center = center + Offset(-radius * 0.38f, -radius * 0.2f),
            )
            drawCircle(
                color = Color(0xFF8F4B1F),
                radius = radius * 0.09f,
                center = center + Offset(radius * 0.2f, -radius * 0.31f),
            )
            drawCircle(
                color = Color(0xFF8F4B1F),
                radius = radius * 0.08f,
                center = center + Offset(-radius * 0.04f, radius * 0.25f),
            )
            drawCircle(
                color = Color(0xFF120F0D),
                radius = radius * 0.28f,
                center = center + Offset(radius * 0.56f, -radius * 0.42f),
            )
        }
    }
}

@Composable
private fun OnboardingCrumbField(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val crumbs = listOf(
            Offset(size.width * 0.12f, size.height * 0.12f) to 3.5f,
            Offset(size.width * 0.86f, size.height * 0.16f) to 5f,
            Offset(size.width * 0.19f, size.height * 0.42f) to 4f,
            Offset(size.width * 0.82f, size.height * 0.5f) to 3.5f,
            Offset(size.width * 0.28f, size.height * 0.78f) to 5.5f,
            Offset(size.width * 0.75f, size.height * 0.84f) to 4.5f,
        )
        crumbs.forEach { (offset, radius) ->
            drawCircle(
                color = Color(0xFFFFB347).copy(alpha = 0.22f),
                radius = radius,
                center = offset,
            )
        }
    }
}
