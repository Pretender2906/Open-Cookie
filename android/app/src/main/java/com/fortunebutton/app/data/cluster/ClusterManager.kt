package com.fortunebutton.app.data.cluster

import android.util.Log
import com.fortunebutton.app.data.local.PreferencesStore
import com.fortunebutton.app.data.rpc.RpcEndpointPool
import com.fortunebutton.app.data.rpc.SolanaRpcClient
import com.fortunebutton.app.data.session.AppSession
import com.fortunebutton.app.domain.model.Cluster
import com.fortunebutton.app.domain.model.ClusterConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClusterManager @Inject constructor(
    private val appSession: AppSession,
    private val preferencesStore: PreferencesStore,
    private val rpcClient: SolanaRpcClient,
) {
    val currentCluster: ClusterConfig get() = appSession.state.value.cluster

    suspend fun applyClusterToRpc() {
        val config = currentCluster
        rpcClient.cluster = config.cluster
        val saved = preferencesStore.getLastGoodRpcEndpoint(config.cluster)
        val endpoint = RpcEndpointPool.preferred(config.cluster, saved)
        rpcClient.rpcEndpoint = endpoint
        rpcClient.markEndpointRestored(endpoint)
        rpcClient.commitment = config.commitmentLevel
        Log.i(TAG, "RPC endpoint [${config.cluster}] -> $endpoint")
    }

    companion object {
        private const val TAG = "ClusterManager"
    }
}
