package com.opencookie.admin.data.repository

import android.util.Log
import com.opencookie.admin.data.program.AccountDeserializer
import com.opencookie.admin.data.program.ProgramAddresses
import com.opencookie.admin.data.rpc.SolanaRpcClient
import com.opencookie.admin.data.session.AdminSession
import com.opencookie.admin.domain.model.AdminError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val rpcClient: SolanaRpcClient,
    private val session: AdminSession,
) {
    suspend fun refresh(): Result<Unit> = runCatching {
        val wallet = session.state.value.walletAddress
        val (configPda) = ProgramAddresses.config()
        val (treasuryPda) = ProgramAddresses.treasuryVault()

        val configInfo = rpcClient.getAccountInfo(configPda).getOrElse { throw it }
        if (configInfo == null) {
            session.updateChainSnapshot(null, 0, 0)
            throw AdminError.ConfigNotFound
        }

        val configData = rpcClient.decodeAccountData(configInfo)
            ?: throw AdminError.ConfigNotFound
        val config = AccountDeserializer.deserializeConfig(configData)

        val treasuryBalance = rpcClient.getBalance(treasuryPda).getOrDefault(0)
        val walletBalance = wallet?.let { rpcClient.getBalance(it).getOrDefault(0) } ?: 0L

        session.updateChainSnapshot(
            config = config,
            treasuryBalanceLamports = treasuryBalance,
            walletBalanceLamports = walletBalance,
        )
    }.onFailure { e ->
        if (e !is AdminError) {
            Log.e("AdminRepository", "Refresh failed with unexpected error", e)
        }
    }
}
