package com.fortunebutton.app.data.transaction

import com.fortunebutton.app.data.rpc.SolanaRpcClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockhashCache @Inject constructor(
    private val rpcClient: SolanaRpcClient,
) {
    suspend fun getFresh() = rpcClient.getLatestBlockhash().getOrThrow()

    fun invalidate() {}
}
