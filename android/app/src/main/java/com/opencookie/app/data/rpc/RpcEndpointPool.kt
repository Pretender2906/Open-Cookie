package com.opencookie.app.data.rpc

import com.opencookie.app.BuildConfig
import com.opencookie.app.domain.model.Cluster

object RpcEndpointPool {

    private const val DEVNET_PUBLIC = "https://api.devnet.solana.com"
    private const val DEVNET_ANKR = "https://rpc.ankr.com/solana_devnet"
    private const val MAINNET_PUBLIC = "https://api.mainnet-beta.solana.com"
    private const val MAINNET_DRPC = "https://solana.drpc.org"
    private const val MAINNET_PUBLICNODE = "https://solana-rpc.publicnode.com"

    fun endpoints(cluster: Cluster): List<String> = when (cluster) {
        Cluster.Devnet -> listOf(
            BuildConfig.DEVNET_RPC_URL,
            DEVNET_ANKR,
            DEVNET_PUBLIC,
        ).filter { it.isNotBlank() }.distinct()

        Cluster.MainnetBeta -> listOf(
            BuildConfig.MAINNET_RPC_URL,
            MAINNET_DRPC,
            MAINNET_PUBLICNODE,
            MAINNET_PUBLIC,
        ).filter { it.isNotBlank() }.distinct()
    }

    fun primary(cluster: Cluster): String = endpoints(cluster).first()

    fun preferred(cluster: Cluster, saved: String?): String {
        val pool = endpoints(cluster)
        if (saved != null && saved in pool) return saved
        return pool.first()
    }

    fun nextAfter(current: String, cluster: Cluster): String {
        val list = endpoints(cluster)
        val index = list.indexOf(current).takeIf { it >= 0 } ?: -1
        return list[(index + 1) % list.size]
    }
}
