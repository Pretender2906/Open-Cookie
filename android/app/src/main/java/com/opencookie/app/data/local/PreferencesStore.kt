package com.opencookie.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.opencookie.app.domain.model.Cluster
import com.opencookie.app.domain.model.NetworkFeePriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun saveWalletAddress(address: String) =
        dataStore.edit { it[KEY_WALLET_ADDRESS] = address }

    suspend fun getWalletAddress(): String? =
        dataStore.data.map { it[KEY_WALLET_ADDRESS] }.first()

    suspend fun saveAuthToken(token: String) =
        dataStore.edit { it[KEY_AUTH_TOKEN] = token }

    suspend fun getAuthToken(): String? =
        dataStore.data.map { it[KEY_AUTH_TOKEN] }.first()

    suspend fun saveWalletAuthCluster(cluster: String) =
        dataStore.edit { it[KEY_WALLET_AUTH_CLUSTER] = cluster }

    suspend fun getWalletAuthCluster(): String? =
        dataStore.data.map { it[KEY_WALLET_AUTH_CLUSTER] }.first()

    suspend fun saveCluster(cluster: String) =
        dataStore.edit { it[KEY_CLUSTER] = cluster }

    suspend fun getCluster(): String? =
        dataStore.data.map { it[KEY_CLUSTER] }.first()

    suspend fun savePendingTransactions(json: String) =
        dataStore.edit { it[KEY_PENDING_TXS] = json }

    suspend fun getPendingTransactions(): String? =
        dataStore.data.map { it[KEY_PENDING_TXS] }.first()

    suspend fun saveWalletUriBase(uri: String) =
        dataStore.edit { it[KEY_WALLET_URI_BASE] = uri }

    suspend fun getWalletUriBase(): String? =
        dataStore.data.map { it[KEY_WALLET_URI_BASE] }.first()

    suspend fun saveLastGoodRpcEndpoint(cluster: Cluster, endpoint: String) =
        dataStore.edit { it[lastGoodRpcKey(cluster)] = endpoint }

    suspend fun getLastGoodRpcEndpoint(cluster: Cluster): String? =
        dataStore.data.map { it[lastGoodRpcKey(cluster)] }.first()

    fun networkFeePriorityFlow(): Flow<NetworkFeePriority> =
        dataStore.data.map { prefs ->
            NetworkFeePriority.fromStored(prefs[KEY_NETWORK_FEE_PRIORITY])
        }

    suspend fun saveNetworkFeePriority(priority: NetworkFeePriority) =
        dataStore.edit { it[KEY_NETWORK_FEE_PRIORITY] = priority.toStoredValue() }

    suspend fun getNetworkFeePriority(): NetworkFeePriority =
        networkFeePriorityFlow().first()

    suspend fun clearWalletAuth() {
        dataStore.edit {
            it.remove(KEY_AUTH_TOKEN)
            it.remove(KEY_WALLET_AUTH_CLUSTER)
        }
    }

    suspend fun clearUserData() {
        dataStore.edit {
            it.remove(KEY_WALLET_ADDRESS)
            it.remove(KEY_AUTH_TOKEN)
            it.remove(KEY_WALLET_AUTH_CLUSTER)
            it.remove(KEY_WALLET_URI_BASE)
            it.remove(KEY_PENDING_TXS)
        }
    }

    companion object {
        private val KEY_WALLET_ADDRESS = stringPreferencesKey("wallet_address")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_WALLET_AUTH_CLUSTER = stringPreferencesKey("wallet_auth_cluster")
        private val KEY_WALLET_URI_BASE = stringPreferencesKey("wallet_uri_base")
        private val KEY_CLUSTER = stringPreferencesKey("cluster")
        private val KEY_PENDING_TXS = stringPreferencesKey("pending_txs")
        private val KEY_NETWORK_FEE_PRIORITY = stringPreferencesKey("network_fee_priority")
        private val KEY_LAST_GOOD_RPC_DEVNET = stringPreferencesKey("last_good_rpc_devnet")
        private val KEY_LAST_GOOD_RPC_MAINNET = stringPreferencesKey("last_good_rpc_mainnet")

        private fun lastGoodRpcKey(cluster: Cluster) = when (cluster) {
            Cluster.Devnet -> KEY_LAST_GOOD_RPC_DEVNET
            Cluster.MainnetBeta -> KEY_LAST_GOOD_RPC_MAINNET
        }
    }
}
