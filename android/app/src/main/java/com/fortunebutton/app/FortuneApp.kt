package com.fortunebutton.app

import android.app.Application
import android.util.Log
import com.fortunebutton.app.data.AppReadiness
import com.fortunebutton.app.data.DataRefreshCoordinator
import com.fortunebutton.app.data.cluster.ClusterManager
import com.fortunebutton.app.data.session.AppSession
import com.fortunebutton.app.data.transaction.BlockhashCache
import com.fortunebutton.app.data.wallet.WalletConnectionManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class FortuneApp : Application() {

    @Inject lateinit var appSession: AppSession
    @Inject lateinit var clusterManager: ClusterManager
    @Inject lateinit var walletConnectionManager: WalletConnectionManager
    @Inject lateinit var dataRefreshCoordinator: DataRefreshCoordinator
    @Inject lateinit var appReadiness: AppReadiness
    @Inject lateinit var blockhashCache: BlockhashCache

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    appSession.restoreFromDisk()
                    walletConnectionManager.restoreSessionFromDisk()
                    clusterManager.applyClusterToRpc()
                    blockhashCache.invalidate()
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
        private const val TAG = "FortuneApp"
    }
}
