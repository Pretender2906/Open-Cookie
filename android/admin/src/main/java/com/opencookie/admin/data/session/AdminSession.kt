package com.opencookie.admin.data.session

import android.net.Uri
import com.opencookie.admin.data.local.PreferencesStore
import com.opencookie.admin.data.program.ProtocolConfig
import com.opencookie.admin.data.rpc.SolanaRpcClient
import com.opencookie.admin.domain.model.Cluster
import com.opencookie.admin.util.PublicKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminSession @Inject constructor(
    private val preferencesStore: PreferencesStore,
) {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    data class SessionState(
        val walletAddress: PublicKey? = null,
        val authToken: String? = null,
        val walletUriBase: Uri? = null,
        val cluster: Cluster = SolanaRpcClient.initialCluster(),
        val config: ProtocolConfig? = null,
        val treasuryBalanceLamports: Long = 0,
        val walletBalanceLamports: Long = 0,
        val isAdminAuthorized: Boolean = false,
        val canAcceptAdmin: Boolean = false,
    ) {
        val isWalletConnected: Boolean
            get() = walletAddress != null && !authToken.isNullOrBlank()
    }

    suspend fun restoreFromDisk() {
        val wallet = preferencesStore.getWalletAddress()?.let { PublicKey(it) }
        val token = preferencesStore.getAuthToken()
        val walletUriBase = preferencesStore.getWalletUriBase()?.let { uri ->
            runCatching { Uri.parse(uri) }.getOrNull()?.takeIf { it.scheme == "https" }
        }
        val cluster = preferencesStore.getCluster()?.let { name ->
            runCatching { Cluster.valueOf(name) }.getOrNull()
        } ?: SolanaRpcClient.initialCluster()
        _state.update {
            it.copy(
                walletAddress = wallet,
                authToken = token,
                walletUriBase = walletUriBase,
                cluster = cluster,
            )
        }
    }

    suspend fun setWallet(pubkey: PublicKey, authToken: String, walletUriBase: Uri? = null) {
        val boundUri = walletUriBase?.takeIf { it.scheme == "https" }
        _state.update { it.copy(walletAddress = pubkey, authToken = authToken, walletUriBase = boundUri) }
        preferencesStore.saveWalletAddress(pubkey.toBase58())
        preferencesStore.saveAuthToken(authToken)
        boundUri?.let { preferencesStore.saveWalletUriBase(it.toString()) }
    }

    suspend fun logout() {
        _state.update {
            SessionState(cluster = it.cluster)
        }
        preferencesStore.clearUserData()
    }

    suspend fun setCluster(cluster: Cluster) {
        _state.update { it.copy(cluster = cluster) }
        preferencesStore.saveCluster(cluster.name)
    }

    fun invalidateAuth() {
        _state.update { it.copy(authToken = null) }
    }

    fun clearChainSnapshot() {
        updateChainSnapshot(
            config = null,
            treasuryBalanceLamports = 0,
            walletBalanceLamports = 0,
        )
    }

    fun updateChainSnapshot(
        config: ProtocolConfig?,
        treasuryBalanceLamports: Long,
        walletBalanceLamports: Long,
    ) {
        val wallet = _state.value.walletAddress
        val isAdmin = wallet != null && config != null && wallet == config.adminAuthority
        val canAccept = wallet != null && config != null &&
            config.hasPendingAdmin && wallet == config.pendingAdmin

        _state.update {
            it.copy(
                config = config,
                treasuryBalanceLamports = treasuryBalanceLamports,
                walletBalanceLamports = walletBalanceLamports,
                isAdminAuthorized = isAdmin,
                canAcceptAdmin = canAccept,
            )
        }
    }
}
