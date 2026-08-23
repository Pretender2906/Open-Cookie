package com.opencookie.app.data.wallet

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class WalletAbandonReason {
    ACTIVITY_RESUMED,
    HOST_REPLACED,
    HOST_DESTROYED,
}

@Singleton
class WalletInteractionTracker @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var interactionInProgress = false

    @Volatile
    private var abandonSignal: CompletableDeferred<Unit>? = null

    @Volatile
    private var transactStartedAtMs: Long = 0L

    @Volatile
    private var pendingAbandonJob: Job? = null

    @Volatile
    var lastAbandonReason: WalletAbandonReason? = null
        private set

    fun onTransactStarted() {
        cancelPendingAbandonTimer()
        interactionInProgress = true
        transactStartedAtMs = System.currentTimeMillis()
        abandonSignal = CompletableDeferred()
        lastAbandonReason = null
    }

    fun onTransactFinished() {
        cancelPendingAbandonTimer()
        interactionInProgress = false
        abandonSignal?.cancel()
        abandonSignal = null
        lastAbandonReason = null
    }

    fun onHostActivityReplaced() {
        if (!interactionInProgress) return
        scheduleAbandonAfterGrace(WalletAbandonReason.HOST_REPLACED)
    }

    suspend fun awaitAbandoned() {
        val signal = abandonSignal ?: return
        signal.await()
    }

    fun onActivityResumed() {
        if (!interactionInProgress) return
        scheduleAbandonAfterGrace(WalletAbandonReason.ACTIVITY_RESUMED)
    }

    fun onHostActivityDestroyed() {
        if (!interactionInProgress) return
        scheduleAbandonAfterGrace(WalletAbandonReason.HOST_DESTROYED)
    }

    private fun scheduleAbandonAfterGrace(trigger: WalletAbandonReason) {
        if (!interactionInProgress) return
        if (pendingAbandonJob?.isActive == true) return
        pendingAbandonJob = scope.launch {
            val ageAtSchedule = transactAgeMs()
            if (ageAtSchedule < MIN_TRANSACT_AGE_MS) {
                delay(MIN_TRANSACT_AGE_MS - ageAtSchedule)
            }
            if (!interactionInProgress) return@launch
            delay(ABANDON_GRACE_MS)
            if (!interactionInProgress) return@launch
            signalAbandonOnce(trigger)
        }
    }

    private fun cancelPendingAbandonTimer() {
        pendingAbandonJob?.cancel()
        pendingAbandonJob = null
    }

    private fun signalAbandonOnce(trigger: WalletAbandonReason) {
        val signal = abandonSignal ?: return
        if (signal.isCompleted) return
        lastAbandonReason = trigger
        Log.w(TAG, "abandon signaled trigger=$trigger")
        signal.complete(Unit)
    }

    private fun transactAgeMs(): Long =
        if (transactStartedAtMs == 0L) 0L else System.currentTimeMillis() - transactStartedAtMs

    companion object {
        private const val TAG = "WalletInteraction"
        private const val ABANDON_GRACE_MS = 3_000L
        private const val MIN_TRANSACT_AGE_MS = 1_000L
    }
}
