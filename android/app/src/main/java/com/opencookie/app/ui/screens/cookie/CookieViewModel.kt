package com.opencookie.app.ui.screens.cookie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencookie.app.data.AppReadiness
import com.opencookie.app.data.DataRefreshCoordinator
import com.opencookie.app.data.CookieRepository
import com.opencookie.app.data.local.PreferencesStore
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.data.transaction.Action
import com.opencookie.app.data.transaction.GlobalTransactionUi
import com.opencookie.app.data.transaction.TransactionOrchestrator
import com.opencookie.app.data.transaction.TransactionRunner
import com.opencookie.app.data.transaction.canStartTransaction
import com.opencookie.app.data.wallet.ActivityResultSenderRegistry
import com.opencookie.app.domain.model.AppError
import com.opencookie.app.domain.model.TransactionOrigin
import com.opencookie.app.domain.model.TransactionState
import com.opencookie.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CookieUiState(
    val callsToday: Int = 0,
    val maxCallsPerDay: Int = 0,
    val totalCalls: Long = 0,
    val cookieMessage: String? = null,
    val transactionState: TransactionState = TransactionState.Idle,
    val isTransactionInProgress: Boolean = false,
    val configLoaded: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
    val buttonEnabled: Boolean = false,
    val showTapHint: Boolean = false,
    val costSol: String? = null,
)

@HiltViewModel
class CookieViewModel @Inject constructor(
    private val appSession: AppSession,
    private val globalTransactionUi: GlobalTransactionUi,
    private val transactionRunner: TransactionRunner,
    private val transactionOrchestrator: TransactionOrchestrator,
    private val cookieRepository: CookieRepository,
    private val dataRefreshCoordinator: DataRefreshCoordinator,
    private val activityResultSenderRegistry: ActivityResultSenderRegistry,
    private val appReadiness: AppReadiness,
    private val preferencesStore: PreferencesStore,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    private val _cookieMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CookieUiState> = combine(
        appSession.state,
        globalTransactionUi.state,
        _error,
        _cookieMessage,
        preferencesStore.cookieTapHintSeenFlow(),
    ) { session, globalTx, error, cookieMessage, tapHintSeen ->
        val profile = session.profile
        CookieUiState(
            callsToday = profile?.callsToday ?: 0,
            maxCallsPerDay = session.config?.maxCallsPerDay ?: 0,
            totalCalls = profile?.totalCalls ?: 0,
            cookieMessage = cookieMessage,
            transactionState = globalTx.phase,
            isTransactionInProgress = session.isTransactionInProgress || transactionRunner.isActive,
            configLoaded = session.config != null,
            isOffline = !session.isOnline,
            error = error,
            buttonEnabled = session.config != null &&
                !session.isTransactionInProgress &&
                !transactionRunner.isActive &&
                activityResultSenderRegistry.current() != null &&
                (profile == null || profile.callsToday < (session.config?.maxCallsPerDay ?: 0)),
            showTapHint = !tapHintSeen,
            costSol = session.config?.priceLamports?.let { formatSol(it) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CookieUiState())

    init {
        viewModelScope.launch {
            appReadiness.awaitReady()
            dataRefreshCoordinator.refreshNow(force = true)
        }
    }

    fun breakCookie() {
        if (!canStartTransaction(appSession, transactionRunner)) return
        _error.value = null
        val started = transactionRunner.launch(Action.BreakCookie, TransactionOrigin.BreakCookie) { state ->
            when (state) {
                is TransactionState.Confirmed -> {
                    viewModelScope.launch {
                        val result = transactionOrchestrator.fetchBreakCookieResult(state.signature)
                        result.onSuccess { cookieResult ->
                            _cookieMessage.value = cookieRepository.cookieMessage(cookieResult.messageIndex)
                        }.onFailure {
                            _error.value = (it as? AppError)?.userMessage ?: "Could not read cookie message"
                        }
                    }
                }
                is TransactionState.Failed -> _error.value = state.error.userMessage
                else -> Unit
            }
        }
        if (!started) _error.value = AppError.TransactionInProgress.userMessage
    }

    fun dismissError() {
        _error.value = null
    }

    fun dismissMessage() {
        _cookieMessage.value = null
    }

    fun markTapHintSeen() {
        viewModelScope.launch { preferencesStore.setCookieTapHintSeen() }
    }

    private companion object {
        fun formatSol(lamports: Long): String {
            if (lamports <= 0L) return "0"
            val sol = lamports.toDouble() / 1_000_000_000.0
            return String.format(java.util.Locale.US, "%.9f", sol)
                .trimEnd('0')
                .trimEnd('.')
        }
    }
}
