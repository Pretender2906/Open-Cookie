package com.fortunebutton.admin.domain.model

enum class Cluster { Devnet, MainnetBeta }

val Cluster.displayName: String
    get() = when (this) {
        Cluster.Devnet -> "Devnet"
        Cluster.MainnetBeta -> "Mainnet"
    }

sealed class AdminError(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause) {
    data object NetworkUnavailable : AdminError("Немає з'єднання з інтернетом")
    data class RpcError(val code: Int, val detail: String) :
        AdminError(if (detail.isNotBlank()) "RPC помилка: $detail" else "RPC помилка")
    data object RpcTimeout : AdminError("Час очікування RPC вичерпано")

    data object WalletRejected : AdminError("Гаманець відхилив запит")
    data object WalletNotFound : AdminError("Гаманець не знайдено")
    data object WalletDisconnected : AdminError("Гаманець відключено")
    data object WalletSigningInterrupted :
        AdminError("Підпис перервано — тримайте гаманець відкритим до завершення")
    data object WalletConnectTimeout :
        AdminError("Час підключення вичерпано — відкрийте гаманець і підтвердіть запит")
    data object WalletClusterMismatch :
        AdminError("Різні мережі в додатку та гаманці — узгодьте Devnet/Mainnet")

    data object BlockhashExpired : AdminError("Транзакція застаріла — спробуйте знову")
    data object TransactionExpired : AdminError("Транзакція не підтвердилась вчасно")
    data class ProgramError(val code: Int, val name: String) :
        AdminError(mapProgramError(code))
    data object Unauthorized : AdminError("Цей гаманець не є admin authority")
    data object ConfigNotFound : AdminError("Config PDA не знайдено — спочатку initialize_config")
    data class InvalidAddress(val input: String) : AdminError("Невірна адреса")
    data class Unknown(override val cause: Throwable?) :
        AdminError("Щось пішло не так: ${cause?.message ?: "невідома помилка"}", cause)
}

private fun mapProgramError(code: Int): String = when (code) {
    6014 -> "Немає прав admin"
    else -> "Помилка on-chain (код $code)"
}

enum class TxPhase {
    Idle,
    Building,
    AwaitingSignature,
    Confirming,
    Success,
    Failed,
}
