package com.opencookie.app.domain.model

enum class Cluster { Devnet, MainnetBeta }

val Cluster.displayLabel: String
    get() = when (this) {
        Cluster.MainnetBeta -> "Mainnet"
        Cluster.Devnet -> "Devnet"
    }

data class ClusterConfig(
    val cluster: Cluster,
    val rpcEndpoint: String,
    val wsEndpoint: String,
    val explorerBaseUrl: String,
    val commitmentLevel: String,
) {
    companion object {
        fun devnet() = ClusterConfig(
            cluster = Cluster.Devnet,
            rpcEndpoint = "https://api.devnet.solana.com",
            wsEndpoint = "wss://api.devnet.solana.com",
            explorerBaseUrl = "https://explorer.solana.com",
            commitmentLevel = "confirmed",
        )

        fun mainnetBeta() = ClusterConfig(
            cluster = Cluster.MainnetBeta,
            rpcEndpoint = "https://api.mainnet-beta.solana.com",
            wsEndpoint = "wss://api.mainnet-beta.solana.com",
            explorerBaseUrl = "https://explorer.solana.com",
            commitmentLevel = "confirmed",
        )
    }
}

object ClusterDefaults {
    fun initialCluster(): ClusterConfig {
        return if (com.opencookie.app.BuildConfig.DEBUG &&
            com.opencookie.app.BuildConfig.DEFAULT_CLUSTER == "devnet"
        ) {
            ClusterConfig.devnet()
        } else {
            ClusterConfig.mainnetBeta()
        }
    }
}
