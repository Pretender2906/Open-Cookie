package com.opencookie.app.data.transaction

import com.opencookie.app.domain.model.TransactionOrigin
import com.opencookie.app.domain.model.TransactionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalTransactionUi @Inject constructor() {
    data class State(
        val phase: TransactionState = TransactionState.Idle,
        val origin: TransactionOrigin? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun begin(origin: TransactionOrigin) {
        _state.value = State(origin = origin)
    }

    fun updatePhase(phase: TransactionState) {
        _state.update { it.copy(phase = phase) }
    }

    fun reset() {
        _state.value = State()
    }

    companion object {
        fun isActivePhase(phase: TransactionState): Boolean =
            phase !is TransactionState.Idle &&
                phase !is TransactionState.Confirmed &&
                phase !is TransactionState.Failed
    }
}

fun canStartTransaction(
    appSession: com.opencookie.app.data.session.AppSession,
    transactionRunner: TransactionRunner,
): Boolean {
    if (transactionRunner.isActive) return false
    val session = appSession.state.value
    if (session.isTransactionInProgress) return false

    val pending = session.pendingTransactions
    if (pending.isNotEmpty()) {
        val now = System.currentTimeMillis()
        // If there are pending transactions that are NOT stale, we block to prevent duplicates.
        // Stale transactions (unresolved records) should not block the user forever.
        val hasActivePending = pending.any { now - it.createdAtMs <= com.opencookie.app.data.session.AppSession.PENDING_MAX_AGE_MS }
        if (hasActivePending) return false
    }

    return true
}
