package com.opencookie.app.data

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.domain.repository.ProfileRepository
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
                    startPolling(appOpened = true)
                }

                override fun onStop(owner: LifecycleOwner) {
                    isForeground = false
                    pollingJob?.cancel()
                }
            },
        )
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            isForeground = true
            startPolling(appOpened = false)
        }
    }

    fun startPolling(appOpened: Boolean = false) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            if (appOpened) runCatching { refreshNow(appOpened = true) }
            while (isActive && isForeground) {
                delay(REFRESH_INTERVAL_MS)
                runCatching { refreshNow() }
            }
        }
    }

    suspend fun refreshNow(force: Boolean = false, appOpened: Boolean = force) {
        refreshMutex.withLock { refreshOnce(force, appOpened) }
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

    private suspend fun refreshOnce(force: Boolean, appOpened: Boolean) {
        appReadiness.awaitReady()
        appSession.pruneStalePendingTransactions()
        val session = appSession.state.value
        if (!session.isOnline) {
            appSession.markRefreshed()
            return
        }
        if (!force && !appOpened && session.isTransactionInProgress) return

        profileRepository.fetchConfig()
        val wallet = session.walletAddress
        if (wallet != null) {
            if (shouldRefreshProfile(session, force, appOpened)) {
                profileRepository.fetchProfile(wallet)
            }
            profileRepository.fetchBalance(wallet)
        }
        appSession.markRefreshed()
        Log.d(
            TAG,
            "refresh complete config=${appSession.state.value.config != null} " +
                "profile=${appSession.state.value.profile != null} " +
                "presence=${appSession.state.value.profilePresence}",
        )
    }

    private fun shouldRefreshProfile(
        session: AppSession.SessionState,
        force: Boolean,
        appOpened: Boolean,
    ): Boolean {
        if (force || appOpened || !session.isProfilePresenceKnown) return true
        if (session.lastProfileRefreshMs == 0L) return true
        val elapsedMs = System.currentTimeMillis() - session.lastProfileRefreshMs
        return elapsedMs >= PROFILE_REFRESH_INTERVAL_MS
    }

    companion object {
        private const val TAG = "DataRefresh"
        private const val REFRESH_INTERVAL_MS = 30_000L
        private const val PROFILE_REFRESH_INTERVAL_MS = 3_600_000L
    }
}
