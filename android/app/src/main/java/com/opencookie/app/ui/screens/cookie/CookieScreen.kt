package com.opencookie.app.ui.screens.cookie

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencookie.app.R
import com.opencookie.app.domain.model.TransactionState
import com.opencookie.app.ui.components.BreakCookieButton
import com.opencookie.app.ui.components.CookieMessageCard
import com.opencookie.app.ui.components.StatCounter

@Composable
fun CookieScreen(
    viewModel: CookieViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.cookie_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                StatCounter(
                    callsToday = uiState.callsToday,
                    totalCalls = uiState.totalCalls,
                )

                BreakCookieButton(
                    text = stringResource(R.string.break_cookie),
                    onClick = { viewModel.breakCookie() },
                    enabled = uiState.buttonEnabled,
                )

                when (val tx = uiState.transactionState) {
                    TransactionState.Building -> StatusText(stringResource(R.string.tx_status_building))
                    TransactionState.AwaitingSignature -> StatusText(stringResource(R.string.tx_status_wallet))
                    is TransactionState.Confirming -> StatusText(stringResource(R.string.tx_status_confirming))
                    TransactionState.Retrying -> StatusText(stringResource(R.string.tx_status_retrying))
                    else -> {
                        if (!uiState.configLoaded) {
                            StatusText(stringResource(R.string.chain_data_loading))
                        }
                    }
                }

                uiState.cookieMessage?.let { message ->
                    CookieMessageCard(message = message)
                    TextButton(onClick = { viewModel.dismissMessage() }) {
                        Text(stringResource(R.string.dismiss_message))
                    }
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text(stringResource(R.string.retry))
                }
            }

            if (uiState.isOffline) {
                Text(
                    text = stringResource(R.string.offline_banner),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
