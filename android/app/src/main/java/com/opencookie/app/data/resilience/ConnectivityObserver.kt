package com.opencookie.app.data.resilience

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.opencookie.app.data.DataRefreshCoordinator
import com.opencookie.app.data.session.AppSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
    private val appSession: AppSession,
    private val dataRefreshCoordinator: DataRefreshCoordinator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    init {
        val isCurrentlyOnline = connectivityManager?.let { cm ->
            val network = cm.activeNetwork ?: return@let false
            val capabilities = cm.getNetworkCapabilities(network) ?: return@let false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: true
        appSession.setOnline(isCurrentlyOnline)

        connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                appSession.setOnline(true)
                scope.launch {
                    runCatching { dataRefreshCoordinator.refreshNow(force = true) }
                }
            }

            override fun onLost(network: Network) {
                val isStillOnline = connectivityManager?.activeNetwork?.let { active ->
                    connectivityManager?.getNetworkCapabilities(active)
                        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } ?: false

                if (!isStillOnline) {
                    appSession.setOnline(false)
                }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet) {
                    appSession.setOnline(true)
                }
            }
        })
    }
}
