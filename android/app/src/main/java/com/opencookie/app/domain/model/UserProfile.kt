package com.opencookie.app.domain.model

import com.opencookie.app.util.PublicKey

data class UserProfile(
    val owner: PublicKey,
    val totalCalls: Long,
    val lastDay: Int,
    val callsToday: Int,
    val bump: Int,
) {
    fun effectiveCallsToday(currentTimeMs: Long = System.currentTimeMillis()): Int {
        val currentDay = (currentTimeMs / 1000 / 86400).toInt()
        return if (lastDay == currentDay) callsToday else 0
    }
}
