package com.opencookie.app.data.transaction

import com.opencookie.app.data.local.PreferencesStore
import com.opencookie.app.data.program.ResolvedAction
import com.opencookie.app.di.ApplicationScope
import com.opencookie.app.domain.model.NetworkFeePriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComputeBudgetPolicy @Inject constructor(
    private val preferencesStore: PreferencesStore,
    @ApplicationScope appScope: CoroutineScope,
) {
    @Volatile
    var networkFeePriority: NetworkFeePriority = NetworkFeePriority.Standard
        internal set

    @Volatile
    var lastUsedLimit: Int = REGULAR_COMPUTE_UNIT_LIMIT
        private set

    @Volatile
    var lastUsedPriceMicrolamports: Long = STANDARD_PRICE_MICROLAMPORTS
        private set

    init {
        appScope.launch {
            networkFeePriority = preferencesStore.getNetworkFeePriority()
            preferencesStore.networkFeePriorityFlow().collect { networkFeePriority = it }
        }
    }

    fun isHeavyTransaction(resolvedAction: ResolvedAction, prependInitializeUser: Boolean): Boolean =
        resolvedAction is ResolvedAction.InitializeUser || prependInitializeUser

    fun resolve(isHeavy: Boolean): ComputeBudgetResolution {
        val limit = if (isHeavy) HEAVY_COMPUTE_UNIT_LIMIT else REGULAR_COMPUTE_UNIT_LIMIT
        val price = when (networkFeePriority) {
            NetworkFeePriority.Standard -> STANDARD_PRICE_MICROLAMPORTS
            NetworkFeePriority.Fast -> FAST_PRICE_MICROLAMPORTS
        }
        lastUsedLimit = limit
        lastUsedPriceMicrolamports = price
        return ComputeBudgetResolution(limit, price)
    }

    companion object {
        const val REGULAR_COMPUTE_UNIT_LIMIT = 20_000
        const val HEAVY_COMPUTE_UNIT_LIMIT = 40_000
        const val STANDARD_PRICE_MICROLAMPORTS = 100_000L
        const val FAST_PRICE_MICROLAMPORTS = 250_000L
    }
}

data class ComputeBudgetResolution(val limit: Int, val priceMicrolamports: Long)
