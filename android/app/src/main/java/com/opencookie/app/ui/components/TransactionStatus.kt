package com.opencookie.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.opencookie.app.R
import com.opencookie.app.data.transaction.GlobalTransactionUi
import com.opencookie.app.domain.model.TransactionOrigin
import com.opencookie.app.domain.model.TransactionState
import com.opencookie.app.util.UiText

@Composable
fun ScreenTransactionStatus(
    phase: TransactionState,
    origin: TransactionOrigin?,
    screenOrigin: TransactionOrigin,
    modifier: Modifier = Modifier,
) {
    if (!GlobalTransactionUi.isActivePhase(phase) || origin == null) return

    val message = when {
        origin == screenOrigin -> when (phase) {
            TransactionState.Building -> if (screenOrigin == TransactionOrigin.Profile) {
                stringResource(R.string.tx_status_close_profile)
            } else {
                stringResource(R.string.tx_status_building)
            }
            TransactionState.AwaitingSignature -> stringResource(R.string.tx_status_wallet)
            is TransactionState.Confirming -> stringResource(R.string.tx_status_confirming)
            TransactionState.Retrying -> stringResource(R.string.tx_status_retrying)
            else -> null
        }
        else -> stringResource(R.string.transaction_in_progress_compact)
    } ?: return

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
fun AppMessage(
    message: UiText?,
    onDismiss: (() -> Unit)? = null,
    isSuccess: Boolean = false,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        if (message == null) return@AnimatedVisibility
        val containerColor = if (isSuccess) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
        val contentColor = if (isSuccess) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = containerColor,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message.asString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
                if (onDismiss != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dismiss_message),
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        modifier = Modifier.clickable(onClick = onDismiss),
                    )
                }
            }
        }
    }
}
