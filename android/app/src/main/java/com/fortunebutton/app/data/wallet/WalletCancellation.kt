package com.fortunebutton.app.data.wallet

import kotlinx.coroutines.CancellationException as CoroutineCancellationException
import java.util.concurrent.CancellationException as ConcurrentCancellationException

internal object WalletCancellation {

    fun isCancellation(error: Throwable?): Boolean = findCancellation(error) != null

    private fun findCancellation(error: Throwable?): Throwable? {
        if (error == null) return null
        var current: Throwable? = error
        val seen = mutableSetOf<Int>()
        while (current != null && System.identityHashCode(current) !in seen) {
            seen.add(System.identityHashCode(current))
            if (current is CoroutineCancellationException || current is ConcurrentCancellationException) {
                return current
            }
            current = current.cause
        }
        return null
    }
}
