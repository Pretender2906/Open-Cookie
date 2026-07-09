package com.fortunebutton.app.data.transaction

import com.fortunebutton.app.di.ApplicationScope
import com.fortunebutton.app.domain.model.TransactionOrigin
import com.fortunebutton.app.domain.model.TransactionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRunner @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val orchestrator: TransactionOrchestrator,
    private val globalTransactionUi: GlobalTransactionUi,
) {
    @Volatile
    private var activeJob: Job? = null

    val isActive: Boolean get() = activeJob?.isActive == true

    fun launch(
        action: Action,
        origin: TransactionOrigin,
        onState: suspend (TransactionState) -> Unit,
    ): Boolean {
        if (isActive) return false
        activeJob = applicationScope.launch {
            globalTransactionUi.begin(origin)
            try {
                orchestrator.execute(action).collect { state ->
                    withContext(Dispatchers.Main.immediate) {
                        globalTransactionUi.updatePhase(state)
                        onState(state)
                    }
                }
            } finally {
                withContext(Dispatchers.Main.immediate) { globalTransactionUi.reset() }
                activeJob = null
            }
        }
        return true
    }
}
