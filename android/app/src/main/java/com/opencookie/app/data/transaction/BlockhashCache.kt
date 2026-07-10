package com.opencookie.app.data.transaction

import com.opencookie.app.data.rpc.SolanaRpcClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockhashCache @Inject constructor(
    private val rpcClient: SolanaRpcClient,
) {
    suspend fun getFresh() = rpcClient.getLatestBlockhash().getOrThrow()

    fun invalidate() {}
}
