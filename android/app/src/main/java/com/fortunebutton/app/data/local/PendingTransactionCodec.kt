package com.fortunebutton.app.data.local

import com.fortunebutton.app.domain.model.PendingTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PendingTransactionCodec {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class StoredPendingTransaction(
        val signature: String,
        val action: String,
        val cluster: String,
        val createdAtMs: Long,
        val lastCheckedMs: Long,
        val hadProfile: Boolean = true,
        val walletAddress: String = "",
    )

    fun encode(transactions: List<PendingTransaction>): String {
        val stored = transactions.map {
            StoredPendingTransaction(
                it.signature, it.action, it.cluster, it.createdAtMs, it.lastCheckedMs,
                it.hadProfile, it.walletAddress,
            )
        }
        return json.encodeToString(stored)
    }

    fun decode(raw: String): List<PendingTransaction> = runCatching {
        json.decodeFromString<List<StoredPendingTransaction>>(raw).map {
            PendingTransaction(
                it.signature, it.action, it.cluster, it.createdAtMs, it.lastCheckedMs,
                it.hadProfile, it.walletAddress,
            )
        }
    }.getOrDefault(emptyList())
}
