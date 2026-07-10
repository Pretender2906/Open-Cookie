package com.opencookie.admin.data.rpc

data class RpcRetryPolicy(
    val maxRetries: Int = 2,
    val initialDelayMs: Long = 400,
    val maxDelayMs: Long = 8_000,
    val backoffMultiplier: Double = 2.0,
    val retryableErrors: Set<Int> = setOf(-32005, -32016),
) {
    fun delayForAttempt(attempt: Int): Long {
        val delay = (initialDelayMs * Math.pow(backoffMultiplier, attempt.toDouble())).toLong()
        return delay.coerceAtMost(maxDelayMs)
    }

    fun isRetryable(errorCode: Int): Boolean = errorCode in retryableErrors
}
