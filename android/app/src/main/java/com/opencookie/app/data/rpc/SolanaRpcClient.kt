package com.opencookie.app.data.rpc

import android.util.Base64
import android.util.Log
import com.opencookie.app.data.local.PreferencesStore
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.domain.model.AppError
import com.opencookie.app.domain.model.Cluster
import com.opencookie.app.domain.model.ClusterDefaults
import com.opencookie.app.util.PublicKey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaRpcClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val preferencesStore: PreferencesStore,
    private val appSession: AppSession,
) {
    var rpcEndpoint: String = RpcEndpointPool.primary(ClusterDefaults.initialCluster().cluster)
    var commitment: String = "confirmed"
    var cluster: Cluster = ClusterDefaults.initialCluster().cluster

    private val retryPolicy = RpcRetryPolicy()
    private var lastPersistedCluster: Cluster? = null
    private var lastPersistedEndpoint: String? = null

    fun markEndpointRestored(endpoint: String) {
        lastPersistedCluster = cluster
        lastPersistedEndpoint = endpoint
    }

    suspend fun getAccountInfo(pubkey: PublicKey): Result<AccountInfoValue?> =
        withRetry {
            val params = JsonArray(listOf(
                JsonPrimitive(pubkey.toBase58()),
                JsonObject(mapOf(
                    "encoding" to JsonPrimitive("base64"),
                    "commitment" to JsonPrimitive(commitment),
                )),
            ))
            val response = rpcCall("getAccountInfo", params)
            json.decodeFromJsonElement<AccountInfoResult>(response).value
        }

    suspend fun getBalance(pubkey: PublicKey): Result<Long> =
        withRetry {
            val params = JsonArray(listOf(
                JsonPrimitive(pubkey.toBase58()),
                JsonObject(mapOf("commitment" to JsonPrimitive(commitment))),
            ))
            val response = rpcCall("getBalance", params)
            json.decodeFromJsonElement<BalanceResult>(response).value
        }

    suspend fun getLatestBlockhash(): Result<BlockhashValue> =
        withRetry {
            val params = JsonArray(listOf(
                JsonObject(mapOf("commitment" to JsonPrimitive(commitment))),
            ))
            val response = rpcCall("getLatestBlockhash", params)
            json.decodeFromJsonElement<BlockhashResult>(response).value
        }

    suspend fun sendTransaction(signedTxBase64: String): Result<String> =
        withRetry {
            val params = JsonArray(listOf(
                JsonPrimitive(signedTxBase64),
                JsonObject(mapOf(
                    "encoding" to JsonPrimitive("base64"),
                    "skipPreflight" to JsonPrimitive(true),
                    "maxRetries" to JsonPrimitive(3),
                )),
            ))
            val response = rpcCall("sendTransaction", params)
            response.jsonPrimitive.content
        }

    data class SimulateDiagnostics(
        val value: SimulateTransactionValue,
        val rawResultJson: String,
    )

    suspend fun simulateTransactionDiagnostics(txBase64: String): Result<SimulateDiagnostics> =
        withRetry {
            val params = JsonArray(listOf(
                JsonPrimitive(txBase64),
                JsonObject(mapOf(
                    "encoding" to JsonPrimitive("base64"),
                    "commitment" to JsonPrimitive(commitment),
                    "sigVerify" to JsonPrimitive(false),
                )),
            ))
            val result = rpcCall("simulateTransaction", params)
            val value = json.decodeFromJsonElement<SimulateTransactionResult>(result).value
            SimulateDiagnostics(value, json.encodeToString(JsonElement.serializer(), result))
        }

    suspend fun getSignatureStatuses(
        signatures: List<String>,
        searchHistory: Boolean = false,
    ): Result<List<SignatureStatusValue?>> =
        withRetry {
            val sigsArray = JsonArray(signatures.map { JsonPrimitive(it) })
            val params = JsonArray(listOf(
                sigsArray,
                JsonObject(mapOf("searchTransactionHistory" to JsonPrimitive(searchHistory))),
            ))
            val response = rpcCall("getSignatureStatuses", params)
            json.decodeFromJsonElement<SignatureStatusResult>(response).value
        }

    suspend fun getTransaction(signature: String): Result<TransactionMeta?> =
        withRetry {
            val params = JsonArray(listOf(
                JsonPrimitive(signature),
                JsonObject(mapOf(
                    "encoding" to JsonPrimitive("json"),
                    "commitment" to JsonPrimitive(commitment),
                    "maxSupportedTransactionVersion" to JsonPrimitive(0),
                )),
            ))
            val response = rpcCall("getTransaction", params)
            json.decodeFromJsonElement<GetTransactionResult>(response).meta
        }

    suspend fun accountExists(pubkey: PublicKey): Result<Boolean> =
        getAccountInfo(pubkey).map { it != null }

    fun decodeAccountData(value: AccountInfoValue): ByteArray? {
        if (value.data.isEmpty()) return null
        return Base64.decode(value.data[0], Base64.DEFAULT)
    }

    fun decodeReturnData(returnData: ReturnData): ByteArray? {
        if (returnData.data.isEmpty()) return null
        return Base64.decode(returnData.data[0], Base64.DEFAULT)
    }

    private suspend fun rpcCall(method: String, params: JsonArray): JsonElement =
        rpcCallWithRawBody(method, params).first

    private suspend fun rpcCallWithRawBody(method: String, params: JsonArray): Pair<JsonElement, String> {
        val request = RpcRequest(method = method, params = params.toList())
        val response = try {
            httpClient.post(rpcEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(RpcRequest.serializer(), request))
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP POST FAILED: ${e.message}", e)
            throw e
        }

        val responseBody = response.body<String>()
        val rpcResponse = json.decodeFromString(RpcResponse.serializer(), responseBody)

        if (rpcResponse.error != null) {
            val errorElement = rpcResponse.error
            val (code, message) = if (errorElement is JsonObject) {
                val c = errorElement["code"]?.jsonPrimitive?.intOrNull ?: -1
                val m = errorElement["message"]?.jsonPrimitive?.content ?: errorElement.toString()
                c to m
            } else {
                -1 to errorElement.toString()
            }
            throw RpcException(code, message)
        }

        val result = rpcResponse.result ?: throw RpcException(-1, "Null result")
        appSession.setOnline(true)
        persistLastGoodEndpointIfNeeded()
        return result to responseBody
    }

    private suspend fun persistLastGoodEndpointIfNeeded() {
        if (lastPersistedCluster == cluster && lastPersistedEndpoint == rpcEndpoint) return
        if (rpcEndpoint !in RpcEndpointPool.endpoints(cluster)) return
        preferencesStore.saveLastGoodRpcEndpoint(cluster, rpcEndpoint)
        lastPersistedCluster = cluster
        lastPersistedEndpoint = rpcEndpoint
    }

    private suspend fun <T> withRetry(block: suspend () -> T): Result<T> {
        var lastException: Throwable? = null
        for (attempt in 0..retryPolicy.maxRetries) {
            try {
                return Result.success(block())
            } catch (e: RpcException) {
                lastException = e
                if (shouldFailoverRpc(e)) rotateEndpointIfApplicable()
                if (!retryPolicy.isRetryable(e.code) || attempt == retryPolicy.maxRetries) break
                delay(retryPolicy.delayForAttempt(attempt))
            } catch (e: SocketTimeoutException) {
                lastException = e
                rotateEndpointIfApplicable()
                if (attempt == retryPolicy.maxRetries) break
                delay(retryPolicy.delayForAttempt(attempt))
            } catch (e: SerializationException) {
                lastException = e
                Log.w(TAG, "Serialization error on $rpcEndpoint, rotating...")
                rotateEndpointIfApplicable()
                if (attempt == retryPolicy.maxRetries) break
                delay(retryPolicy.delayForAttempt(attempt))
            } catch (e: IOException) {
                lastException = e
                rotateEndpointIfApplicable()
                if (attempt == retryPolicy.maxRetries) break
                delay(retryPolicy.delayForAttempt(attempt))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                return Result.failure(mapToAppError(e))
            }
        }
        return Result.failure(mapToAppError(lastException ?: Exception("Unknown RPC error")))
    }

    private fun shouldFailoverRpc(error: RpcException): Boolean {
        if (error.code == -32000 || error.code == -1 || error.code == 403) return true
        val message = error.message.orEmpty()
        return message.contains("Unauthorized", ignoreCase = true) ||
            message.contains("rate limit", ignoreCase = true)
    }

    private fun rotateEndpointIfApplicable() {
        val pool = RpcEndpointPool.endpoints(cluster)
        if (rpcEndpoint !in pool) return
        val next = RpcEndpointPool.nextAfter(rpcEndpoint, cluster)
        if (next == rpcEndpoint) return
        rpcEndpoint = next
        Log.w(TAG, "RPC failover [$cluster] -> $rpcEndpoint")
    }

    companion object {
        private const val TAG = "SolanaRpcClient"

        fun mapToAppError(e: Throwable): AppError = when (e) {
            is RpcException -> {
                if (e.message?.contains("BlockhashNotFound") == true) {
                    AppError.BlockhashExpired
                } else {
                    AppError.RpcError(e.code, e.message ?: "RPC error")
                }
            }
            is SocketTimeoutException -> AppError.RpcTimeout
            is IOException -> AppError.NetworkUnavailable
            is AppError -> e
            else -> AppError.Unknown(e)
        }
    }
}

class RpcException(val code: Int, message: String) : Exception(message)
