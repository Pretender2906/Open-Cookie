package com.opencookie.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencookie.app.BuildConfig
import com.opencookie.app.R
import com.opencookie.app.domain.model.NetworkFeePriority
import com.opencookie.app.domain.model.TransactionOrigin
import com.opencookie.app.ui.components.AppMessage
import com.opencookie.app.ui.components.ScreenTransactionStatus
import com.opencookie.app.ui.components.WalletChip
import com.opencookie.app.ui.theme.OpenCookieBackground

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onDisconnected: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.logoutCompleted.collect {
            onDisconnected()
        }
    }

    OpenCookieBackground {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.profile_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 12.dp, bottom = 16.dp),
                ) {
                    ScreenTransactionStatus(
                        phase = uiState.transactionState,
                        origin = uiState.transactionOrigin,
                        screenOrigin = TransactionOrigin.Profile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )

                    AppMessage(
                        message = uiState.message,
                        isSuccess = uiState.isSuccess,
                        onDismiss = { viewModel.dismissMessage() },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Button(
                        onClick = { viewModel.disconnect() },
                        enabled = !uiState.isLoggingOut &&
                            !uiState.isTransactionInProgress &&
                            !uiState.hasPendingTransactions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        if (uiState.isLoggingOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        } else {
                            Text(
                                stringResource(R.string.disconnect_wallet),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (uiState.hasProfile) {
                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                if (uiState.canCloseProfile) {
                                    viewModel.requestCloseProfile()
                                } else {
                                    viewModel.retryChainSync()
                                }
                            },
                            enabled = !uiState.isClosingProfile && !uiState.isLoggingOut &&
                                !uiState.isTransactionInProgress &&
                                !uiState.hasPendingTransactions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            if (uiState.isClosingProfile) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    stringResource(R.string.profile_close),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        if (!uiState.canCloseProfile && !uiState.isClosingProfile) {
                            Text(
                                text = stringResource(R.string.profile_close_wait_sync),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            ) {
                Spacer(Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.Center,
                ) {
                    WalletChip(
                        address = uiState.walletAddress,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    Text(
                        text = if (uiState.balanceKnown) {
                            "${uiState.balanceSol} SOL"
                        } else {
                            stringResource(R.string.profile_balance_loading)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.network_release, uiState.clusterName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )

                Spacer(Modifier.height(24.dp))
                NetworkFeePrioritySelector(
                    selected = uiState.networkFeePriority,
                    enabled = !uiState.isLoggingOut && !uiState.isClosingProfile &&
                        !uiState.isTransactionInProgress && !uiState.hasPendingTransactions,
                    onSelected = viewModel::setNetworkFeePriority,
                )

                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.profile_cookie_stats),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileRow(
                            label = stringResource(R.string.profile_stat_total),
                            value = uiState.totalCalls.toString(),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ProfileRow(
                            label = stringResource(R.string.profile_stat_today),
                            value = "${uiState.callsToday}/${uiState.maxCallsPerDay}",
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Build ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }
        }
    }
    }

    if (uiState.showCloseProfileDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCloseProfileDialog() },
            title = { Text(stringResource(R.string.profile_close_confirm_title)) },
            text = { Text(stringResource(R.string.profile_close_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmCloseProfile() }) {
                    Text(stringResource(R.string.profile_close_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCloseProfileDialog() }) {
                    Text(stringResource(R.string.profile_close_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NetworkFeePrioritySelector(
    selected: NetworkFeePriority,
    enabled: Boolean,
    onSelected: (NetworkFeePriority) -> Unit,
) {
    val isFast = selected == NetworkFeePriority.Fast
    Text(
        text = stringResource(R.string.network_fee_priority_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeePriorityOption(
                title = stringResource(R.string.network_fee_priority_standard),
                subtitle = stringResource(R.string.network_fee_priority_standard_desc),
                selected = !isFast,
                enabled = enabled,
                textAlign = TextAlign.Start,
                onClick = { onSelected(NetworkFeePriority.Standard) },
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isFast,
                onCheckedChange = { fast ->
                    onSelected(if (fast) NetworkFeePriority.Fast else NetworkFeePriority.Standard)
                },
                enabled = enabled,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            FeePriorityOption(
                title = stringResource(R.string.network_fee_priority_fast),
                subtitle = stringResource(R.string.network_fee_priority_fast_desc),
                selected = isFast,
                enabled = enabled,
                textAlign = TextAlign.End,
                onClick = { onSelected(NetworkFeePriority.Fast) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FeePriorityOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    textAlign: TextAlign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = when (textAlign) {
            TextAlign.End -> Alignment.End
            else -> Alignment.Start
        },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            },
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (selected) 0.82f else 0.52f),
            textAlign = textAlign,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}
