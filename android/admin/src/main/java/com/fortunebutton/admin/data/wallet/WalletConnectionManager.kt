package com.fortunebutton.admin.data.wallet

import android.util.Base64
import android.util.Log
import com.fortunebutton.admin.data.rpc.RpcException
import com.fortunebutton.admin.data.rpc.SolanaRpcClient
import com.fortunebutton.admin.data.session.AdminSession
import com.fortunebutton.admin.domain.model.AdminError
import com.fortunebutton.admin.domain.model.Cluster
import com.fortunebutton.admin.util.PublicKey
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.Blockchain
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.Solana
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient
import com.solana.mobilewalletadapter.clientlib.scenario.Scenario
import com.solana.mobilewalletadapter.clientlib.successPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CancellationException as ConcurrentCancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletConnectionManager @Inject constructor(
    private val walletAdapter: MobileWalletAdapter,
    private val session: AdminSession,
    private val rpcClient: SolanaRpcClient,
) {
    companion object {
        private const val TAG = "FortuneAdminWallet"
        private const val TRANSACT_TIMEOUT_MS = Scenario.DEFAULT_CLIENT_TIMEOUT_MS + 15_000L
        private const val CONNECT_TIMEOUT_MS = Scenario.DEFAULT_CLIENT_TIMEOUT_MS + 45_000L
    }

    @Volatile
    private var appliedBlockchain: Blockchain? = null

    suspend fun restoreSessionFromDisk() {
        rpcClient.applyCluster(session.state.value.cluster)
        val state = session.state.value
        applyBlockchainFromSession()
        walletAdapter.applyPersistedSession(state.authToken, state.walletUriBase)
    }

    suspend fun connect(sender: ActivityResultSender, forceAuthorize: Boolean = false): Result<ConnectResult> {
        applyBlockchainFromSession()
        if (forceAuthorize) {
            walletAdapter.clearWalletSession()
        } else {
            val state = session.state.value
            walletAdapter.applyPersistedSession(state.authToken, state.walletUriBase)
        }

        return try {
            val result = transactConnect(sender)
            if (result is TransactionResult.Failure && !forceAuthorize && walletAdapter.authToken != null) {
                walletAdapter.applyPersistedSession(null, session.state.value.walletUriBase)
                processConnectResult(transactConnect(sender))
            } else {
                processConnectResult(result)
            }
        } catch (e: CancellationException) {
            Result.failure(WalletSignErrorMapper.fromException(e))
        } catch (e: ConcurrentCancellationException) {
            Result.failure(WalletSignErrorMapper.fromException(e))
        } catch (e: Exception) {
            Result.failure(WalletSignErrorMapper.fromException(e))
        }
    }

    private suspend fun transactConnect(
        sender: ActivityResultSender,
    ): TransactionResult<MobileWalletAdapterClient.AuthorizationResult> {
        return try {
            withTimeout(CONNECT_TIMEOUT_MS) {
                walletAdapter.transact(sender) { it }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "connect() timed out waiting for wallet authorize")
            TransactionResult.Failure(
                "Timed out waiting for wallet authorize",
                e,
            )
        }
    }

    private suspend fun processConnectResult(
        result: TransactionResult<MobileWalletAdapterClient.AuthorizationResult>,
    ): Result<ConnectResult> = when (result) {
        is TransactionResult.Success -> {
            val authResult = result.authResult
            val account = authResult.accounts.firstOrNull()
            val pubkey = account?.publicKey?.let { PublicKey(it) }
                ?: return Result.failure(AdminError.WalletNotFound)
            session.setWallet(pubkey, authResult.authToken, authResult.walletUriBase)
            Result.success(ConnectResult(pubkey, account.accountLabel))
        }
        is TransactionResult.NoWalletFound -> Result.failure(AdminError.WalletNotFound)
        is TransactionResult.Failure -> Result.failure(
            WalletSignErrorMapper.fromFailure(result.message, result.e),
        )
    }

    suspend fun signThenSend(
        sender: ActivityResultSender,
        txBytes: ByteArray,
        forceReauthorize: Boolean = false,
    ): Result<String> {
        return try {
            applyBlockchainFromSession()
            val state = session.state.value
            walletAdapter.applyPersistedSession(
                authToken = if (forceReauthorize) null else state.authToken,
                walletUriBase = state.walletUriBase,
            )
            val result = try {
                withTimeout(TRANSACT_TIMEOUT_MS) {
                    walletAdapter.transact(sender) {
                        @Suppress("DEPRECATION")
                        signTransactions(arrayOf(txBytes))
                    }
                }
            } catch (_: TimeoutCancellationException) {
                return Result.failure(AdminError.WalletSigningInterrupted)
            } catch (e: CancellationException) {
                return Result.failure(WalletSignErrorMapper.fromException(e))
            } catch (e: ConcurrentCancellationException) {
                return Result.failure(WalletSignErrorMapper.fromException(e))
            }

            when (result) {
                is TransactionResult.Success -> {
                    syncAuthFromResult(result.authResult)
                    val signedTx = result.successPayload?.signedPayloads?.firstOrNull()
                        ?: return Result.failure(AdminError.WalletRejected)
                    val signedTxBase64 = Base64.encodeToString(signedTx, Base64.NO_WRAP)
                    rpcClient.sendTransaction(signedTxBase64).fold(
                        onSuccess = { Result.success(it) },
                        onFailure = { e ->
                            Result.failure(
                                when (e) {
                                    is RpcException -> AdminError.RpcError(e.code, e.message ?: "send failed")
                                    is AdminError -> e
                                    else -> AdminError.Unknown(e)
                                },
                            )
                        },
                    )
                }
                is TransactionResult.NoWalletFound -> Result.failure(AdminError.WalletNotFound)
                is TransactionResult.Failure -> {
                    if (!forceReauthorize && session.state.value.authToken != null) {
                        Log.w(TAG, "signThenSend() auth likely stale; retrying with fresh authorize")
                        return signThenSend(sender, txBytes, forceReauthorize = true)
                    }
                    Result.failure(
                        WalletSignErrorMapper.fromFailure(result.message, result.e),
                    )
                }
            }
        } catch (e: CancellationException) {
            Result.failure(WalletSignErrorMapper.fromException(e))
        } catch (e: ConcurrentCancellationException) {
            Result.failure(WalletSignErrorMapper.fromException(e))
        } catch (e: Exception) {
            Result.failure(WalletSignErrorMapper.fromException(e))
        }
    }

    suspend fun disconnect(sender: ActivityResultSender) {
        applyBlockchainFromSession()
        val state = session.state.value
        walletAdapter.applyPersistedSession(state.authToken, state.walletUriBase)
        try {
            walletAdapter.disconnect(sender)
        } catch (_: Exception) {
        } finally {
            session.logout()
        }
    }

    suspend fun setCluster(cluster: Cluster) {
        session.setCluster(cluster)
        rpcClient.applyCluster(cluster)
        session.clearChainSnapshot()
        applyBlockchainFromSession()
    }

    data class ConnectResult(
        val publicKey: PublicKey,
        val accountLabel: String? = null,
    )

    private suspend fun syncAuthFromResult(authResult: MobileWalletAdapterClient.AuthorizationResult) {
        val account = authResult.accounts.firstOrNull() ?: return
        val pubkey = account.publicKey?.let { PublicKey(it) } ?: return
        val token = authResult.authToken ?: return
        Log.d(TAG, "syncAuthFromResult: persisting refreshed auth token")
        session.setWallet(pubkey, token, authResult.walletUriBase)
    }

    private fun applyBlockchainFromSession() {
        val target = when (session.state.value.cluster) {
            Cluster.Devnet -> Solana.Devnet
            Cluster.MainnetBeta -> Solana.Mainnet
        }
        if (appliedBlockchain != null && appliedBlockchain != target) {
            session.invalidateAuth()
        }
        walletAdapter.blockchain = target
        appliedBlockchain = target
    }
}
