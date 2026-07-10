package com.opencookie.app.domain.repository

import com.opencookie.app.domain.model.ProgramConfig
import com.opencookie.app.domain.model.UserProfile
import com.opencookie.app.util.PublicKey

interface ProfileRepository {
    suspend fun fetchProfile(wallet: PublicKey): Result<UserProfile>
    suspend fun fetchConfig(): Result<ProgramConfig>
    suspend fun fetchBalance(wallet: PublicKey): Result<Long>
    suspend fun profileExists(wallet: PublicKey): Result<Boolean>
}
