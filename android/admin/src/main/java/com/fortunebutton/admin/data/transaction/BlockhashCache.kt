package com.fortunebutton.admin.data.transaction

import com.fortunebutton.admin.data.rpc.BlockhashValue
import com.fortunebutton.admin.data.rpc.SolanaRpcClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockhashCache @Inject constructor(
    private val rpcClient: SolanaRpcClient,
) {
    suspend fun getFresh(): BlockhashValue = rpcClient.getLatestBlockhash().getOrThrow()

    fun invalidate() = Unit
}
