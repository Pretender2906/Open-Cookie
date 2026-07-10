package com.opencookie.app.data.session

import android.net.Uri
import com.opencookie.app.BuildConfig
import com.opencookie.app.data.local.PendingTransactionCodec
import com.opencookie.app.data.local.PreferencesStore
import com.opencookie.app.domain.model.Cluster
import com.opencookie.app.domain.model.ClusterConfig
import com.opencookie.app.domain.model.ClusterDefaults
import com.opencookie.app.domain.model.PendingTransaction
import com.opencookie.app.domain.model.ProgramConfig
import com.opencookie.app.domain.model.UserProfile
import com.opencookie.app.util.PublicKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSession @Inject constructor(
    private val preferencesStore: PreferencesStore,
) {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    data class SessionState(
        val walletAddress: PublicKey? = null,
        val authToken: String? = null,
        val walletUriBase: Uri? = null,
        val cluster: ClusterConfig = ClusterDefaults.initialCluster(),
        val clusterLocked: Boolean = false,
        val profile: UserProfile? = null,
        val config: ProgramConfig? = null,
        val balanceLamports: Long = 0,
        val isTransactionInProgress: Boolean = false,
        val pendingTransactions: List<PendingTransaction> = emptyList(),
        val isOnline: Boolean = true,
        val lastRefreshMs: Long = 0,
        val isProfileInitialized: Boolean = false,
    ) {
        val isWalletConnected: Boolean get() = walletAddress != null
        val hasProfile: Boolean get() = profile != null || isProfileInitialized
        val hasPendingTransactions: Boolean get() = pendingTransactions.isNotEmpty()
    }

    fun updateProfile(profile: UserProfile?) {
        _state.update {
            it.copy(profile = profile, isProfileInitialized = profile != null || it.isProfileInitialized)
        }
    }

    fun clearProfile() {
        _state.update { it.copy(profile = null, isProfileInitialized = false) }
    }

    fun updateConfig(config: ProgramConfig) {
        _state.update { it.copy(config = config) }
    }

    fun setTransactionInProgress(inProgress: Boolean) {
        _state.update { it.copy(isTransactionInProgress = inProgress) }
    }

    fun updateBalance(lamports: Long) {
        _state.update { it.copy(balanceLamports = lamports) }
    }

    fun setWallet(address: PublicKey, authToken: String, walletUriBase: Uri? = null) {
        val boundUri = walletUriBase?.takeIf { it.scheme == "https" }
        _state.update {
            it.copy(
                walletAddress = address,
                authToken = authToken,
                walletUriBase = boundUri,
                profile = null,
                isProfileInitialized = false,
            )
        }
    }

    fun updateWalletSession(address: PublicKey, authToken: String, walletUriBase: Uri? = null) {
        val boundUri = walletUriBase?.takeIf { it.scheme == "https" }
        _state.update { it.copy(walletAddress = address, authToken = authToken, walletUriBase = boundUri) }
    }

    fun invalidateWalletAuthorization() {
        _state.update { it.copy(authToken = null) }
    }

    fun lockCluster() {
        _state.update { it.copy(clusterLocked = true) }
    }

    fun setCluster(clusterConfig: ClusterConfig) {
        _state.update { it.copy(cluster = clusterConfig) }
    }

    fun setOnline(online: Boolean) {
        _state.update { it.copy(isOnline = online) }
    }

    fun markRefreshed() {
        _state.update { it.copy(lastRefreshMs = System.currentTimeMillis()) }
    }

    suspend fun addPendingTransaction(tx: PendingTransaction) {
        _state.update { it.copy(pendingTransactions = it.pendingTransactions + tx) }
        persistPendingTransactionsToDisk()
    }

    suspend fun removePendingTransaction(signature: String) {
        _state.update { it.copy(pendingTransactions = it.pendingTransactions.filter { tx -> tx.signature != signature }) }
        persistPendingTransactionsToDisk()
    }

    suspend fun pruneStalePendingTransactions(maxAgeMs: Long = PENDING_MAX_AGE_MS) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        _state.update { it.copy(pendingTransactions = it.pendingTransactions.filter { tx -> tx.createdAtMs >= cutoff }) }
        persistPendingTransactionsToDisk()
    }

    private suspend fun persistPendingTransactionsToDisk() {
        preferencesStore.savePendingTransactions(PendingTransactionCodec.encode(_state.value.pendingTransactions))
    }

    suspend fun logout() {
        _state.update { SessionState(cluster = it.cluster, isOnline = it.isOnline) }
        preferencesStore.clearUserData()
    }

    suspend fun persistToDisk() {
        val s = _state.value
        s.walletAddress?.let { preferencesStore.saveWalletAddress(it.toBase58()) }
        s.authToken?.let { preferencesStore.saveAuthToken(it) }
        s.walletUriBase?.let { preferencesStore.saveWalletUriBase(it.toString()) }
        preferencesStore.saveCluster(s.cluster.cluster.name)
    }

    suspend fun restoreFromDisk() {
        val walletStr = preferencesStore.getWalletAddress()
        val authToken = preferencesStore.getAuthToken()
        val walletUriBaseStr = preferencesStore.getWalletUriBase()
        val clusterStr = preferencesStore.getCluster()

        val clusterConfig = if (!BuildConfig.DEBUG) {
            ClusterConfig.mainnetBeta()
        } else when (clusterStr) {
            Cluster.MainnetBeta.name -> ClusterConfig.mainnetBeta()
            Cluster.Devnet.name -> ClusterConfig.devnet()
            else -> ClusterDefaults.initialCluster()
        }

        val walletPubkey = walletStr?.let { runCatching { PublicKey(it) }.getOrNull() }
        val walletUriBase = walletUriBaseStr?.let {
            runCatching { Uri.parse(it) }.getOrNull()?.takeIf { uri -> uri.scheme == "https" }
        }

        _state.update {
            it.copy(
                walletAddress = walletPubkey,
                authToken = authToken,
                walletUriBase = walletUriBase,
                cluster = clusterConfig,
                clusterLocked = walletPubkey != null && !authToken.isNullOrBlank(),
            )
        }

        val raw = preferencesStore.getPendingTransactions()
        if (raw != null) {
            val decoded = PendingTransactionCodec.decode(raw).filter { tx ->
                walletStr != null && tx.walletAddress == walletStr && tx.cluster == clusterConfig.cluster.name
            }
            _state.update { it.copy(pendingTransactions = decoded) }
        }
    }

    companion object {
        const val PENDING_MAX_AGE_MS = 120_000L
    }
}
