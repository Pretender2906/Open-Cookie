package com.fortunebutton.app.data.wallet

import com.fortunebutton.app.domain.model.AppError
import com.solana.mobilewalletadapter.clientlib.protocol.JsonRpc20Client
import kotlinx.coroutines.CancellationException as CoroutineCancellationException
import java.util.concurrent.CancellationException as ConcurrentCancellationException

internal object WalletSignErrorMapper {

    fun fromFailure(walletMessage: String?, error: Throwable?): AppError {
        val text = collectText(walletMessage, error)
        return when {
            isConnectTimeout(error, text) -> AppError.WalletConnectTimeout
            WalletCancellation.isCancellation(error) || isAssociationCancelled(text) ->
                AppError.WalletSigningInterrupted
            isBlockhashError(text) -> AppError.BlockhashExpired
            isSimulationError(text) -> AppError.TransactionSimulationFailed
            isClusterMismatch(text) -> AppError.WalletClusterMismatch
            text.contains("declined", ignoreCase = true) ||
                text.contains("not authorize", ignoreCase = true) ||
                text.contains("not signed", ignoreCase = true) ->
                AppError.WalletRejected
            else -> AppError.WalletRejected
        }
    }

    fun fromException(error: Throwable): AppError {
        val text = collectText(error.message, error)
        return when {
            isConnectTimeout(error, text) -> AppError.WalletConnectTimeout
            error is CoroutineCancellationException ||
                error is ConcurrentCancellationException ||
                WalletCancellation.isCancellation(error) ||
                isAssociationCancelled(text) ->
                AppError.WalletSigningInterrupted
            isBlockhashError(text) -> AppError.BlockhashExpired
            isSimulationError(text) -> AppError.TransactionSimulationFailed
            isClusterMismatch(text) -> AppError.WalletClusterMismatch
            text.contains("not found", ignoreCase = true) && text.contains("wallet", ignoreCase = true) ->
                AppError.WalletNotFound
            text.contains("declined", ignoreCase = true) ||
                text.contains("authorized", ignoreCase = true) ->
                AppError.WalletRejected
            else -> AppError.Unknown(error)
        }
    }

    private fun isBlockhashError(text: String): Boolean =
        text.contains("blockhash", ignoreCase = true) ||
            text.contains("BlockhashNotFound", ignoreCase = true)

    private fun isSimulationError(text: String): Boolean =
        text.contains("simulation failed", ignoreCase = true) ||
            text.contains("simulationFailed", ignoreCase = true)

    private fun isClusterMismatch(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains("mainnet") && lower.contains("devnet")) ||
            lower.contains("cluster mismatch", ignoreCase = true) ||
            lower.contains("network mismatch", ignoreCase = true)
    }

    private fun isConnectTimeout(error: Throwable?, text: String): Boolean =
        error is kotlinx.coroutines.TimeoutCancellationException ||
            text.contains("timed out waiting", ignoreCase = true)

    private fun isAssociationCancelled(text: String): Boolean =
        text.contains("cancelled before connected", ignoreCase = true) ||
            text.contains("association was cancelled", ignoreCase = true)

    private fun collectText(walletMessage: String?, error: Throwable?): String {
        val parts = mutableListOf<String>()
        walletMessage?.let { parts.add(it) }
        if (error != null) {
            var current: Throwable? = error
            val seen = mutableSetOf<Int>()
            while (current != null && System.identityHashCode(current) !in seen) {
                seen.add(System.identityHashCode(current))
                current.message?.let { parts.add(it) }
                if (current is JsonRpc20Client.JsonRpc20RemoteException) {
                    current.data?.let { parts.add(it) }
                }
                current = current.cause
            }
        }
        return parts.joinToString("\n")
    }
}
