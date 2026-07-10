package com.opencookie.app.data.wallet

import android.util.Base64
import android.util.Log
import com.opencookie.app.data.rpc.RpcException
import com.opencookie.app.data.rpc.SolanaRpcClient
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.domain.model.AppError
import com.opencookie.app.domain.model.Cluster
import com.opencookie.app.util.PublicKey
import com.solana.mobilewalletadapter.clientlib.Blockchain
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.Solana
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.protocol.JsonRpc20Client
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient
import com.solana.mobilewalletadapter.clientlib.scenario.Scenario
import com.solana.mobilewalletadapter.clientlib.successPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.join
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CancellationException as ConcurrentCancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletConnectionManager @Inject constructor(
    private val walletAdapter: MobileWalletAdapter,
    private val appSession: AppSession,
    private val rpcClient: SolanaRpcClient,
    private val walletInteractionTracker: WalletInteractionTracker,
    private val activityResultSenderRegistry: ActivityResultSenderRegistry,
) {
    private val deferredCleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var appliedBlockchain: Blockchain? = null

    fun syncWalletBlockchain() = applyBlockchainFromSession()

    fun restoreSessionFromDisk() {
        val session = appSession.state.value
        applyBlockchainFromSession()
        walletAdapter.applyPersistedSession(session.authToken, session.walletUriBase)
    }

    suspend fun connect(forceAuthorize: Boolean = false): Result<ConnectResult> {
        if (activityResultSenderRegistry.current() == null) {
            return Result.failure(AppError.WalletActivityUnavailable)
        }
        applyBlockchainFromSession()
        if (forceAuthorize) {
            walletAdapter.clearWalletSession()
        } else {
            val session = appSession.state.value
            walletAdapter.applyPersistedSession(session.authToken, session.walletUriBase)
        }

        return try {
            var result = transactConnect()
            if (result is TransactionResult.Failure && !forceAuthorize && walletAdapter.authToken != null) {
                walletAdapter.applyPersistedSession(null, appSession.state.value.walletUriBase)
                result = transactConnect()
            }
            processConnectResult(result)
        } catch (e: CancellationException) {
            Result.failure(mapWalletError(e))
        } catch (e: ConcurrentCancellationException) {
            Result.failure(mapWalletError(e))
        } catch (e: Exception) {
            Result.failure(mapWalletError(e))
        }
    }

    data class ConnectResult(val publicKey: PublicKey, val accountLabel: String? = null)

    suspend fun signTransaction(txBytes: ByteArray, feePayerBase58: String): Result<ByteArray> {
        return try {
            WalletFailureDiagnostics.logSignAndSendAttempt(txBytes, feePayerBase58)
            prepareWalletSession()
            var result = transactSign(txBytes) ?: return Result.failure(AppError.WalletSigningInterrupted)
            if (result is TransactionResult.Failure && walletAdapter.authToken != null && isAuthorizationError(result.e)) {
                appSession.invalidateWalletAuthorization()
                appSession.persistToDisk()
                walletAdapter.applyPersistedSession(null, appSession.state.value.walletUriBase)
                result = transactSign(txBytes) ?: return Result.failure(AppError.WalletSigningInterrupted)
            }
            when (result) {
                is TransactionResult.Success -> {
                    syncAuthFromResult(result.authResult)
                    val signedTx = result.successPayload?.signedPayloads?.firstOrNull()
                    if (signedTx.isNullOrEmpty()) Result.failure(AppError.WalletRejected)
                    else Result.success(signedTx)
                }
                is TransactionResult.NoWalletFound -> Result.failure(AppError.WalletNotFound)
                is TransactionResult.Failure -> {
                    WalletFailureDiagnostics.logSignAndSendFailure(result.message, result.e, txBytes)
                    Result.failure(WalletSignErrorMapper.fromFailure(result.message, result.e))
                }
            }
        } catch (e: CancellationException) {
            WalletFailureDiagnostics.logSignAndSendFailure(null, e, txBytes)
            Result.failure(mapWalletError(e))
        } catch (e: Exception) {
            WalletFailureDiagnostics.logSignAndSendFailure(null, e, txBytes)
            Result.failure(mapWalletError(e))
        }
    }

    suspend fun sendSignedTransaction(signedTx: ByteArray, unsignedTxBytes: ByteArray): Result<String> {
        val signedTxBase64 = Base64.encodeToString(signedTx, Base64.NO_WRAP)
        return rpcClient.sendTransaction(signedTxBase64).mapError { mapRpcSendError(it) }
            .also { result ->
                result.onSuccess { WalletFailureDiagnostics.logSignAndSendSuccess(it) }
                result.onFailure { WalletFailureDiagnostics.logSignAndSendFailure(null, it, signedTx) }
            }
    }

    suspend fun rebroadcastSignedTransaction(signedTx: ByteArray, unsignedTxBytes: ByteArray) {
        val signedTxBase64 = Base64.encodeToString(signedTx, Base64.NO_WRAP)
        Log.d(TAG, "rebroadcastSignedTransaction() signed tx size=${signedTx.size}")
        rpcClient.sendTransaction(signedTxBase64).fold(
            onSuccess = { signature -> Log.i(TAG, "rebroadcastSignedTransaction() ok signature=$signature") },
            onFailure = { e -> Log.w(TAG, "rebroadcastSignedTransaction() failed", e) },
        )
    }

    private fun prepareWalletSession() {
        applyBlockchainFromSession()
        val session = appSession.state.value
        walletAdapter.applyPersistedSession(session.authToken, session.walletUriBase)
    }

    private suspend fun transactConnect(): TransactionResult<MobileWalletAdapterClient.AuthorizationResult> {
        val sender = activityResultSenderRegistry.current()
            ?: return TransactionResult.Failure("Wallet activity unavailable", CancellationException("unavailable"))
        return runWalletTransact("connect") {
            withTimeout(CONNECT_TIMEOUT_MS) { walletAdapter.transact(sender) { it } }
        } ?: TransactionResult.Failure("Wallet connect interrupted", CancellationException("interrupted"))
    }

    private suspend fun transactSign(txBytes: ByteArray) = runWalletTransact("signTransaction") {
        val sender = activityResultSenderRegistry.current()
            ?: throw CancellationException("Wallet activity unavailable")
        withTimeout(TRANSACT_TIMEOUT_MS) {
            walletAdapter.transact(sender) {
                @Suppress("DEPRECATION")
                signTransactions(arrayOf(txBytes))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <T> runWalletTransact(label: String, block: suspend () -> T): T? {
        walletInteractionTracker.onTransactStarted()
        Log.d(TAG, "$label transact waiting (hardCapMs=$TRANSACT_HARD_CAP_MS)")
        val outcome = CompletableDeferred<T?>()
        val transactJob = CoroutineScope(currentCoroutineContext() + SupervisorJob()).launch {
            try {
                outcome.complete(block())
            } catch (e: CancellationException) {
                outcome.cancel(e)
            } catch (e: ConcurrentCancellationException) {
                outcome.cancel(CancellationException(e.message, e))
            } catch (e: Exception) {
                Log.e(TAG, "$label failed", e)
                if (!outcome.isCompleted) outcome.complete(null)
            }
        }
        var deferTransactJobCancel = false
        try {
            return coroutineScope {
                val abandonDeferred = async { walletInteractionTracker.awaitAbandoned() }
                try {
                    select {
                        outcome.onAwait {
                            Log.d(TAG, "$label select outcome=wallet_result")
                            it
                        }
                        abandonDeferred.onAwait {
                            val reason = walletInteractionTracker.lastAbandonReason
                            when (reason) {
                                WalletAbandonReason.ACTIVITY_RESUMED -> {
                                    deferTransactJobCancel = true
                                    Log.w(
                                        TAG,
                                        "$label select outcome=abandoned reason=$reason — deferring cleanup until transact job completes",
                                    )
                                    scheduleDeferredTransactCleanup(label, transactJob, outcome)
                                }
                                WalletAbandonReason.HOST_REPLACED,
                                WalletAbandonReason.HOST_DESTROYED,
                                -> {
                                    transactJob.cancel()
                                    Log.w(TAG, "$label select outcome=abandoned reason=$reason — immediate cleanup")
                                    cleanupWalletSessionAfterInterrupt("$label/abandon_$reason")
                                }
                                null -> {
                                    transactJob.cancel()
                                    Log.w(TAG, "$label select outcome=abandoned reason=unknown — immediate cleanup")
                                    cleanupWalletSessionAfterInterrupt("$label/abandon_unknown")
                                }
                            }
                            @Suppress("UNCHECKED_CAST")
                            null as T?
                        }
                        onTimeout(TRANSACT_HARD_CAP_MS) {
                            transactJob.cancel()
                            Log.w(TAG, "$label select outcome=hard_cap ${TRANSACT_HARD_CAP_MS}ms exceeded")
                            cleanupWalletSessionAfterInterrupt("$label/hard_cap")
                            @Suppress("UNCHECKED_CAST")
                            null as T?
                        }
                    }
                } finally {
                    abandonDeferred.cancel()
                }
            }
        } catch (e: CancellationException) {
            cleanupWalletSessionAfterInterrupt("$label/coroutine_cancel")
            throw e
        } catch (e: ConcurrentCancellationException) {
            cleanupWalletSessionAfterInterrupt("$label/concurrent_cancel")
            throw CancellationException(e.message, e)
        } finally {
            if (!deferTransactJobCancel) {
                transactJob.cancel()
            }
            walletInteractionTracker.onTransactFinished()
        }
    }

    private fun scheduleDeferredTransactCleanup(
        label: String,
        transactJob: Job,
        outcome: CompletableDeferred<*>,
    ) {
        deferredCleanupScope.launch {
            runDeferredTransactCleanup(label, transactJob, outcome)
        }
    }

    private suspend fun runDeferredTransactCleanup(
        label: String,
        transactJob: Job,
        outcome: CompletableDeferred<*>,
    ) {
        Log.d(TAG, "$label deferred cleanup — joining transact job")
        try {
            transactJob.join()
            val producedResult = when {
                outcome.isCancelled -> false
                !outcome.isCompleted -> false
                else -> outcome.await() != null
            }
            if (producedResult) {
                Log.d(TAG, "$label deferred cleanup — transact produced result, keeping adapter session")
            } else {
                Log.w(TAG, "$label deferred cleanup — transact finished without usable result, clearing session")
                cleanupWalletSessionAfterInterrupt("$label/deferred_resume_abandon")
            }
        } catch (e: CancellationException) {
            Log.w(TAG, "$label deferred cleanup cancelled while joining transact job", e)
            cleanupWalletSessionAfterInterrupt("$label/deferred_join_cancelled")
        } catch (e: Exception) {
            Log.w(TAG, "$label deferred cleanup failed while joining transact job", e)
            cleanupWalletSessionAfterInterrupt("$label/deferred_join_failed")
        } finally {
            if (transactJob.isActive) {
                transactJob.cancel()
            }
        }
    }

    private fun cleanupWalletSessionAfterInterrupt(cause: String) {
        runCatching {
            walletAdapter.clearWalletSession()
            Log.d(TAG, "wallet session cleared after interrupted transact cause=$cause")
        }.onFailure { e ->
            Log.w(TAG, "clearWalletSession after interrupt failed cause=$cause", e)
        }
    }

    private suspend fun processConnectResult(
        result: TransactionResult<MobileWalletAdapterClient.AuthorizationResult>,
    ): Result<ConnectResult> = when (result) {
        is TransactionResult.Success -> {
            val authResult = result.authResult
            val account = authResult.accounts.firstOrNull()
            val pubkey = account?.publicKey?.let { PublicKey(it) }
                ?: return Result.failure(AppError.WalletNotFound)
            persistConnectResult(pubkey, authResult)
            Result.success(ConnectResult(pubkey, account.accountLabel?.takeIf { it.isNotBlank() }))
        }
        is TransactionResult.NoWalletFound -> Result.failure(AppError.WalletNotFound)
        is TransactionResult.Failure -> Result.failure(WalletSignErrorMapper.fromFailure(result.message, result.e))
    }

    private suspend fun persistConnectResult(
        pubkey: PublicKey,
        authResult: MobileWalletAdapterClient.AuthorizationResult,
    ) {
        appSession.setWallet(pubkey, authResult.authToken, authResult.walletUriBase)
        appSession.lockCluster()
        appSession.persistToDisk()
    }

    private suspend fun syncAuthFromResult(authResult: MobileWalletAdapterClient.AuthorizationResult) {
        val pubkey = authResult.accounts.firstOrNull()?.publicKey?.let { PublicKey(it) } ?: return
        appSession.updateWalletSession(pubkey, authResult.authToken, authResult.walletUriBase)
        appSession.persistToDisk()
    }

    private fun applyBlockchainFromSession() {
        val target = when (appSession.state.value.cluster.cluster) {
            Cluster.Devnet -> Solana.Devnet
            Cluster.MainnetBeta -> Solana.Mainnet
        }
        if (appliedBlockchain != null && appliedBlockchain != target) {
            appSession.invalidateWalletAuthorization()
        }
        walletAdapter.blockchain = target
        appliedBlockchain = target
    }

    private fun isAuthorizationError(e: Throwable?): Boolean {
        var current = e
        val seen = mutableSetOf<Int>()
        while (current != null && System.identityHashCode(current) !in seen) {
            seen.add(System.identityHashCode(current))
            if (current is JsonRpc20Client.JsonRpc20RemoteException && current.code == -1) return true
            current = current.cause
        }
        return false
    }

    private fun <T> Result<T>.mapError(transform: (Throwable) -> AppError): Result<T> =
        fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(transform(it)) })

    private fun mapRpcSendError(e: Throwable): AppError = when (e) {
        is AppError -> e
        is RpcException -> SolanaRpcClient.mapToAppError(e)
        else -> AppError.Unknown(e)
    }

    private fun mapWalletError(e: Throwable): AppError = WalletSignErrorMapper.fromException(e)

    companion object {
        private const val TAG = "WalletConnection"
        private const val TRANSACT_HARD_CAP_MS = 100_000L
        private const val TRANSACT_TIMEOUT_MS = Scenario.DEFAULT_CLIENT_TIMEOUT_MS + 15_000L
        private const val CONNECT_TIMEOUT_MS = Scenario.DEFAULT_CLIENT_TIMEOUT_MS + 45_000L
    }
}
