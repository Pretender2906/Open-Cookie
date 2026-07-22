package com.opencookie.app.domain.model

import com.opencookie.app.BuildConfig

sealed class AppError(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause) {
    data object NetworkUnavailable : AppError("No internet connection")
    data class RpcError(val code: Int, val detail: String) :
        AppError(if (detail.isNotBlank()) "Network error ($detail)" else "Network error, please try again")
    data object RpcTimeout : AppError("Request timed out, please retry")

    data object WalletRejected : AppError("Wallet request declined or session expired")
    data object WalletNotFound : AppError("No wallet app found")
    data object WalletDisconnected : AppError("Wallet disconnected")
    data object WalletSigningInterrupted :
        AppError("Wallet signing interrupted — keep the wallet open until approval completes")
    data object WalletConnectTimeout :
        AppError("Wallet connect timed out — open your wallet app and approve the request")
    data object WalletActivityUnavailable : AppError("App screen unavailable — reopen Open Cookie and try again")
    data object WalletClusterMismatch :
        AppError(
            if (BuildConfig.DEBUG) {
                "Wallet session was on another network — open Profile, log out, reconnect on Devnet"
            } else {
                "App and wallet are on different networks — set your wallet to Mainnet, then try again"
            },
        )
    data object ConfigNotLoaded : AppError("Chain data still loading — please wait a moment")

    data object BlockhashExpired : AppError("Transaction stale — tap again")
    data object TransactionSimulationFailed : AppError("Wallet simulation glitch — tap again")
    data object TransactionExpired : AppError("Network didn't confirm in time — tap to try again")
    data object TransactionInProgress : AppError("Another transaction is in progress")
    data class ProgramError(val code: Int, val name: String) : AppError(mapProgramError(code))

    data object ProfileNotFound : AppError("Profile not found on-chain")
    data class Unknown(override val cause: Throwable?) : AppError("Something went wrong", cause)

    val isSignRetryable: Boolean
        get() = this is BlockhashExpired || this is TransactionSimulationFailed
}

private fun mapProgramError(code: Int): String = when (code) {
    6001 -> "Daily limit reached — come back tomorrow"
    6002 -> "Unauthorized"
    6003 -> "Invalid PDA"
    else -> "Transaction failed (code $code)"
}
