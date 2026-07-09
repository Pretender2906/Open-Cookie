package com.fortunebutton.app.domain.model

import com.fortunebutton.app.util.PublicKey

data class UserProfile(
    val owner: PublicKey,
    val totalCalls: Long,
    val lastDay: Int,
    val callsToday: Int,
    val bump: Int,
) {
    companion object {
        const val MAX_CALLS_PER_DAY = 3
    }
}
