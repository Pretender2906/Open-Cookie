package com.fortunebutton.app.data.repository

import com.fortunebutton.app.data.program.AccountDeserializer
import com.fortunebutton.app.data.program.ProgramAddresses
import com.fortunebutton.app.data.rpc.SolanaRpcClient
import com.fortunebutton.app.data.session.AppSession
import com.fortunebutton.app.domain.model.AppError
import com.fortunebutton.app.domain.model.ProgramConfig
import com.fortunebutton.app.domain.model.UserProfile
import com.fortunebutton.app.domain.repository.ProfileRepository
import com.fortunebutton.app.util.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val rpcClient: SolanaRpcClient,
    private val appSession: AppSession,
) : ProfileRepository {

    override suspend fun fetchProfile(wallet: PublicKey): Result<UserProfile> {
        val (pda) = ProgramAddresses.userProfile(wallet)
        val delays = listOf(0L, 250L, 500L, 1000L)
        for (delayMs in delays) {
            if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
            val accountInfo = rpcClient.getAccountInfo(pda).getOrNull() ?: continue
            val data = rpcClient.decodeAccountData(accountInfo) ?: continue
            return try {
                val profile = AccountDeserializer.deserializeUserProfile(data)
                appSession.updateProfile(profile)
                Result.success(profile)
            } catch (e: Exception) {
                Result.failure(AppError.Unknown(e))
            }
        }
        return Result.failure(AppError.ProfileNotFound)
    }

    override suspend fun fetchConfig(): Result<ProgramConfig> {
        val (pda) = ProgramAddresses.config()
        val accountInfo = rpcClient.getAccountInfo(pda).getOrElse { return Result.failure(it) }
            ?: return Result.failure(AppError.ConfigNotLoaded)
        val data = rpcClient.decodeAccountData(accountInfo)
            ?: return Result.failure(AppError.ConfigNotLoaded)
        return try {
            val config = AccountDeserializer.deserializeConfig(data)
            appSession.updateConfig(config)
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(AppError.Unknown(e))
        }
    }

    override suspend fun fetchBalance(wallet: PublicKey): Result<Long> =
        rpcClient.getBalance(wallet).onSuccess { appSession.updateBalance(it) }

    override suspend fun profileExists(wallet: PublicKey): Result<Boolean> {
        val (pda) = ProgramAddresses.userProfile(wallet)
        return rpcClient.accountExists(pda)
    }
}
