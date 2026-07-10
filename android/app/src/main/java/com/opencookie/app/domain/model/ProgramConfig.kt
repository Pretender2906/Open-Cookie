package com.opencookie.app.domain.model

import com.opencookie.app.util.PublicKey

data class ProgramConfig(
    val adminAuthority: PublicKey,
    val pendingAdmin: PublicKey,
    val priceLamports: Long,
    val treasuryBump: Int,
    val configBump: Int,
) {
    val hasPendingAdmin: Boolean
        get() = pendingAdmin != PublicKey.ZERO
}
