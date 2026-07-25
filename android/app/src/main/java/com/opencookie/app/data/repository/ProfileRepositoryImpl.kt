package com.opencookie.app.data.repository

import com.opencookie.app.data.program.AccountDeserializer
import com.opencookie.app.data.program.ProgramAddresses
import com.opencookie.app.data.rpc.SolanaRpcClient
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.domain.model.AppError
import com.opencookie.app.domain.model.ProfilePresence
import com.opencookie.app.domain.model.ProgramConfig
import com.opencookie.app.domain.model.UserProfile
import com.opencookie.app.domain.repository.ProfileRepository
import com.opencookie.app.util.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val rpcClient: SolanaRpcClient,
    private val appSession: AppSession,
) : ProfileRepository {

    override suspend fun fetchProfile(wallet: PublicKey): Result<UserProfile> {
        val currentPresence = appSession.state.value.profilePresence
        if (currentPresence == ProfilePresence.Unknown || currentPresence == ProfilePresence.CheckFailed) {
            appSession.setProfilePresence(ProfilePresence.Checking)
        }
        val (pda) = ProgramAddresses.userProfile(wallet)
        val delays = listOf(0L, 250L, 500L, 1000L)
        var lastRpcError: AppError? = null

        for (delayMs in delays) {
            if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
            val accountInfoResult = rpcClient.getAccountInfo(pda)
            if (accountInfoResult.isFailure) {
                lastRpcError = accountInfoResult.exceptionOrNull() as? AppError ?: AppError.Unknown(
                    accountInfoResult.exceptionOrNull(),
                )
                continue
            }

            val accountInfo = accountInfoResult.getOrNull()
            if (accountInfo == null) {
                appSession.clearProfile()
                appSession.setProfilePresence(ProfilePresence.NotExists)
                appSession.markProfileRefreshed()
                return Result.failure(AppError.ProfileNotFound)
            }

            val data = rpcClient.decodeAccountData(accountInfo) ?: continue
            return try {
                val profile = AccountDeserializer.deserializeUserProfile(data)
                appSession.updateProfile(profile)
                appSession.setProfilePresence(ProfilePresence.Exists)
                appSession.markProfileRefreshed()
                Result.success(profile)
            } catch (e: Exception) {
                appSession.setProfilePresence(ProfilePresence.CheckFailed)
                Result.failure(AppError.Unknown(e))
            }
        }

        if (lastRpcError != null) {
            appSession.setProfilePresence(ProfilePresence.CheckFailed)
            return Result.failure(lastRpcError)
        }

        appSession.clearProfile()
        appSession.setProfilePresence(ProfilePresence.NotExists)
        appSession.markProfileRefreshed()
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
