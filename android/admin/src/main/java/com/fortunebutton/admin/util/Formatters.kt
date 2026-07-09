package com.fortunebutton.admin.util

fun formatLamports(lamports: Long): String {
    val sol = lamports / 1_000_000_000.0
    return if (sol >= 0.001) {
        String.format("%.4f SOL", sol)
    } else {
        "$lamports lamports"
    }
}

fun lamportsFromSolInput(input: String): Long? {
    val trimmed = input.trim().replace(",", ".")
    if (trimmed.isEmpty()) return null
    return runCatching {
        (trimmed.toDouble() * 1_000_000_000).toLong()
    }.getOrNull()
}

fun formatPriceLamports(lamports: Long): String {
    val sol = lamports / 1_000_000_000.0
    return if (sol >= 0.000001) {
        String.format("%.6f SOL", sol)
    } else {
        "$lamports lamports"
    }
}
