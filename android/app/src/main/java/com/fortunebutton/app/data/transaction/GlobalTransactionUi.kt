package com.fortunebutton.app.data.transaction

import com.fortunebutton.app.domain.model.TransactionOrigin
import com.fortunebutton.app.domain.model.TransactionState
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
}

fun canStartTransaction(
    appSession: com.fortunebutton.app.data.session.AppSession,
    transactionRunner: TransactionRunner,
): Boolean = !transactionRunner.isActive && !appSession.state.value.isTransactionInProgress
