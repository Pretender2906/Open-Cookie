package com.opencookie.app.domain.model

sealed interface TransactionState {
    data object Idle : TransactionState
    data object Building : TransactionState
    data object AwaitingSignature : TransactionState
    data class Confirming(val signature: String) : TransactionState
    data object Retrying : TransactionState
    data class Confirmed(val signature: String) : TransactionState
    data class Failed(val error: AppError) : TransactionState
}

data class PendingTransaction(
    val signature: String,
    val action: String,
    val cluster: String,
    val createdAtMs: Long,
    val lastCheckedMs: Long,
    val hadProfile: Boolean = true,
    val walletAddress: String = "",
)

enum class TransactionOrigin {
    BreakCookie,
    Onboarding,
    Profile,
}
