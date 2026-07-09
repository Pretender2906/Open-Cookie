package com.fortunebutton.app.data.transaction

import android.util.Log
import com.fortunebutton.app.data.program.InstructionBuilder
import com.fortunebutton.app.data.program.ResolvedAction
import com.fortunebutton.app.data.program.ReturnDataParser
import com.fortunebutton.app.data.session.AppSession
import com.fortunebutton.app.data.wallet.WalletConnectionManager
import com.fortunebutton.app.domain.model.AppError
import com.fortunebutton.app.domain.model.PendingTransaction
import com.fortunebutton.app.domain.model.TransactionState
import com.fortunebutton.app.domain.model.UserProfile
import com.fortunebutton.app.domain.repository.ProfileRepository
import com.fortunebutton.app.util.PublicKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

sealed interface Action {
    data object Fortune : Action
    data object InitializeUser : Action
}

data class FortuneTxResult(
    val signature: String,
    val fortuneIndex: Int,
    val totalCalls: Long,
    val callsToday: Int,
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
    private val rpcClient: com.fortunebutton.app.data.rpc.SolanaRpcClient,
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
                    Action.Fortune -> ResolvedAction.Fortune
                    Action.InitializeUser -> ResolvedAction.InitializeUser
                }
                val hadProfile = session.hasProfile

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
                        if (!hadProfile) profileRepository.fetchProfile(wallet)
                        else profileRepository.fetchProfile(wallet)
                        emit(TransactionState.Confirmed(sent.signature))
                    }
                    ConfirmOutcome.Expired -> {
                        appSession.removePendingTransaction(sent.signature)
                        if (!hadProfile) appSession.clearProfile()
                        emit(TransactionState.Failed(AppError.TransactionExpired))
                    }
                    is ConfirmOutcome.Error -> {
                        appSession.removePendingTransaction(sent.signature)
                        if (!hadProfile) appSession.clearProfile()
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

    suspend fun fetchFortuneResult(signature: String): Result<FortuneTxResult> {
        repeat(5) { attempt ->
            if (attempt > 0) delay(400L * attempt)
            val meta = rpcClient.getTransaction(signature).getOrNull()
            val returnData = meta?.returnData
            if (returnData != null) {
                val bytes = rpcClient.decodeReturnData(returnData) ?: continue
                val parsed = ReturnDataParser.parseFortuneResult(bytes)
                return Result.success(
                    FortuneTxResult(signature, parsed.fortuneIndex, parsed.totalCalls, parsed.callsToday),
                )
            }
        }
        return Result.failure(AppError.RpcError(-1, "Return data not available"))
    }

    private data class SentPayload(val signature: String, val signed: ByteArray, val unsigned: ByteArray)

    private suspend fun signAndSend(
        resolvedAction: ResolvedAction,
        wallet: PublicKey,
        config: com.fortunebutton.app.domain.model.ProgramConfig,
        hadProfile: Boolean,
        emitState: suspend (TransactionState) -> Unit,
    ): Result<SentPayload> {
        repeat(2) { attempt ->
            if (attempt > 0) {
                blockhashCache.invalidate()
                walletManager.syncWalletBlockchain()
                emitState(TransactionState.Retrying)
                delay(400)
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
        config: com.fortunebutton.app.domain.model.ProgramConfig,
        hadProfile: Boolean,
    ): Result<ByteArray> {
        val blockhash = try {
            blockhashCache.getFresh().blockhash
        } catch (e: Exception) {
            return Result.failure((e as? AppError) ?: AppError.Unknown(e))
        }
        walletManager.syncWalletBlockchain()
        val prependInitializeUser = !hadProfile && resolvedAction is ResolvedAction.Fortune
        val isHeavy = computeBudgetPolicy.isHeavyTransaction(resolvedAction, prependInitializeUser)
        val instructions = instructionBuilder.buildInstructions(
            resolvedAction, wallet, prependInitializeUser,
        )
        return Result.success(
            transactionFactory.buildSerializedTransaction(
                instructions = instructions,
                feePayer = wallet,
                recentBlockhash = blockhash,
                isHeavy = isHeavy,
            ),
        )
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
        val deadlineMs = System.currentTimeMillis() + 50_000L
        var delayMs = 400L
        while (System.currentTimeMillis() < deadlineMs) {
            val status = rpcClient.getSignatureStatuses(listOf(signature), searchHistory = true)
                .getOrNull()?.firstOrNull()
            if (status != null) {
                if (status.err != null) {
                    return ConfirmOutcome.Error(extractProgramError(status.err.toString()))
                }
                if (status.confirmationStatus == "confirmed" || status.confirmationStatus == "finalized") {
                    return ConfirmOutcome.Confirmed
                }
            }
            delay(delayMs)
            delayMs = (delayMs * 1.5).toLong().coerceAtMost(5_000L)
        }
        return ConfirmOutcome.Expired
    }

    private fun extractProgramError(errJson: String): AppError {
        val customCode = Regex("""Custom["\s:]*(\d+)""").find(errJson)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (customCode != null) return AppError.ProgramError(customCode, "Custom($customCode)")
        return AppError.RpcError(-1, "On-chain: $errJson")
    }

    private fun actionToName(action: Action): String = when (action) {
        Action.Fortune -> "fortune"
        Action.InitializeUser -> "initialize_user"
    }

    companion object {
        private const val TAG = "TxOrchestrator"
    }
}
