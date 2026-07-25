package com.opencookie.app

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.opencookie.app.data.AppReadiness
import com.opencookie.app.data.DataRefreshCoordinator
import com.opencookie.app.data.cluster.ClusterManager
import com.opencookie.app.data.resilience.ConnectivityObserver
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.data.transaction.BlockhashCache
import com.opencookie.app.data.transaction.TransactionOrchestrator
import com.opencookie.app.data.wallet.WalletConnectionManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class OpenCookieApp : Application() {

    @Inject lateinit var appSession: AppSession
    @Inject lateinit var clusterManager: ClusterManager
    @Inject lateinit var walletConnectionManager: WalletConnectionManager
    @Inject lateinit var dataRefreshCoordinator: DataRefreshCoordinator
    @Inject lateinit var appReadiness: AppReadiness
    @Inject lateinit var blockhashCache: BlockhashCache
    @Inject lateinit var transactionOrchestrator: TransactionOrchestrator
    @Inject lateinit var connectivityObserver: ConnectivityObserver

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setApplicationLocales(AppCompatDelegate.getApplicationLocales())
        // Online flag before any RPC refresh.
        connectivityObserver.hashCode()

        appScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    appSession.restoreFromDisk()
                    walletConnectionManager.restoreSessionFromDisk()
                    clusterManager.applyClusterToRpc()
                    blockhashCache.invalidate()
                    transactionOrchestrator.recoverPendingOnLaunch()
                }
            } catch (e: Exception) {
                Log.e(TAG, "startup init failed", e)
            } finally {
                appReadiness.markReady()
            }
            dataRefreshCoordinator.start()
            runCatching { dataRefreshCoordinator.refreshNow(force = true) }
        }
    }

    companion object {
        private const val TAG = "OpenCookieApp"
    }
}
