package com.fortunebutton.app.data.transaction

import com.fortunebutton.app.data.program.ResolvedAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComputeBudgetPolicy @Inject constructor() {
    @Volatile
    var lastUsedLimit: Int = REGULAR_COMPUTE_UNIT_LIMIT
        private set

    @Volatile
    var lastUsedPriceMicrolamports: Long = STANDARD_PRICE_MICROLAMPORTS
        private set

    fun isHeavyTransaction(resolvedAction: ResolvedAction, prependInitializeUser: Boolean): Boolean =
        resolvedAction is ResolvedAction.InitializeUser || prependInitializeUser

    fun resolve(isHeavy: Boolean): ComputeBudgetResolution {
        val limit = if (isHeavy) HEAVY_COMPUTE_UNIT_LIMIT else REGULAR_COMPUTE_UNIT_LIMIT
        val price = STANDARD_PRICE_MICROLAMPORTS
        lastUsedLimit = limit
        lastUsedPriceMicrolamports = price
        return ComputeBudgetResolution(limit, price)
    }

    companion object {
        const val REGULAR_COMPUTE_UNIT_LIMIT = 40_000
        const val HEAVY_COMPUTE_UNIT_LIMIT = 80_000
        const val STANDARD_PRICE_MICROLAMPORTS = 100_000L
    }
}

data class ComputeBudgetResolution(val limit: Int, val priceMicrolamports: Long)
