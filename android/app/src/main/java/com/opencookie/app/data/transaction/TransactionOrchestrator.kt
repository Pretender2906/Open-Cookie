package com.opencookie.app.data.transaction

import android.util.Log
import com.opencookie.app.data.program.InstructionBuilder
import com.opencookie.app.data.program.ResolvedAction
import com.opencookie.app.data.program.ReturnDataParser
import com.opencookie.app.data.rpc.SolanaRpcClient
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.data.wallet.WalletConnectionManager
import com.opencookie.app.data.wallet.WalletFailureDiagnostics
import com.opencookie.app.domain.model.AppError
import com.opencookie.app.domain.model.PendingTransaction
import com.opencookie.app.domain.model.ProfilePresence
import com.opencookie.app.domain.model.TransactionState
import com.opencookie.app.domain.model.UserProfile
import com.opencookie.app.domain.repository.ProfileRepository
import com.opencookie.app.util.PublicKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface Action {
    data object BreakCookie : Action
    data object InitializeUser : Action
    data object CloseUser : Action
}

data class BreakCookieTxResult(
    val signature: String,
    val messageIndex: Int,
)

@Singleton
class TransactionOrchestrator @Inject constructor(
    private val walletManager: WalletConnectionManager,
    private val instructionBuilder: InstructionBuilder,
    private val transactionFactory: SolanaTransactionFactory,
    private val computeBudgetPolicy: ComputeBudgetPolicy,
    private val blockhashCache: BlockhashCache,
    private val appSession: AppSession,
    private val profileRepository: ProfileRepository,
    private val rpcClient: SolanaRpcClient,
) {
    private val txMutex = Mutex()

    fun execute(action: Action): Flow<TransactionState> = flow {
        if (!txMutex.tryLock()) {
            emit(TransactionState.Failed(AppError.TransactionInProgress))
            return@flow
        }
        try {
            appSession.setTransactionInProgress(true)
            try {
                emit(TransactionState.Building)
                val session = appSession.state.value
                val wallet = session.walletAddress
                    ?: run {
                        emit(TransactionState.Failed(AppError.WalletDisconnected))
                        return@flow
                    }
                val config = session.config ?: run {
                    emit(TransactionState.Failed(AppError.ConfigNotLoaded))
                    return@flow
                }

                val resolvedAction = when (action) {
                    Action.BreakCookie -> ResolvedAction.BreakCookie
                    Action.InitializeUser -> ResolvedAction.InitializeUser
                    Action.CloseUser -> ResolvedAction.CloseUser
                }
                val hadProfile = hadProfileForAction(session, action)
                if (action is Action.CloseUser && !hadProfile) {
                    emit(TransactionState.Failed(AppError.ProfileNotFound))
                    return@flow
                }

                emit(TransactionState.AwaitingSignature)
                val signResult = signAndSend(resolvedAction, wallet, config, hadProfile) { state ->
                    emit(state)
                }
                if (signResult.isFailure) {
                    emit(TransactionState.Failed(signResult.exceptionOrNull() as? AppError ?: AppError.WalletRejected))
                    return@flow
                }

                val sent = signResult.getOrThrow()
                val pending = PendingTransaction(
                    signature = sent.signature,
                    action = actionToName(action),
                    cluster = session.cluster.cluster.name,
                    createdAtMs = System.currentTimeMillis(),
                    lastCheckedMs = System.currentTimeMillis(),
                    hadProfile = hadProfile,
                    walletAddress = wallet.toBase58(),
                )
                appSession.addPendingTransaction(pending)
                if (!hadProfile) applyOptimisticProfile(wallet)

                emit(TransactionState.Confirming(sent.signature))
                when (val confirm = confirmWithBackoff(sent)) {
                    ConfirmOutcome.Confirmed -> {
                        appSession.removePendingTransaction(sent.signature)
                        if (action is Action.CloseUser) {
                            onUserClosedAfterTx()
                        } else if (action is Action.BreakCookie) {
                            if (!hadProfile) {
                                appSession.markProfileCreatedLocally()
                            }
                            // Always refresh profile after BreakCookie to sync stats from chain
                            appSession.state.value.walletAddress?.let {
                                profileRepository.fetchProfile(it)
                            }
                        } else {
                            profileRepository.fetchProfile(wallet)
                        }
                        emit(TransactionState.Confirmed(sent.signature))
                    }
                    ConfirmOutcome.Expired -> {
                        appSession.removePendingTransaction(sent.signature)
                        if (!hadProfile) {
                            appSession.clearProfile()
                            appSession.setProfilePresence(ProfilePresence.NotExists)
                        }
                        emit(TransactionState.Failed(AppError.TransactionExpired))
                    }
                    is ConfirmOutcome.Error -> {
                        appSession.removePendingTransaction(sent.signature)
                        if (!hadProfile) {
                            appSession.clearProfile()
                            appSession.setProfilePresence(ProfilePresence.NotExists)
                        }
                        emit(TransactionState.Failed(confirm.error))
                    }
                }
            } catch (e: CancellationException) {
                emit(TransactionState.Failed(AppError.WalletSigningInterrupted))
            } catch (e: Exception) {
                emit(TransactionState.Failed((e as? AppError) ?: AppError.Unknown(e)))
            } finally {
                appSession.setTransactionInProgress(false)
            }
        } finally {
            txMutex.unlock()
        }
    }

    suspend fun recoverPendingOnLaunch() {
        // Initial pruning for clearly invalid entries (e.g. from future or extremely old)
        appSession.pruneStalePendingTransactions(maxAgeMs = AppSession.PENDING_MAX_AGE_MS)

        val pending = appSession.state.value.pendingTransactions
        if (pending.isEmpty()) return

        val sigs = pending.map { it.signature }
        // If RPC fails (null), we continue to evaluate age-based pruning for each transaction.
        val statuses = rpcClient.getSignatureStatuses(sigs, searchHistory = true).getOrNull()

        val now = System.currentTimeMillis()
        for ((i, tx) in pending.withIndex()) {
            val status = statuses?.getOrNull(i)
            if (status != null) {
                val confirmed = status.confirmationStatus == "confirmed" ||
                    status.confirmationStatus == "finalized"
                when {
                    confirmed && status.err == null -> {
                        applyRecoveredConfirmationSideEffects(tx)
                        appSession.removePendingTransaction(tx.signature)
                    }
                    status.err != null -> {
                        // Transaction failed on-chain, can be removed from local tracking
                        appSession.removePendingTransaction(tx.signature)
                    }
                }
            } else if (now - tx.createdAtMs > AppSession.PENDING_MAX_AGE_MS) {
                // Not found on-chain and exceeded maximum lifetime
                appSession.removePendingTransaction(tx.signature)
            }
        }
    }

    fun deriveBreakCookieResult(signature: String): BreakCookieTxResult {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(signature.toByteArray())
        // Use first 8 bytes for a stable large positive index
        val hashValue = ByteBuffer.wrap(hashBytes).order(ByteOrder.BIG_ENDIAN).long and Long.MAX_VALUE

        return BreakCookieTxResult(
            signature = signature,
            messageIndex = (hashValue % Int.MAX_VALUE).toInt(),
        )
    }

    private data class SentPayload(val signature: String, val signed: ByteArray, val unsigned: ByteArray)

    private suspend fun signAndSend(
        resolvedAction: ResolvedAction,
        wallet: PublicKey,
        config: com.opencookie.app.domain.model.ProgramConfig,
        hadProfile: Boolean,
        emitState: suspend (TransactionState) -> Unit,
    ): Result<SentPayload> {
        repeat(2) { attempt ->
            if (attempt > 0) {
                blockhashCache.invalidate()
                walletManager.syncWalletBlockchain()
                emitState(TransactionState.Retrying)
                delay(SIGN_RETRY_DELAY_MS)
            }
            val txBytes = buildTxBytes(resolvedAction, wallet, config, hadProfile).getOrElse {
                return Result.failure(it as? AppError ?: AppError.Unknown(it))
            }
            val signed = walletManager.signTransaction(txBytes, wallet.toBase58()).getOrElse {
                val error = it as? AppError ?: AppError.WalletRejected
                if (!error.isSignRetryable || attempt == 1) return Result.failure(error)
                return@repeat
            }
            val signature = walletManager.sendSignedTransaction(signed, txBytes).getOrElse {
                return Result.failure(it as? AppError ?: AppError.Unknown(it))
            }
            return Result.success(SentPayload(signature, signed, txBytes))
        }
        return Result.failure(AppError.WalletRejected)
    }

    private suspend fun buildTxBytes(
        resolvedAction: ResolvedAction,
        wallet: PublicKey,
        config: com.opencookie.app.domain.model.ProgramConfig,
        hadProfile: Boolean,
    ): Result<ByteArray> {
        val blockhash = try {
            blockhashCache.getFresh().blockhash
        } catch (e: Exception) {
            return Result.failure((e as? AppError) ?: AppError.Unknown(e))
        }
        walletManager.syncWalletBlockchain()
        val prependInitializeUser = !hadProfile &&
            resolvedAction is ResolvedAction.BreakCookie
        val isHeavy = computeBudgetPolicy.isHeavyTransaction(resolvedAction, prependInitializeUser)
        val instructions = instructionBuilder.buildInstructions(
            resolvedAction, wallet, prependInitializeUser,
        )
        val txBytes = transactionFactory.buildSerializedTransaction(
            instructions = instructions,
            feePayer = wallet,
            recentBlockhash = blockhash,
            isHeavy = isHeavy,
        )
        WalletFailureDiagnostics.preflightSimulation(rpcClient, txBytes, wallet).onFailure {
            return Result.failure(it)
        }
        return Result.success(txBytes)
    }

    private fun hadProfileForAction(
        session: AppSession.SessionState,
        action: Action,
    ): Boolean = when (action) {
        Action.CloseUser -> session.profile != null
        else -> session.hasProfile
    }

    private suspend fun onUserClosedAfterTx() {
        blockhashCache.invalidate()
        appSession.clearProfile()
        appSession.setProfilePresence(ProfilePresence.NotExists)
        appSession.markProfileRefreshed()
        appSession.pruneStalePendingTransactions()
        val wallet = appSession.state.value.walletAddress ?: return
        profileRepository.fetchBalance(wallet)
    }

    private suspend fun applyRecoveredConfirmationSideEffects(tx: PendingTransaction) {
        when (tx.action) {
            "close_user" -> onUserClosedAfterTx()
            else -> if (!tx.hadProfile) {
                val wallet = appSession.state.value.walletAddress ?: return
                profileRepository.fetchProfile(wallet)
            }
        }
    }

    private fun applyOptimisticProfile(wallet: PublicKey) {
        appSession.updateProfile(
            UserProfile(wallet, totalCalls = 0, lastDay = 0, callsToday = 0, bump = 0),
        )
    }

    private sealed interface ConfirmOutcome {
        data object Confirmed : ConfirmOutcome
        data object Expired : ConfirmOutcome
        data class Error(val error: AppError) : ConfirmOutcome
    }

    private suspend fun confirmWithBackoff(sent: SentPayload): ConfirmOutcome {
        val signature = sent.signature
        val startedMs = System.currentTimeMillis()
        val deadlineMs = startedMs + CONFIRM_DEADLINE_MS
        var delayMs = CONFIRM_INITIAL_DELAY_MS
        var pollAttempt = 0
        var rebroadcastCount = 0
        var lastRebroadcastMs = startedMs
        var historyLookupLogged = false

        while (System.currentTimeMillis() < deadlineMs) {
            if (pollAttempt > 0) {
                val remaining = deadlineMs - System.currentTimeMillis()
                if (remaining <= 0) break
                delay(delayMs.coerceAtMost(remaining))
            }

            if (pollAttempt > 0 &&
                rebroadcastCount < REBROADCAST_MAX &&
                System.currentTimeMillis() - lastRebroadcastMs >= REBROADCAST_INTERVAL_MS
            ) {
                rebroadcastCount++
                lastRebroadcastMs = System.currentTimeMillis()
                Log.d(TAG, "confirm rebroadcast $rebroadcastCount/$REBROADCAST_MAX signature=$signature")
                walletManager.rebroadcastSignedTransaction(sent.signed, sent.unsigned)
            }

            val elapsedMs = System.currentTimeMillis() - startedMs
            val searchHistory = elapsedMs >= CONFIRM_HISTORY_AFTER_MS
            if (searchHistory && !historyLookupLogged) {
                Log.d(TAG, "history lookup for $signature")
                historyLookupLogged = true
            }
            val status = rpcClient.getSignatureStatuses(
                listOf(signature),
                searchHistory = searchHistory,
            ).getOrNull()?.firstOrNull()

            if (status != null) {
                if (status.err != null) {
                    return ConfirmOutcome.Error(extractProgramError(status.err.toString()))
                }
                if (status.confirmationStatus == "confirmed" || status.confirmationStatus == "finalized") {
                    Log.d(
                        TAG,
                        "confirm success attempt=$pollAttempt elapsedMs=$elapsedMs " +
                            "searchHistory=$searchHistory rebroadcasts=$rebroadcastCount",
                    )
                    return ConfirmOutcome.Confirmed
                }
            }

            pollAttempt++
            delayMs = (delayMs * 1.5).toLong().coerceAtMost(CONFIRM_MAX_DELAY_MS)
        }

        Log.w(
            TAG,
            "confirm expired signature=$signature attempts=$pollAttempt rebroadcasts=$rebroadcastCount",
        )
        return ConfirmOutcome.Expired
    }

    private fun extractProgramError(errJson: String): AppError {
        val customCode = Regex("""Custom["\s:]*(\d+)""")
            .find(errJson)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        if (customCode != null) {
            return AppError.ProgramError(customCode, "Custom($customCode)")
        }
        val anchorConstraint = Regex("""InstructionError"\s*:\s*\[\s*\d+\s*,\s*"([^"]+)"""")
            .find(errJson)
            ?.groupValues
            ?.getOrNull(1)
        if (anchorConstraint == "ConstraintMut" || errJson.contains("ConstraintMut", ignoreCase = true)) {
            return AppError.ProgramError(2006, "ConstraintMut")
        }
        if (errJson.contains("SignatureVerification", ignoreCase = true)) {
            return AppError.WalletRejected
        }
        Log.e(TAG, "On-chain error: $errJson")
        return AppError.BlockchainError
    }

    private fun actionToName(action: Action): String = when (action) {
        Action.BreakCookie -> "break_cookie"
        Action.InitializeUser -> "initialize_user"
        Action.CloseUser -> "close_user"
    }

    companion object {
        private const val TAG = "TxOrchestrator"
        private const val CONFIRM_INITIAL_DELAY_MS = 400L
        private const val CONFIRM_MAX_DELAY_MS = 5_000L
        private const val CONFIRM_DEADLINE_MS = 50_000L
        private const val CONFIRM_HISTORY_AFTER_MS = 30_000L
        private const val REBROADCAST_INTERVAL_MS = 15_000L
        private const val REBROADCAST_MAX = 2
        private const val SIGN_RETRY_DELAY_MS = 400L
    }
}
