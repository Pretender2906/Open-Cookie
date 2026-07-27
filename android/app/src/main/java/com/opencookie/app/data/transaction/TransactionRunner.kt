package com.opencookie.app.data.transaction

import com.opencookie.app.di.ApplicationScope
import com.opencookie.app.domain.model.TransactionOrigin
import com.opencookie.app.domain.model.TransactionState
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

    @Volatile
    private var currentTransactionId: Int = 0

    val isActive: Boolean get() = activeJob?.isActive == true

    fun cancel() {
        currentTransactionId++
        val job = activeJob
        activeJob = null
        job?.cancel()
        globalTransactionUi.reset()
    }

    fun launch(
        action: Action,
        origin: TransactionOrigin,
        onState: suspend (TransactionState) -> Unit,
    ): Boolean {
        if (isActive) return false

        val transactionId = ++currentTransactionId

        activeJob = applicationScope.launch {
            globalTransactionUi.begin(origin)
            try {
                orchestrator.execute(action).collect { state ->
                    if (currentTransactionId == transactionId) {
                        withContext(Dispatchers.Main.immediate) {
                            globalTransactionUi.updatePhase(state)
                            onState(state)
                        }
                    }
                }
            } finally {
                if (currentTransactionId == transactionId) {
                    withContext(Dispatchers.Main.immediate) { globalTransactionUi.reset() }
                    activeJob = null
                }
            }
        }
        return true
    }
}
