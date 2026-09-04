package com.opencookie.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencookie.app.R
import com.opencookie.app.data.DataRefreshCoordinator
import com.opencookie.app.data.local.AppLocaleManager
import com.opencookie.app.data.local.PreferencesStore
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.data.transaction.Action
import com.opencookie.app.data.transaction.GlobalTransactionUi
import com.opencookie.app.data.transaction.TransactionRunner
import com.opencookie.app.data.transaction.canStartTransaction
import com.opencookie.app.data.wallet.ActivityResultSenderRegistry
import com.opencookie.app.data.wallet.WalletConnectionManager
import com.opencookie.app.domain.model.AppLanguage
import com.opencookie.app.domain.model.AppError
import com.opencookie.app.domain.model.ChainSyncState
import com.opencookie.app.domain.model.NetworkFeePriority
import com.opencookie.app.domain.model.TransactionOrigin
import com.opencookie.app.domain.model.TransactionState
import com.opencookie.app.domain.model.chainSyncState
import com.opencookie.app.domain.model.displayLabel
import com.opencookie.app.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

data class ProfileUiState(
    val walletAddress: String = "",
    val balanceSol: String = "0",
    val balanceKnown: Boolean = false,
    val callsToday: Int = 0,
    val maxCallsPerDay: Int = 0,
    val totalCalls: Long = 0,
    val clusterName: String = "",
    val networkFeePriority: NetworkFeePriority = NetworkFeePriority.Standard,
    val appLanguage: AppLanguage = AppLanguage.SystemDefault,
    val hasProfile: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isClosingProfile: Boolean = false,
    val isTransactionInProgress: Boolean = false,
    val hasPendingTransactions: Boolean = false,
    val showCloseProfileDialog: Boolean = false,
    val showCloseSuccessDialog: Boolean = false,
    val transactionState: TransactionState = TransactionState.Idle,
    val transactionOrigin: TransactionOrigin? = null,
    val message: UiText? = null,
    val isSuccess: Boolean = false,
    val canCloseProfile: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val appSession: AppSession,
    private val globalTransactionUi: GlobalTransactionUi,
    private val walletManager: WalletConnectionManager,
    private val activityResultSenderRegistry: ActivityResultSenderRegistry,
    private val preferencesStore: PreferencesStore,
    private val appLocaleManager: AppLocaleManager,
    private val transactionRunner: TransactionRunner,
    private val dataRefreshCoordinator: DataRefreshCoordinator,
) : ViewModel() {

    private val _isLoggingOut = MutableStateFlow(false)
    private val _isClosingProfile = MutableStateFlow(false)
    private val _showCloseProfileDialog = MutableStateFlow(false)
    private val _showCloseSuccessDialog = MutableStateFlow(false)
    private val _message = MutableStateFlow<UiText?>(null)
    private val _isSuccess = MutableStateFlow(false)
    private val _logoutCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutCompleted: SharedFlow<Unit> = _logoutCompleted.asSharedFlow()

    private val _appLanguage = MutableStateFlow(appLocaleManager.getCurrentLanguage())

    private data class ProfileFlags(
        val isLoggingOut: Boolean,
        val isClosingProfile: Boolean,
        val showCloseProfileDialog: Boolean,
        val showCloseSuccessDialog: Boolean,
    )

    val uiState: StateFlow<ProfileUiState> = combine(
        appSession.state,
        globalTransactionUi.state,
        combine(
            _isLoggingOut,
            _isClosingProfile,
            _showCloseProfileDialog,
            _showCloseSuccessDialog
        ) { a, b, c, d ->
            ProfileFlags(a, b, c, d)
        },
        combine(_message, _isSuccess) { message, isSuccess -> message to isSuccess },
        combine(preferencesStore.networkFeePriorityFlow(), _appLanguage) { a, b -> a to b },
    ) { session, globalTx, flags, (message, isSuccess), settings ->
        val (networkFeePriority, appLanguage) = settings
        val balanceSol = "%.4f".format(session.balanceLamports / 1_000_000_000.0)
        val balanceKnown = session.lastRefreshMs > 0L
        val syncState = chainSyncState(
            isOnline = session.isOnline,
            configLoaded = session.config != null,
            lastRefreshMs = session.lastRefreshMs,
        )

        ProfileUiState(
            walletAddress = session.walletAddress?.toBase58() ?: "",
            balanceSol = balanceSol,
            balanceKnown = balanceKnown,
            callsToday = session.profile?.effectiveCallsToday() ?: 0,
            maxCallsPerDay = session.config?.maxCallsPerDay ?: 0,
            totalCalls = session.profile?.totalCalls ?: 0,
            clusterName = session.cluster.cluster.displayLabel,
            networkFeePriority = networkFeePriority,
            appLanguage = appLanguage,
            hasProfile = session.profile != null,
            isLoggingOut = flags.isLoggingOut,
            isClosingProfile = flags.isClosingProfile,
            isTransactionInProgress = session.isTransactionInProgress,
            hasPendingTransactions = session.hasPendingTransactions,
            showCloseProfileDialog = flags.showCloseProfileDialog,
            showCloseSuccessDialog = flags.showCloseSuccessDialog,
            transactionState = globalTx.phase,
            transactionOrigin = globalTx.origin,
            message = message,
            isSuccess = isSuccess,
            canCloseProfile = session.profile != null && syncState == ChainSyncState.Ready,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState(
            walletAddress = appSession.state.value.walletAddress?.toBase58() ?: "",
            clusterName = appSession.state.value.cluster.cluster.displayLabel,
            networkFeePriority = runBlocking { preferencesStore.getNetworkFeePriority() },
            appLanguage = _appLanguage.value,
        )
    )

    fun disconnect() {
        if (_isLoggingOut.value) return
        if (appSession.state.value.isTransactionInProgress) return
        // Disconnect is an escape hatch; we allow it even if there are unresolved
        // pending transactions records. appSession.logout() will clear them.
        viewModelScope.launch {
            _isLoggingOut.value = true
            try {
                walletManager.disconnect()
                _logoutCompleted.emit(Unit)
            } finally {
                _isLoggingOut.value = false
            }
        }
    }

    fun requestCloseProfile() {
        if (_isClosingProfile.value || appSession.state.value.profile == null) return
        val syncState = chainSyncState(
            isOnline = appSession.state.value.isOnline,
            configLoaded = appSession.state.value.config != null,
            lastRefreshMs = appSession.state.value.lastRefreshMs,
        )
        if (syncState != ChainSyncState.Ready) {
            showError(AppError.ConfigNotLoaded.asUiText())
            return
        }
        _showCloseProfileDialog.value = true
    }

    fun retryChainSync() {
        viewModelScope.launch {
            dataRefreshCoordinator.refreshNow(force = true)
        }
    }

    fun dismissCloseProfileDialog() {
        _showCloseProfileDialog.value = false
    }

    fun dismissCloseSuccessDialog() {
        _showCloseSuccessDialog.value = false
    }

    fun confirmCloseProfile() {
        if (_isClosingProfile.value || !canStartTransaction(appSession, transactionRunner)) return
        _showCloseProfileDialog.value = false
        if (activityResultSenderRegistry.current() == null) {
            showError(AppError.WalletActivityUnavailable.asUiText())
            return
        }

        _isClosingProfile.value = true
        clearMessage()
        val started = transactionRunner.launch(Action.CloseUser, TransactionOrigin.Profile) { state ->
            when (state) {
                is TransactionState.Building -> clearMessage()
                is TransactionState.Confirmed -> {
                    _isClosingProfile.value = false
                    runCatching { dataRefreshCoordinator.refreshNow(force = true) }
                    _showCloseSuccessDialog.value = true
                }
                is TransactionState.Failed -> {
                    _isClosingProfile.value = false
                    showError(state.error.asUiText())
                }
                else -> Unit
            }
        }
        if (!started) {
            _isClosingProfile.value = false
        }
    }

    fun dismissMessage() {
        clearMessage()
    }

    fun setNetworkFeePriority(priority: NetworkFeePriority) {
        viewModelScope.launch {
            preferencesStore.saveNetworkFeePriority(priority)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        if (language == _appLanguage.value) return
        _appLanguage.value = language
        viewModelScope.launch {
            appLocaleManager.applyLanguage(language)
        }
    }

    private fun showError(text: UiText) {
        _isSuccess.value = false
        _message.value = text
    }

    private fun showSuccess(text: UiText) {
        _isSuccess.value = true
        _message.value = text
    }

    private fun clearMessage() {
        _message.value = null
        _isSuccess.value = false
    }
}
