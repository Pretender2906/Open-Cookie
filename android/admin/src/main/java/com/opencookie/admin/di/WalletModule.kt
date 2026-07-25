package com.opencookie.admin.di

import androidx.core.net.toUri
import com.opencookie.admin.data.wallet.DAppIdentity
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
                identityUri = DAppIdentity.IDENTITY_URI.toUri(),
                iconUri = DAppIdentity.ICON_RELATIVE.toUri(),
                identityName = DAppIdentity.IDENTITY_NAME,
            ),
        )
}
