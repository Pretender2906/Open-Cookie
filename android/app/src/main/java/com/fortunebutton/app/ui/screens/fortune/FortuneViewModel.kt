package com.fortunebutton.app.ui.screens.fortune

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortunebutton.app.data.AppReadiness
import com.fortunebutton.app.data.DataRefreshCoordinator
import com.fortunebutton.app.data.FortuneRepository
import com.fortunebutton.app.data.session.AppSession
import com.fortunebutton.app.data.transaction.Action
import com.fortunebutton.app.data.transaction.GlobalTransactionUi
import com.fortunebutton.app.data.transaction.TransactionOrchestrator
import com.fortunebutton.app.data.transaction.TransactionRunner
import com.fortunebutton.app.data.transaction.canStartTransaction
import com.fortunebutton.app.data.wallet.ActivityResultSenderRegistry
import com.fortunebutton.app.domain.model.AppError
import com.fortunebutton.app.domain.model.TransactionOrigin
import com.fortunebutton.app.domain.model.TransactionState
import com.fortunebutton.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FortuneUiState(
    val callsToday: Int = 0,
    val totalCalls: Long = 0,
    val fortuneMessage: String? = null,
    val transactionState: TransactionState = TransactionState.Idle,
    val isTransactionInProgress: Boolean = false,
    val configLoaded: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
    val buttonEnabled: Boolean = false,
)

@HiltViewModel
class FortuneViewModel @Inject constructor(
    private val appSession: AppSession,
    private val globalTransactionUi: GlobalTransactionUi,
    private val transactionRunner: TransactionRunner,
    private val transactionOrchestrator: TransactionOrchestrator,
    private val fortuneRepository: FortuneRepository,
    private val dataRefreshCoordinator: DataRefreshCoordinator,
    private val activityResultSenderRegistry: ActivityResultSenderRegistry,
    private val appReadiness: AppReadiness,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    private val _fortuneMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FortuneUiState> = combine(
        appSession.state,
        globalTransactionUi.state,
        _error,
        _fortuneMessage,
    ) { session, globalTx, error, fortuneMessage ->
        val profile = session.profile
        FortuneUiState(
            callsToday = profile?.callsToday ?: 0,
            totalCalls = profile?.totalCalls ?: 0,
            fortuneMessage = fortuneMessage,
            transactionState = globalTx.phase,
            isTransactionInProgress = session.isTransactionInProgress || transactionRunner.isActive,
            configLoaded = session.config != null,
            isOffline = !session.isOnline,
            error = error,
            buttonEnabled = session.config != null &&
                !session.isTransactionInProgress &&
                !transactionRunner.isActive &&
                activityResultSenderRegistry.current() != null &&
                (profile == null || profile.callsToday < UserProfile.MAX_CALLS_PER_DAY),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FortuneUiState())

    init {
        viewModelScope.launch {
            appReadiness.awaitReady()
            dataRefreshCoordinator.refreshNow(force = true)
        }
    }

    fun pressFortune() {
        if (!canStartTransaction(appSession, transactionRunner)) return
        _error.value = null
        val started = transactionRunner.launch(Action.Fortune, TransactionOrigin.Fortune) { state ->
            when (state) {
                is TransactionState.Confirmed -> {
                    viewModelScope.launch {
                        val result = transactionOrchestrator.fetchFortuneResult(state.signature)
                        result.onSuccess { fortune ->
                            _fortuneMessage.value = fortuneRepository.fortuneMessage(fortune.fortuneIndex)
                        }.onFailure {
                            _error.value = (it as? AppError)?.userMessage ?: "Could not read fortune"
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

    fun dismissFortune() {
        _fortuneMessage.value = null
    }
}
