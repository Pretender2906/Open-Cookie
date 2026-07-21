package com.opencookie.admin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opencookie.admin.BuildConfig
import com.opencookie.admin.domain.model.TxPhase
import com.opencookie.admin.domain.model.displayName
import com.opencookie.admin.ui.AdminTab
import com.opencookie.admin.ui.AdminUiState
import com.opencookie.admin.ui.AdminViewModel
import com.opencookie.admin.ui.components.AdminTabChip
import com.opencookie.admin.ui.theme.AdminBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    AdminBackground {
        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Open Cookie Admin")
                            Text(
                                "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    actions = {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp))
                        } else {
                            IconButton(onClick = viewModel::refreshChainState) {
                                Icon(Icons.Default.Refresh, contentDescription = "Оновити")
                            }
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                WalletHeader(uiState, onDisconnect = viewModel::disconnectWallet)
                ClusterSelector(
                    selected = uiState.cluster,
                    onSelect = viewModel::setCluster,
                    enabled = uiState.txPhase == TxPhase.Idle && !uiState.isLoading,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (!uiState.isAdminAuthorized && !uiState.canAcceptAdmin) {
                    NonAdminBanner(uiState.adminAuthority)
                }
                AdminTabRow(uiState.selectedTab, viewModel::selectTab)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (uiState.selectedTab) {
                        AdminTab.Config -> ConfigTab(uiState, viewModel)
                        AdminTab.AcceptAdmin -> AcceptAdminTab(uiState, viewModel)
                        AdminTab.Treasury -> TreasuryTab(uiState, viewModel)
                    }
                    TxStatusBanner(uiState.txPhase)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun NonAdminBanner(adminAuthority: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "Режим перегляду",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ваш гаманець не є admin authority. Дані on-chain видно, але admin-дії заблоковані.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            adminAuthority?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Admin: ${it.take(8)}…${it.takeLast(6)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun WalletHeader(uiState: AdminUiState, onDisconnect: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Гаманець", style = MaterialTheme.typography.labelMedium)
                val address = uiState.walletAddress
                Text(
                    text = address?.let { "${it.take(6)}…${it.takeLast(4)}" } ?: "—",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = if (address != null) {
                        Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("wallet", address))
                            Toast.makeText(context, "Адресу скопійовано", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Modifier
                    },
                )
                Text(
                    "${uiState.cluster.displayName} · ${uiState.walletBalance}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                AdminStatusChip(uiState.isAdminAuthorized, uiState.canAcceptAdmin)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onDisconnect) {
                    Text("Відключити")
                }
            }
        }
    }
}

@Composable
private fun AdminStatusChip(isAdmin: Boolean, canAccept: Boolean) {
    when {
        isAdmin -> AssistChip(
            onClick = {},
            label = { Text("Admin ✓") },
            leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
        )
        canAccept -> AssistChip(
            onClick = {},
            label = { Text("Pending admin") },
            leadingIcon = { Icon(Icons.Default.Warning, null) },
        )
        else -> AssistChip(
            onClick = {},
            label = { Text("Не admin") },
            leadingIcon = { Icon(Icons.Default.Error, null) },
        )
    }
}

@Composable
private fun AdminTabRow(selected: AdminTab, onSelect: (AdminTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AdminTab.entries.forEach { tab ->
            AdminTabChip(
                selected = tab == selected,
                label = tab.title,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun ConfigTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    val form = uiState.configForm
    val fieldColors = defaultFieldColors()

    Text(
        text = "Конфігурація протоколу",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )

    StatCard("Admin authority", uiState.adminAuthority ?: "—")
    StatCard("Pending admin", uiState.pendingAdmin ?: "немає")
    StatCard("Ціна", uiState.priceDisplay)

    OutlinedButton(
        onClick = viewModel::reloadConfigFromChain,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Завантажити з chain")
    }

    ConfigField(
        label = "Pending admin",
        value = form.pendingAdmin,
        hint = "Адреса нового admin (порожньо = скинути). Потім accept_admin на новому гаманці.",
        onChange = { viewModel.updateConfigForm { copy(pendingAdmin = it) } },
        enabled = uiState.isAdminAuthorized,
        colors = fieldColors,
    )
    ConfigField(
        label = "Ціна (SOL)",
        value = form.priceSol,
        hint = "Ціна натискання кнопки в SOL.",
        onChange = { viewModel.updateConfigForm { copy(priceSol = it) } },
        enabled = uiState.isAdminAuthorized,
        colors = fieldColors,
    )
    ConfigField(
        label = "Max calls per day",
        value = form.maxCallsPerDay,
        hint = "Максимум повідомлень на користувача за добу (on-chain).",
        onChange = { viewModel.updateConfigForm { copy(maxCallsPerDay = it) } },
        enabled = uiState.isAdminAuthorized,
        colors = fieldColors,
    )

    if (uiState.isAdminAuthorized) {
        OutlinedButton(onClick = viewModel::setPendingAdminToWallet, modifier = Modifier.fillMaxWidth()) {
            Text("Вставити мій гаманець як pending")
        }
        OutlinedButton(onClick = viewModel::clearPendingAdmin, modifier = Modifier.fillMaxWidth()) {
            Text("Очистити pending admin")
        }
    }

    Button(
        onClick = viewModel::submitUpdateConfig,
        enabled = uiState.isAdminAuthorized && uiState.txPhase == TxPhase.Idle && uiState.config != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Update Config")
    }
}

@Composable
private fun AcceptAdminTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text(
        text = "Передача admin authority",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )

    StatCard("Поточний admin", uiState.adminAuthority ?: "—")
    StatCard("Pending admin", uiState.pendingAdmin ?: "немає")

    if (uiState.canAcceptAdmin) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ваш гаманець збігається з pending admin",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Натисніть Accept Admin, щоб підтвердити передачу admin authority.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(
            onClick = viewModel::acceptAdmin,
            enabled = uiState.txPhase == TxPhase.Idle,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Accept Admin")
        }
    } else {
        Text(
            "Accept Admin доступний лише коли ваш гаманець збігається з pending admin on-chain.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (uiState.isAdminAuthorized) {
            Text(
                "1. Вкажіть pending admin у вкладці Конфіг → Update Config\n2. Новий admin підключає цей додаток і натискає Accept Admin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TreasuryTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    val fieldColors = defaultFieldColors()

    Text(
        text = "Виведення з treasury",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
    StatCard("Баланс treasury (SOL)", uiState.treasuryBalance)

    OutlinedTextField(
        value = uiState.withdrawDestination,
        onValueChange = viewModel::updateWithdrawDestination,
        label = { Text("Адреса отримувача") },
        supportingText = { Text("Будь-який system account, не treasury PDA") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = uiState.isAdminAuthorized,
        colors = fieldColors,
    )
    OutlinedTextField(
        value = uiState.withdrawAmountSol,
        onValueChange = viewModel::updateWithdrawAmount,
        label = { Text("Сума (SOL)") },
        supportingText = { Text("Залишається rent-exempt мінімум у vault") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = uiState.isAdminAuthorized,
        colors = fieldColors,
    )
    Button(
        onClick = viewModel::withdrawTreasury,
        enabled = uiState.isAdminAuthorized && uiState.txPhase == TxPhase.Idle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Withdraw Treasury")
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    hint: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    colors: TextFieldColors,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = { Text(hint) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        colors = colors,
    )
}

@Composable
private fun defaultFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
)

@Composable
private fun TxStatusBanner(phase: TxPhase) {
    if (phase == TxPhase.Idle) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (phase == TxPhase.AwaitingSignature || phase == TxPhase.Building) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
            Text(
                when (phase) {
                    TxPhase.Building -> "Збірка транзакції…"
                    TxPhase.AwaitingSignature -> "Очікування підпису в гаманці…"
                    TxPhase.Confirming -> "Підтвердження…"
                    TxPhase.Success -> "Транзакція підтверджена ✓"
                    TxPhase.Failed -> "Транзакція не вдалась"
                    TxPhase.Idle -> ""
                },
            )
        }
    }
}
