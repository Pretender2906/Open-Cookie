package com.opencookie.admin.data.transaction

import com.opencookie.admin.data.program.AdminInstructionBuilder
import com.opencookie.admin.data.program.TransactionInstruction
import com.opencookie.admin.data.program.UpdateConfigParams
import com.opencookie.admin.data.rpc.SolanaRpcClient
import com.opencookie.admin.data.session.AdminSession
import com.opencookie.admin.data.wallet.WalletConnectionManager
import com.opencookie.admin.domain.model.AdminError
import com.opencookie.admin.util.PublicKey
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminTransactionExecutor @Inject constructor(
    private val walletManager: WalletConnectionManager,
    private val transactionFactory: SolanaTransactionFactory,
    private val blockhashCache: BlockhashCache,
    private val rpcClient: SolanaRpcClient,
    private val session: AdminSession,
) {
    suspend fun execute(
        sender: ActivityResultSender,
        instruction: TransactionInstruction,
        requireAdmin: Boolean = true,
    ): Result<String> {
        val wallet = session.state.value.walletAddress
            ?: return Result.failure(AdminError.WalletDisconnected)
        if (requireAdmin && !session.state.value.isAdminAuthorized) {
            return Result.failure(AdminError.Unauthorized)
        }

        val blockhash = blockhashCache.getFresh().blockhash
        val txBytes = transactionFactory.buildSerializedTransaction(instruction, wallet, blockhash)
        val signature = walletManager.signThenSend(sender, txBytes).getOrElse { return Result.failure(it) }
        return confirm(signature)
    }

    suspend fun updateConfig(
        sender: ActivityResultSender,
        params: UpdateConfigParams,
    ): Result<String> {
        val wallet = session.state.value.walletAddress
            ?: return Result.failure(AdminError.WalletDisconnected)
        val instruction = AdminInstructionBuilder.buildUpdateConfig(wallet, params)
        return execute(sender, instruction)
    }

    suspend fun acceptAdmin(sender: ActivityResultSender): Result<String> {
        val wallet = session.state.value.walletAddress
            ?: return Result.failure(AdminError.WalletDisconnected)
        if (!session.state.value.canAcceptAdmin) {
            return Result.failure(AdminError.Unauthorized)
        }
        val instruction = AdminInstructionBuilder.buildAcceptAdmin(wallet)
        return execute(sender, instruction, requireAdmin = false)
    }

    suspend fun withdrawTreasury(
        sender: ActivityResultSender,
        destination: PublicKey,
        lamports: Long,
    ): Result<String> {
        val wallet = session.state.value.walletAddress
            ?: return Result.failure(AdminError.WalletDisconnected)
        val instruction = AdminInstructionBuilder.buildWithdrawTreasury(wallet, destination, lamports)
        return execute(sender, instruction)
    }

    private suspend fun confirm(signature: String): Result<String> {
        var delayMs = 400L
        repeat(10) { attempt ->
            if (attempt > 0) delay(delayMs)
            val status = rpcClient.getSignatureStatuses(listOf(signature)).getOrNull()?.firstOrNull()
            if (status != null) {
                if (status.err != null) {
                    return Result.failure(extractProgramError(status.err.toString()))
                }
                if (status.confirmationStatus == "confirmed" || status.confirmationStatus == "finalized") {
                    return Result.success(signature)
                }
            }
            delayMs = (delayMs * 1.5).toLong().coerceAtMost(5_000L)
        }
        return Result.failure(AdminError.TransactionExpired)
    }

    private fun extractProgramError(errJson: String): AdminError {
        val customCode = Regex("""Custom["\s:]*(\d+)""")
            .find(errJson)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        return if (customCode != null) {
            AdminError.ProgramError(customCode, "Custom($customCode)")
        } else {
            AdminError.RpcError(-1, errJson)
        }
    }
}
