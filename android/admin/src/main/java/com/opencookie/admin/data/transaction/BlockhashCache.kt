package com.opencookie.admin.data.transaction

import com.opencookie.admin.data.rpc.BlockhashValue
import com.opencookie.admin.data.rpc.SolanaRpcClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockhashCache @Inject constructor(
    private val rpcClient: SolanaRpcClient,
) {
    suspend fun getFresh(): BlockhashValue = rpcClient.getLatestBlockhash().getOrThrow()

    fun invalidate() = Unit
}
