package com.opencookie.admin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencookie.admin.domain.model.Cluster
import com.opencookie.admin.domain.model.displayName
import com.opencookie.admin.ui.AdminViewModel
import com.opencookie.admin.ui.theme.AdminBackground

@Composable
fun ConnectWalletScreen(
    onConnected: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isWalletConnected) {
        if (uiState.isWalletConnected) onConnected()
    }

    if (uiState.isWalletConnected) return

    AdminBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Open Cookie Admin",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Підключіть admin-гаманець для керування Open Cookie on-chain",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            ClusterSelector(
                selected = uiState.cluster,
                onSelect = viewModel::setCluster,
                enabled = !uiState.isConnecting,
            )

            Spacer(Modifier.height(24.dp))

            if (uiState.isConnecting) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = { viewModel.connectWallet() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Підключити гаманець")
                }
            }

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun ClusterSelector(
    selected: Cluster,
    onSelect: (Cluster) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Мережа", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Cluster.entries.forEach { cluster ->
                val isSelected = cluster == selected
                if (isSelected) {
                    Button(
                        onClick = { onSelect(cluster) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    ) { Text(cluster.displayName) }
                } else {
                    OutlinedButton(
                        onClick = { onSelect(cluster) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    ) { Text(cluster.displayName) }
                }
            }
        }
    }
}
