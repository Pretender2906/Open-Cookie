package com.opencookie.admin.util

import java.math.BigDecimal
import java.math.RoundingMode

private const val LAMPORTS_PER_SOL = 1_000_000_000L

fun formatLamports(lamports: Long): String {
    val sol = lamportsToSolDecimal(lamports)
    return "${sol.toPlainString()} SOL"
}

fun solFromLamports(lamports: Long): String {
    return lamportsToSolDecimal(lamports).toPlainString()
}

fun lamportsFromSolInput(input: String): Long? {
    val trimmed = input.trim().replace(",", ".")
    if (trimmed.isEmpty()) return null
    return runCatching {
        BigDecimal(trimmed)
            .multiply(BigDecimal.valueOf(LAMPORTS_PER_SOL))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()
}

private fun lamportsToSolDecimal(lamports: Long): BigDecimal {
    return BigDecimal.valueOf(lamports)
        .divide(BigDecimal.valueOf(LAMPORTS_PER_SOL), 9, RoundingMode.UNNECESSARY)
        .stripTrailingZeros()
}
