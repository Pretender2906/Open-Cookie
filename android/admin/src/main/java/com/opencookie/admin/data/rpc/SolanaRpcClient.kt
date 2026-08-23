package com.opencookie.admin.data.rpc

import android.util.Base64
import android.util.Log
import com.opencookie.admin.BuildConfig
import com.opencookie.admin.data.local.PreferencesStore
import com.opencookie.admin.domain.model.AdminError
import com.opencookie.admin.domain.model.Cluster
import com.opencookie.admin.util.PublicKey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
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
) {
    var cluster: Cluster = initialCluster()
    var rpcEndpoint: String = RpcEndpointPool.primary(initialCluster())
    var commitment: String = "confirmed"

    private val retryPolicy = RpcRetryPolicy()
    private var lastPersistedCluster: Cluster? = null
    private var lastPersistedEndpoint: String? = null

    fun markEndpointRestored(endpoint: String) {
        lastPersistedCluster = cluster
        lastPersistedEndpoint = endpoint
    }

    suspend fun applyCluster(newCluster: Cluster) {
        cluster = newCluster
        val saved = preferencesStore.getLastGoodRpcEndpoint(newCluster)
        rpcEndpoint = RpcEndpointPool.preferred(newCluster, saved)
        markEndpointRestored(rpcEndpoint)
        Log.i(TAG, "RPC endpoint [$newCluster] -> $rpcEndpoint")
    }

    suspend fun getAccountInfo(pubkey: PublicKey): Result<AccountInfoValue?> = withRetry {
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

    suspend fun getBalance(pubkey: PublicKey): Result<Long> = withRetry {
        val params = JsonArray(listOf(
            JsonPrimitive(pubkey.toBase58()),
            JsonObject(mapOf("commitment" to JsonPrimitive(commitment))),
        ))
        val response = rpcCall("getBalance", params)
        json.decodeFromJsonElement<BalanceResult>(response).value
    }

    suspend fun getLatestBlockhash(): Result<BlockhashValue> = withRetry {
        val params = JsonArray(listOf(
            JsonObject(mapOf("commitment" to JsonPrimitive(commitment))),
        ))
        val response = rpcCall("getLatestBlockhash", params)
        json.decodeFromJsonElement<BlockhashResult>(response).value
    }

    suspend fun sendTransaction(signedTxBase64: String): Result<String> = withRetry {
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

    suspend fun getSignatureStatuses(signatures: List<String>): Result<List<SignatureStatusValue?>> =
        withRetry {
            val params = JsonArray(listOf(
                JsonArray(signatures.map { JsonPrimitive(it) }),
                JsonObject(mapOf("searchTransactionHistory" to JsonPrimitive(true))),
            ))
            val response = rpcCall("getSignatureStatuses", params)
            json.decodeFromJsonElement<SignatureStatusResult>(response).value
        }

    fun decodeAccountData(value: AccountInfoValue): ByteArray? {
        if (value.data.isEmpty()) return null
        return Base64.decode(value.data[0], Base64.DEFAULT)
    }

    private suspend fun rpcCall(method: String, params: JsonArray): JsonElement {
        val request = RpcRequest(method = method, params = params.toList())
        val requestBody = json.encodeToString(RpcRequest.serializer(), request)

        Log.d(TAG, ">>> RPC Request to ${maskEndpoint(rpcEndpoint)}: $method")

        val response = httpClient.post(rpcEndpoint) {
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
            header("User-Agent", "Mozilla/5.0 (Linux; Android 14) OpenCookieAdmin/1.0")
            header("Origin", "https://open-cookie.pages.dev")
            setBody(requestBody)
        }

        val responseBody = response.body<String>()

        if (!response.status.isSuccess()) {
            Log.e(TAG, "<<< HTTP ERROR [${maskEndpoint(rpcEndpoint)}]: ${response.status.value}")
            throw RpcException(response.status.value, "HTTP ${response.status.value}: $responseBody")
        }
        if (responseBody.isBlank()) {
            throw RpcException(-1, "Empty response from RPC")
        }

        val rpcResponse = try {
            json.decodeFromString(RpcResponse.serializer(), responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "<<< JSON DECODE FAILED [${maskEndpoint(rpcEndpoint)}]")
            throw RpcException(-1, "Invalid JSON")
        }

        if (rpcResponse.error != null) {
            val errorElement = rpcResponse.error
            val (code, message) = if (errorElement is JsonObject) {
                val c = errorElement["code"]?.jsonPrimitive?.intOrNull ?: -1
                val m = errorElement["message"]?.jsonPrimitive?.content ?: errorElement.toString()
                c to m
            } else {
                -1 to errorElement.toString()
            }
            Log.e(TAG, "<<< RPC ERROR [${maskEndpoint(rpcEndpoint)}]: $code - $message")
            throw RpcException(code, message)
        }
        val result = rpcResponse.result ?: throw RpcException(-1, "Null result")
        persistLastGoodEndpointIfNeeded()
        return result
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
                val failover = shouldFailoverRpc(e)
                Log.w(TAG, "Attempt $attempt failed on ${maskEndpoint(rpcEndpoint)}: RPC $e (failover=$failover)")
                if (failover) rotateEndpointIfApplicable()
                if (!retryPolicy.isRetryable(e.code) || (attempt == retryPolicy.maxRetries && e.code != -1)) break
                delay(retryPolicy.delayForAttempt(attempt))
            } catch (e: ResponseException) {
                lastException = e
                Log.w(TAG, "Attempt $attempt HTTP error on ${maskEndpoint(rpcEndpoint)}, rotating...")
                rotateEndpointIfApplicable()
                if (attempt == retryPolicy.maxRetries) break
                delay(retryPolicy.delayForAttempt(attempt))
            } catch (e: SocketTimeoutException) {
                lastException = e
                Log.w(TAG, "Attempt $attempt timeout on ${maskEndpoint(rpcEndpoint)}, rotating...")
                rotateEndpointIfApplicable()
                if (attempt == retryPolicy.maxRetries) break
                delay(retryPolicy.delayForAttempt(attempt))
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "Attempt $attempt IO error on ${maskEndpoint(rpcEndpoint)}: ${e.message}, rotating...")
                rotateEndpointIfApplicable()
                if (attempt == retryPolicy.maxRetries) break
                delay(retryPolicy.delayForAttempt(attempt))
            } catch (e: SerializationException) {
                lastException = e
                Log.w(TAG, "Attempt $attempt serialization error on ${maskEndpoint(rpcEndpoint)}, rotating...")
                rotateEndpointIfApplicable()
                if (attempt == retryPolicy.maxRetries) break
                delay(retryPolicy.delayForAttempt(attempt))
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "Unexpected error on ${maskEndpoint(rpcEndpoint)}: ${e.message}", e)
                rotateEndpointIfApplicable()
                if (attempt == retryPolicy.maxRetries) break
                delay(retryPolicy.delayForAttempt(attempt))
            }
        }
        return Result.failure(mapError(lastException ?: Exception("RPC error")))
    }

    private fun shouldFailoverRpc(error: RpcException): Boolean {
        if (error.code == -32000 || error.code == -1 || error.code == 403 || error.code == 429 || error.code == 35) return true
        val message = error.message.orEmpty()
        return message.contains("Unauthorized", ignoreCase = true) ||
            message.contains("rate limit", ignoreCase = true) ||
            message.contains("chain is not available", ignoreCase = true)
    }

    private fun rotateEndpointIfApplicable() {
        val pool = RpcEndpointPool.endpoints(cluster)
        if (rpcEndpoint !in pool) return
        val next = RpcEndpointPool.nextAfter(rpcEndpoint, cluster)
        if (next == rpcEndpoint) return
        rpcEndpoint = next
        Log.w(TAG, "RPC failover [$cluster] -> ${maskEndpoint(rpcEndpoint)}")
    }

    private fun maskEndpoint(url: String): String = try {
        val uri = java.net.URI(url)
        val host = uri.host ?: url
        if (url.contains("api-key=")) {
            "$host?api-key=***"
        } else if (uri.path.length > 10) {
            "$host/...${uri.path.takeLast(4)}"
        } else {
            host
        }
    } catch (_: Exception) {
        "unknown-host"
    }

    private fun mapError(e: Throwable): AdminError {
        Log.e(TAG, "Mapping error: ${e.message}", e)
        return when (e) {
            is RpcException -> {
                if (e.message?.contains("BlockhashNotFound") == true) {
                    AdminError.BlockhashExpired
                } else {
                    AdminError.RpcError(e.code, e.message ?: "RPC error")
                }
            }
            is ResponseException -> AdminError.RpcError(e.response.status.value, "HTTP ${e.response.status.value}")
            is SocketTimeoutException -> AdminError.RpcTimeout
            is IOException -> AdminError.NetworkUnavailable
            is AdminError -> e
            else -> AdminError.Unknown(e)
        }
    }

    companion object {
        private const val TAG = "OpenCookieAdminRpc"

        fun initialCluster(): Cluster = when (BuildConfig.DEFAULT_CLUSTER.lowercase()) {
            "mainnet", "mainnet-beta", "mainnetbeta" -> Cluster.MainnetBeta
            else -> Cluster.Devnet
        }
    }
}
