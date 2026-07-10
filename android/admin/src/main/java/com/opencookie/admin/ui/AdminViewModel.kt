package com.opencookie.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencookie.admin.data.program.ProtocolConfig
import com.opencookie.admin.data.program.UpdateConfigParams
import com.opencookie.admin.data.repository.AdminRepository
import com.opencookie.admin.data.rpc.SolanaRpcClient
import com.opencookie.admin.data.session.AdminSession
import com.opencookie.admin.data.transaction.AdminTransactionExecutor
import com.opencookie.admin.data.wallet.ActivityResultSenderRegistry
import com.opencookie.admin.data.wallet.WalletConnectionManager
import com.opencookie.admin.domain.model.AdminError
import com.opencookie.admin.domain.model.Cluster
import com.opencookie.admin.domain.model.TxPhase
import com.opencookie.admin.util.PublicKey
import com.opencookie.admin.util.formatLamports
import com.opencookie.admin.util.formatPriceLamports
import com.opencookie.admin.util.lamportsFromSolInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfigFormState(
    val pendingAdmin: String = "",
    val priceLamports: String = "",
    val maxCallsPerDay: String = "",
)

data class AdminUiState(
    val isWalletConnected: Boolean = false,
    val walletAddress: String? = null,
    val walletLabel: String? = null,
    val cluster: Cluster = Cluster.Devnet,
    val isAdminAuthorized: Boolean = false,
    val canAcceptAdmin: Boolean = false,
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val txPhase: TxPhase = TxPhase.Idle,
    val lastSignature: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val config: ProtocolConfig? = null,
    val treasuryBalance: String = "—",
    val walletBalance: String = "—",
    val adminAuthority: String? = null,
    val pendingAdmin: String? = null,
    val priceDisplay: String = "—",
    val configForm: ConfigFormState = ConfigFormState(),
    val withdrawDestination: String = "",
    val withdrawAmountSol: String = "",
    val selectedTab: AdminTab = AdminTab.Config,
)

