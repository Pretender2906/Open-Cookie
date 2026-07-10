package com.opencookie.app.di

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
                identityUri = IDENTITY_URI.toUri(),
                iconUri = "icon.png".toUri(),
                identityName = "Open Cookie",
            ),
        )

    private const val IDENTITY_URI = "https://open-cookie.pages.dev"
}
