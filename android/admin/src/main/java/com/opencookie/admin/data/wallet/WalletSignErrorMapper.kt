package com.opencookie.admin.data.wallet

import com.opencookie.admin.domain.model.AdminError
import com.solana.mobilewalletadapter.clientlib.protocol.JsonRpc20Client
import kotlinx.coroutines.CancellationException as CoroutineCancellationException
import java.util.concurrent.CancellationException as ConcurrentCancellationException

internal object WalletSignErrorMapper {
    fun fromFailure(walletMessage: String?, error: Throwable?): AdminError {
        val text = collectText(walletMessage, error)
        return when {
            isConnectTimeout(error, text) -> AdminError.WalletConnectTimeout
            isCancellation(error) || text.contains("cancelled", ignoreCase = true) ->
                AdminError.WalletSigningInterrupted
            text.contains("blockhash", ignoreCase = true) -> AdminError.BlockhashExpired
            text.contains("mainnet", ignoreCase = true) && text.contains("devnet", ignoreCase = true) ->
                AdminError.WalletClusterMismatch
            text.contains("declined", ignoreCase = true) -> AdminError.WalletRejected
            else -> AdminError.WalletRejected
        }
    }

    fun fromException(error: Throwable): AdminError {
        val text = collectText(error.message, error)
        return when {
            isConnectTimeout(error, text) -> AdminError.WalletConnectTimeout
            error is CoroutineCancellationException ||
                error is ConcurrentCancellationException ||
                isCancellation(error) ->
                AdminError.WalletSigningInterrupted
            text.contains("not found", ignoreCase = true) && text.contains("wallet", ignoreCase = true) ->
                AdminError.WalletNotFound
            else -> AdminError.Unknown(error)
        }
    }

    private fun isCancellation(error: Throwable?): Boolean {
        if (error == null) return false
        var current: Throwable? = error
        while (current != null) {
            if (current is CoroutineCancellationException || current is ConcurrentCancellationException) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun isConnectTimeout(error: Throwable?, text: String): Boolean =
        error is kotlinx.coroutines.TimeoutCancellationException ||
            text.contains("timed out waiting", ignoreCase = true) ||
            text.contains("Timed out while waiting", ignoreCase = true)

    private fun collectText(walletMessage: String?, error: Throwable?): String {
        val parts = mutableListOf<String>()
        walletMessage?.let { parts.add(it) }
        if (error != null) {
            var current: Throwable? = error
            while (current != null) {
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
