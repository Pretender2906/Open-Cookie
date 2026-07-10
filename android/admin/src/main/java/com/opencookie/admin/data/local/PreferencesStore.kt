package com.opencookie.admin.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.opencookie.admin.domain.model.Cluster
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun saveWalletAddress(address: String) =
        dataStore.edit { it[KEY_WALLET] = address }

    suspend fun getWalletAddress(): String? =
        dataStore.data.map { it[KEY_WALLET] }.first()

    suspend fun saveAuthToken(token: String) =
        dataStore.edit { it[KEY_AUTH_TOKEN] = token }

    suspend fun getAuthToken(): String? =
        dataStore.data.map { it[KEY_AUTH_TOKEN] }.first()

    suspend fun saveWalletUriBase(uri: String) =
        dataStore.edit { it[KEY_WALLET_URI_BASE] = uri }

    suspend fun getWalletUriBase(): String? =
        dataStore.data.map { it[KEY_WALLET_URI_BASE] }.first()

    suspend fun saveCluster(cluster: String) =
        dataStore.edit { it[KEY_CLUSTER] = cluster }

    suspend fun getCluster(): String? =
        dataStore.data.map { it[KEY_CLUSTER] }.first()

    suspend fun saveLastGoodRpcEndpoint(cluster: Cluster, endpoint: String) {
        dataStore.edit { it[lastGoodRpcKey(cluster)] = endpoint }
    }

    suspend fun getLastGoodRpcEndpoint(cluster: Cluster): String? =
        dataStore.data.map { it[lastGoodRpcKey(cluster)] }.first()

    suspend fun clearUserData() {
        dataStore.edit {
            it.remove(KEY_WALLET)
            it.remove(KEY_AUTH_TOKEN)
            it.remove(KEY_WALLET_URI_BASE)
        }
    }

    companion object {
        private val KEY_WALLET = stringPreferencesKey("wallet_address")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_WALLET_URI_BASE = stringPreferencesKey("wallet_uri_base")
        private val KEY_CLUSTER = stringPreferencesKey("cluster")
        private val KEY_LAST_GOOD_RPC_DEVNET = stringPreferencesKey("last_good_rpc_devnet")
        private val KEY_LAST_GOOD_RPC_MAINNET = stringPreferencesKey("last_good_rpc_mainnet")

        private fun lastGoodRpcKey(cluster: Cluster) = when (cluster) {
            Cluster.Devnet -> KEY_LAST_GOOD_RPC_DEVNET
            Cluster.MainnetBeta -> KEY_LAST_GOOD_RPC_MAINNET
        }
    }
}
