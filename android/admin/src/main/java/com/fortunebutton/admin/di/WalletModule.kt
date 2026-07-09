package com.fortunebutton.admin.di

import androidx.core.net.toUri
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WalletModule {
    @Provides
    @Singleton
    fun provideMobileWalletAdapter(): MobileWalletAdapter =
        MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = "https://fortune-button.pages.dev".toUri(),
                iconUri = "icon.png".toUri(),
                identityName = "Fortune Button Admin",
            ),
        )
}
