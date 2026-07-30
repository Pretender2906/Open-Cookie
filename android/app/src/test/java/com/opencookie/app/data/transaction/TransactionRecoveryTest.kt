package com.opencookie.app.data.transaction

import com.opencookie.app.data.session.AppSession
import com.opencookie.app.domain.model.PendingTransaction
import org.junit.Assert.*
import org.junit.Test

class TransactionRecoveryTest {

    @Test
    fun testPruningFilterLogic() {
        val now = 1_000_000L
        val maxAge = AppSession.PENDING_MAX_AGE_MS
        val cutoff = now - maxAge
        val futureLimit = now + 10_000L
        
        val transactions = listOf(
            PendingTransaction("stale", "action", "mainnet", now - (maxAge + 1000L), now),
            PendingTransaction("valid_active", "action", "mainnet", now - (maxAge / 2), now),
            PendingTransaction("valid_near_future", "action", "mainnet", now + 5_000L, now),
            PendingTransaction("invalid_far_future", "action", "mainnet", now + 3_600_000L, now),
        )
        
        val filtered = transactions.filter { tx ->
            tx.createdAtMs in (cutoff..futureLimit)
        }
        
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.signature == "valid_active" })
        assertTrue(filtered.any { it.signature == "valid_near_future" })
        assertFalse(filtered.any { it.signature == "stale" })
        assertFalse(filtered.any { it.signature == "invalid_far_future" })
    }

    @Test
    fun testCanStartTransactionLogic() {
        val now = 1_000_000L
        val maxAge = AppSession.PENDING_MAX_AGE_MS
        
        fun canStart(pending: List<PendingTransaction>): Boolean {
            if (pending.isNotEmpty()) {
                val hasActivePending = pending.any { now - it.createdAtMs <= maxAge }
                if (hasActivePending) return false
            }
            return true
        }
        
        assertTrue("Empty pending should allow starting", canStart(emptyList()))
        
        val activePending = listOf(
            PendingTransaction("sig1", "action", "mainnet", now - 30_000L, now)
        )
        assertFalse("Active pending should block", canStart(activePending))
        
        val stalePending = listOf(
            PendingTransaction("sig2", "action", "mainnet", now - 150_000L, now)
        )
        assertTrue("Stale pending should NOT block", canStart(stalePending))
        
        val mixedPending = listOf(
            PendingTransaction("sig1", "action", "mainnet", now - 30_000L, now),
            PendingTransaction("sig2", "action", "mainnet", now - 150_000L, now)
        )
        assertFalse("Any active pending should block even if stale exists", canStart(mixedPending))
    }
}
