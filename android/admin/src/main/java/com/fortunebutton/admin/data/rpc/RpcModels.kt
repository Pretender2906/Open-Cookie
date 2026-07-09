package com.fortunebutton.admin.data.rpc

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
    val id: JsonElement? = null,
    val result: JsonElement? = null,
    val error: JsonElement? = null,
)

@Serializable
data class RpcErrorBody(
    val code: Int,
    val message: String,
)

@Serializable
data class AccountInfoResult(val value: AccountInfoValue?)

@Serializable
data class AccountInfoValue(
    val data: List<String>,
    val lamports: Long = 0,
)

@Serializable
data class BlockhashResult(val value: BlockhashValue)

@Serializable
data class BlockhashValue(
    val blockhash: String,
    val lastValidBlockHeight: Long,
)

@Serializable
data class SignatureStatusResult(val value: List<SignatureStatusValue?>)

@Serializable
data class SignatureStatusValue(
    val err: JsonElement? = null,
    val confirmationStatus: String? = null,
)

@Serializable
data class BalanceResult(val value: Long)

class RpcException(val code: Int, message: String) : Exception(message)
