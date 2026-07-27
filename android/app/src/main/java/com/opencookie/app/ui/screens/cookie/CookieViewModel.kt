package com.opencookie.app.ui.screens.cookie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencookie.app.R
import com.opencookie.app.data.AppReadiness
import com.opencookie.app.data.DataRefreshCoordinator
import com.opencookie.app.data.CookieRepository
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.data.transaction.Action
import com.opencookie.app.data.transaction.GlobalTransactionUi
import com.opencookie.app.data.transaction.TransactionOrchestrator
import com.opencookie.app.data.transaction.TransactionRunner
import com.opencookie.app.data.transaction.canStartTransaction
import com.opencookie.app.data.wallet.ActivityResultSenderRegistry
import com.opencookie.app.domain.model.AppError
import com.opencookie.app.domain.model.ProfilePresence
import com.opencookie.app.domain.model.TransactionOrigin
import com.opencookie.app.domain.model.TransactionState
import com.opencookie.app.util.UiText
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
    val transactionOrigin: TransactionOrigin? = null,
    val isTransactionInProgress: Boolean = false,
    val isCookieOpeningInProgress: Boolean = false,
    val configLoaded: Boolean = false,
    val isOffline: Boolean = false,
    val error: UiText? = null,
    val buttonEnabled: Boolean = false,
    val profilePresence: ProfilePresence = ProfilePresence.Unknown,
    val showFirstLaunchOnboarding: Boolean = false,
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
) : ViewModel() {

    private val _error = MutableStateFlow<UiText?>(null)
    private val _cookieMessage = MutableStateFlow<String?>(null)
    private val _cookieOpeningPending = MutableStateFlow(false)
    private val cookieOpeningVisualState = combine(
        _error,
        _cookieMessage,
        _cookieOpeningPending,
    ) { error, cookieMessage, cookieOpeningPending ->
        CookieOpeningVisualState(error, cookieMessage, cookieOpeningPending)
    }

    val uiState: StateFlow<CookieUiState> = combine(
        appSession.state,
        globalTransactionUi.state,
        cookieOpeningVisualState,
    ) { session, globalTx, visualState ->
        val profile = session.profile
        val profileReady = session.isProfilePresenceKnown
        CookieUiState(
            callsToday = profile?.callsToday ?: 0,
            maxCallsPerDay = session.config?.maxCallsPerDay ?: 0,
            totalCalls = profile?.totalCalls ?: 0,
            cookieMessage = visualState.cookieMessage,
            transactionState = globalTx.phase,
            transactionOrigin = globalTx.origin,
            isTransactionInProgress = session.isTransactionInProgress || transactionRunner.isActive,
            isCookieOpeningInProgress = visualState.cookieOpeningPending,
            configLoaded = session.config != null,
            isOffline = !session.isOnline,
            error = visualState.error,
            buttonEnabled = session.config != null &&
                profileReady &&
                !session.isTransactionInProgress &&
                !transactionRunner.isActive &&
                !visualState.cookieOpeningPending &&
                activityResultSenderRegistry.current() != null &&
                (profile == null || profile.callsToday < (session.config?.maxCallsPerDay ?: 0)),
            profilePresence = session.profilePresence,
            showFirstLaunchOnboarding = session.profilePresence == ProfilePresence.NotExists,
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
        if (!canStartTransaction(appSession, transactionRunner)) {
            _error.value = AppError.TransactionInProgress.asUiText()
            _cookieOpeningPending.value = false
            return
        }
        _error.value = null
        val started = transactionRunner.launch(Action.BreakCookie, TransactionOrigin.BreakCookie) { state ->
            when (state) {
                is TransactionState.Confirmed -> {
                    viewModelScope.launch {
                        val result = transactionOrchestrator.fetchBreakCookieResult(state.signature)
                        result.onSuccess { cookieResult ->
                            appSession.applyBreakCookieStats(
                                totalCalls = cookieResult.totalCalls,
                                callsToday = cookieResult.callsToday,
                            )
                            _cookieMessage.value = cookieRepository.cookieMessage(cookieResult.messageIndex)
                            _cookieOpeningPending.value = false
                        }.onFailure {
                            _error.value = (it as? AppError)?.asUiText() ?: UiText.StringResource(R.string.error_read_cookie_failed)
                            _cookieOpeningPending.value = false
                        }
                    }
                }
                is TransactionState.Failed -> {
                    _error.value = state.error.asUiText()
                    _cookieOpeningPending.value = false
                }
                else -> Unit
            }
        }
        if (started) {
            _cookieOpeningPending.value = true
        } else {
            _error.value = AppError.TransactionInProgress.asUiText()
            _cookieOpeningPending.value = false
        }
    }

    fun cancelTransaction() {
        transactionRunner.cancel()
        _cookieOpeningPending.value = false
        _error.value = null
        _cookieMessage.value = null
    }

    fun dismissError() {
        _error.value = null
        _cookieOpeningPending.value = false
    }

    fun dismissMessage() {
        _cookieMessage.value = null
        _cookieOpeningPending.value = false
    }

    fun retryProfileCheck() {
        viewModelScope.launch {
            dataRefreshCoordinator.refreshNow(force = true)
        }
    }

    private companion object {
        data class CookieOpeningVisualState(
            val error: UiText?,
            val cookieMessage: String?,
            val cookieOpeningPending: Boolean,
        )

        fun formatSol(lamports: Long): String {
            if (lamports <= 0L) return "0"
            val sol = lamports.toDouble() / 1_000_000_000.0
            return String.format(java.util.Locale.US, "%.9f", sol)
                .trimEnd('0')
                .trimEnd('.')
        }
    }
}