enum class AdminTab(val title: String) {
    Config("Конфіг"),
    AcceptAdmin("Accept Admin"),
    Treasury("Treasury"),
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val session: AdminSession,
    private val walletManager: WalletConnectionManager,
    private val repository: AdminRepository,
    private val rpcClient: SolanaRpcClient,
    private val txExecutor: AdminTransactionExecutor,
    private val senderRegistry: ActivityResultSenderRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            session.restoreFromDisk()
            walletManager.restoreSessionFromDisk()
            rpcClient.applyCluster(session.state.value.cluster)
            syncFromSession()
            if (session.state.value.isWalletConnected) {
                refreshChainState()
            }
        }
        viewModelScope.launch {
            session.state.collect { syncFromSession() }
        }
    }

    private fun syncFromSession() {
        val s = session.state.value
        val config = s.config

        _uiState.update { current ->
            current.copy(
                isWalletConnected = s.isWalletConnected,
                walletAddress = s.walletAddress?.toBase58(),
                cluster = s.cluster,
                isAdminAuthorized = s.isAdminAuthorized,
                canAcceptAdmin = s.canAcceptAdmin,
                config = config,
                treasuryBalance = formatLamports(s.treasuryBalanceLamports),
                walletBalance = formatLamports(s.walletBalanceLamports),
                adminAuthority = config?.adminAuthority?.toBase58(),
                pendingAdmin = config?.pendingAdmin?.takeUnless { it.isDefault() }?.toBase58(),
                priceDisplay = config?.let { formatPriceLamports(it.priceLamports) } ?: "—",
                configForm = config?.let { configToForm(it) } ?: current.configForm,
            )
        }
    }

    fun selectTab(tab: AdminTab) {
        _uiState.update { it.copy(selectedTab = tab, errorMessage = null, successMessage = null) }
    }

    fun setCluster(cluster: Cluster) {
        if (_uiState.value.cluster == cluster) return
        viewModelScope.launch {
            val wasConnected = session.state.value.isWalletConnected
            walletManager.setCluster(cluster)
            clearMessages()
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.refresh()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    if (cluster == Cluster.MainnetBeta) {
                        val hint = if (wasConnected) {
                            "Переключено на Mainnet. Підключіть гаманець знову — у гаманці теж має бути Mainnet."
                        } else {
                            "Mainnet. Перед підключенням перевірте, що гаманець на Mainnet."
                        }
                        _uiState.update { it.copy(successMessage = hint) }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    showError((e as? AdminError)?.userMessage ?: e.message)
                }
        }
    }

    fun updateConfigForm(update: ConfigFormState.() -> ConfigFormState) {
        _uiState.update { it.copy(configForm = it.configForm.update()) }
    }

    fun updateWithdrawDestination(value: String) {
        _uiState.update { it.copy(withdrawDestination = value) }
    }

    fun updateWithdrawAmount(value: String) {
        _uiState.update { it.copy(withdrawAmountSol = value) }
    }

    fun connectWallet(force: Boolean = false) {
        val sender = senderRegistry.current()
        if (sender == null) {
            showError(AdminError.WalletRejected.userMessage)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            walletManager.connect(sender, forceAuthorize = force)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(walletLabel = result.accountLabel, isConnecting = false)
                    }
                    refreshChainState()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isConnecting = false) }
                    showError((e as? AdminError)?.userMessage ?: e.message)
                }
        }
    }

    fun disconnectWallet() {
        val sender = senderRegistry.current() ?: return
        viewModelScope.launch {
            walletManager.disconnect(sender)
            _uiState.update { AdminUiState(cluster = session.state.value.cluster) }
        }
    }

    fun refreshChainState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.refresh()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    showError((e as? AdminError)?.userMessage ?: e.message)
                }
        }
    }

    fun submitUpdateConfig() {
        val sender = senderRegistry.current() ?: return
        val form = _uiState.value.configForm
        val params = runCatching {
            UpdateConfigParams(
                pendingAdmin = parsePubkeyOrDefault(form.pendingAdmin),
                priceLamports = form.priceLamports.toLong(),
                maxCallsPerDay = form.maxCallsPerDay.toInt(),
            )
        }.getOrElse {
            showError("Перевірте поля конфігурації")
            return
        }

        runTransaction("Конфіг оновлено") {
            txExecutor.updateConfig(sender, params)
        }
    }

    fun withdrawTreasury() {
        val sender = senderRegistry.current() ?: return
        val destination = _uiState.value.withdrawDestination.trim()
        if (!PublicKey.isValid(destination)) {
            showError(AdminError.InvalidAddress(destination).userMessage)
            return
        }
        val lamports = lamportsFromSolInput(_uiState.value.withdrawAmountSol)
        if (lamports == null || lamports <= 0) {
            showError("Вкажіть суму в SOL")
            return
        }
        runTransaction("Виведено ${formatLamports(lamports)}") {
            txExecutor.withdrawTreasury(sender, PublicKey(destination), lamports)
        }
    }

    fun acceptAdmin() {
        val sender = senderRegistry.current() ?: return
        runTransaction("Admin authority прийнято") {
            txExecutor.acceptAdmin(sender)
        }
    }

    fun setPendingAdminToWallet() {
        val wallet = _uiState.value.walletAddress ?: return
        _uiState.update {
            it.copy(configForm = it.configForm.copy(pendingAdmin = wallet))
        }
    }

    fun clearPendingAdmin() {
        _uiState.update {
            it.copy(configForm = it.configForm.copy(pendingAdmin = ""))
        }
    }

    fun reloadConfigFromChain() {
        val config = _uiState.value.config ?: return
        _uiState.update {
            it.copy(configForm = configToForm(config))
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun runTransaction(successText: String, block: suspend () -> Result<String>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(txPhase = TxPhase.Building, errorMessage = null, successMessage = null)
            }
            _uiState.update { it.copy(txPhase = TxPhase.AwaitingSignature) }
            block()
                .onSuccess { signature ->
                    _uiState.update {
                        it.copy(
                            txPhase = TxPhase.Success,
                            lastSignature = signature,
                            successMessage = "$successText\nTx: ${signature.take(16)}…",
                        )
                    }
                    refreshChainState()
                    _uiState.update { it.copy(txPhase = TxPhase.Idle) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            txPhase = TxPhase.Failed,
                            errorMessage = (e as? AdminError)?.userMessage ?: e.message,
                        )
                    }
                    _uiState.update { it.copy(txPhase = TxPhase.Idle) }
                }
        }
    }

    private fun showError(message: String?) {
        _uiState.update { it.copy(errorMessage = message ?: "Помилка") }
    }

    private fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun parsePubkeyOrDefault(input: String): PublicKey {
        val trimmed = input.trim()
        return if (trimmed.isEmpty() || !PublicKey.isValid(trimmed)) {
            PublicKey.DEFAULT
        } else {
            PublicKey(trimmed)
        }
    }

    private fun configToForm(config: ProtocolConfig) = ConfigFormState(
        pendingAdmin = config.pendingAdmin.takeUnless { it.isDefault() }?.toBase58() ?: "",
        priceLamports = config.priceLamports.toString(),
        maxCallsPerDay = config.maxCallsPerDay.toString(),
    )
}
