package com.opencookie.app.domain.model

import com.opencookie.app.util.PublicKey

data class UserProfile(
    val owner: PublicKey,
    val totalCalls: Long,
    val lastDay: Int,
    val callsToday: Int,
    val bump: Int,
)
