package com.opencookie.admin.data.rpc

import com.opencookie.admin.BuildConfig
import com.opencookie.admin.domain.model.Cluster

/** Free RPC mirrors per cluster — rotated on timeout / rate limit / auth errors. */
object RpcEndpointPool {

    private const val DEVNET_PUBLIC = "https://api.devnet.solana.com"

    private const val MAINNET_PUBLICNODE = "https://solana-rpc.publicnode.com"
    private const val MAINNET_PUBLIC = "https://api.mainnet-beta.solana.com"

    fun endpoints(cluster: Cluster): List<String> = when (cluster) {
        Cluster.Devnet -> listOf(
            BuildConfig.DEVNET_RPC_URL,
            DEVNET_PUBLIC,
        ).filter { it.isNotBlank() }.distinct()

        Cluster.MainnetBeta -> {
            val list = mutableListOf<String>()
            if (BuildConfig.MAINNET_ALCHEMY_URL.isNotBlank()) {
                list.add(BuildConfig.MAINNET_ALCHEMY_URL)
            }
            if (BuildConfig.MAINNET_HELIUS_URL.isNotBlank()) {
                list.add(BuildConfig.MAINNET_HELIUS_URL)
            }
            list.add(MAINNET_PUBLICNODE)
            list.add(MAINNET_PUBLIC)
            if (BuildConfig.MAINNET_RPC_URL.isNotBlank()) {
                list.add(BuildConfig.MAINNET_RPC_URL)
            }
            list.distinct()
        }
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
