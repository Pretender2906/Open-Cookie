package com.opencookie.app.domain.model

import com.opencookie.app.BuildConfig
import com.opencookie.app.R
import com.opencookie.app.util.UiText

sealed class AppError(cause: Throwable? = null) : Exception(cause) {
    abstract fun asUiText(): UiText

    data object NetworkUnavailable : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_network_unavailable)
    }

    data class RpcError(val code: Int, val detail: String) : AppError() {
        override fun asUiText() = if (detail.isNotBlank()) {
            UiText.StringResource(R.string.error_rpc_with_detail, detail)
        } else {
            UiText.StringResource(R.string.error_rpc_generic)
        }
    }

    data object RpcTimeout : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_rpc_timeout)
    }

    data object WalletRejected : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_wallet_rejected)
    }

    data object WalletNotFound : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_wallet_not_found)
    }

    data object WalletDisconnected : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_wallet_disconnected)
    }

    data object WalletSigningInterrupted : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_wallet_signing_interrupted)
    }

    data object WalletConnectTimeout : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_wallet_connect_timeout)
    }

    data object WalletActivityUnavailable : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_wallet_activity_unavailable)
    }

    data object WalletClusterMismatch : AppError() {
        override fun asUiText() = if (BuildConfig.DEBUG) {
            UiText.StringResource(R.string.error_wallet_cluster_mismatch_debug)
        } else {
            UiText.StringResource(R.string.error_wallet_cluster_mismatch_release)
        }
    }

    data object ConfigNotLoaded : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_config_not_loaded)
    }

    data object BlockhashExpired : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_blockhash_expired)
    }

    data object TransactionSimulationFailed : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_transaction_simulation_failed)
    }

    data object TransactionExpired : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_transaction_expired)
    }

    data object TransactionInProgress : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_transaction_in_progress)
    }

    data class ProgramError(val code: Int, val name: String) : AppError() {
        override fun asUiText() = when (code) {
            6001 -> UiText.StringResource(R.string.error_daily_limit_reached)
            6002 -> UiText.StringResource(R.string.error_unauthorized)
            6003 -> UiText.StringResource(R.string.error_invalid_pda)
            else -> UiText.StringResource(R.string.error_program_generic, code)
        }
    }

    data object ProfileNotFound : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_profile_not_found)
    }

    data object RevealMessageFailed : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_reveal_message_failed)
    }

    data object BlockchainError : AppError() {
        override fun asUiText() = UiText.StringResource(R.string.error_blockchain_generic)
    }

    data class Unknown(override val cause: Throwable?) : AppError(cause) {
        override fun asUiText() = UiText.StringResource(R.string.error_unknown)
    }

    val isSignRetryable: Boolean
        get() = this is BlockhashExpired || this is TransactionSimulationFailed

    // Compatibility property for logging or where string is strictly needed
    val userMessage: String
        get() = "AppError: ${this::class.simpleName}"
}
