package com.fortunebutton.app.domain.repository

import com.fortunebutton.app.domain.model.ProgramConfig
import com.fortunebutton.app.domain.model.UserProfile
import com.fortunebutton.app.util.PublicKey

interface ProfileRepository {
    suspend fun fetchProfile(wallet: PublicKey): Result<UserProfile>
    suspend fun fetchConfig(): Result<ProgramConfig>
    suspend fun fetchBalance(wallet: PublicKey): Result<Long>
    suspend fun profileExists(wallet: PublicKey): Result<Boolean>
}
