package com.fortunebutton.app.data

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fortunebutton.app.data.session.AppSession
import com.fortunebutton.app.domain.repository.ProfileRepository
import com.fortunebutton.app.util.PublicKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRefreshCoordinator @Inject constructor(
    private val appSession: AppSession,
    private val profileRepository: ProfileRepository,
    private val appReadiness: AppReadiness,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private var pollingJob: Job? = null
    private var isForeground = false

    fun start(lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()) {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    isForeground = true
                    startPolling(immediate = true)
                }

                override fun onStop(owner: LifecycleOwner) {
                    isForeground = false
                    pollingJob?.cancel()
                }
            },
        )
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            isForeground = true
            startPolling(immediate = false)
        }
    }

    fun startPolling(immediate: Boolean = false) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            if (immediate) runCatching { refreshNow() }
            while (isActive && isForeground) {
                delay(REFRESH_INTERVAL_MS)
                runCatching { refreshNow() }
            }
        }
    }

    suspend fun refreshNow(force: Boolean = false) {
        refreshMutex.withLock { refreshOnce(force) }
    }

    suspend fun refreshAfterConnect() {
        refreshNow(force = true)
        repeat(2) {
            val session = appSession.state.value
            if (session.hasProfile || session.config == null) return
            delay(500)
            refreshNow(force = true)
        }
    }

    private suspend fun refreshOnce(force: Boolean) {
        appReadiness.awaitReady()
        appSession.pruneStalePendingTransactions()
        val session = appSession.state.value
        if (!session.isOnline) {
            appSession.markRefreshed()
            return
        }
        if (!force && session.isTransactionInProgress) return

        profileRepository.fetchConfig()
        val wallet = session.walletAddress
        if (wallet != null) {
            profileRepository.fetchProfile(wallet)
            profileRepository.fetchBalance(wallet)
        }
        appSession.markRefreshed()
        Log.d(TAG, "refresh complete config=${appSession.state.value.config != null} profile=${appSession.state.value.profile != null}")
    }

    companion object {
        private const val TAG = "DataRefresh"
        private const val REFRESH_INTERVAL_MS = 30_000L
    }
}
