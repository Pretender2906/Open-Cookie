package com.opencookie.app.data.rpc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String,
    val params: List<JsonElement> = emptyList(),
)

@Serializable
data class RpcResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: JsonElement? = null,
    val error: JsonElement? = null,
)

@Serializable
data class AccountInfoResult(
    val value: AccountInfoValue?,
)

@Serializable
data class AccountInfoValue(
    val data: List<String>,
    val executable: Boolean = false,
    val lamports: Long = 0,
    val owner: String = "",
)

@Serializable
data class BlockhashResult(
    val value: BlockhashValue,
)

@Serializable
data class BlockhashValue(
    val blockhash: String,
    val lastValidBlockHeight: Long,
)

@Serializable
data class SignatureStatusResult(
    val value: List<SignatureStatusValue?>,
)

@Serializable
data class SignatureStatusValue(
    val slot: Long? = null,
    val confirmations: Int? = null,
    val err: JsonElement? = null,
    val confirmationStatus: String? = null,
)

@Serializable
data class BalanceResult(
    val value: Long,
)

@Serializable
data class MultipleAccountsResult(
    val value: List<AccountInfoValue?>,
)

@Serializable
data class TransactionResult(
    val meta: TransactionMeta?,
)

@Serializable
data class TransactionMeta(
    val err: JsonElement? = null,
    val returnData: ReturnData? = null,
)

@Serializable
data class ReturnData(
    val programId: String,
    val data: List<String>,
)

@Serializable
data class GetTransactionResult(
    val transaction: JsonElement? = null,
    val meta: TransactionMeta? = null,
)

@Serializable
data class SimulateTransactionResult(
    val value: SimulateTransactionValue,
)

@Serializable
data class SimulateTransactionValue(
    val err: JsonElement? = null,
    val logs: List<String>? = null,
    val unitsConsumed: Long? = null,
)
