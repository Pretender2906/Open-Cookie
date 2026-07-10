package com.opencookie.app.data.wallet

import android.net.Uri
import android.util.Log
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter

private const val TAG = "WalletAdapterSession"

/** Matches SDK filtering in [MobileWalletAdapter.transact]. */
internal fun Uri?.toBoundWalletUri(): Uri? = this?.takeIf { it.scheme == "https" }

/**
 * Applies persisted MWA session to the singleton adapter before [MobileWalletAdapter.transact].
 * SDK keeps `walletUriBase` private; without it association uses a generic intent and Android
 * shows the wallet chooser on every transaction.
 */
internal fun MobileWalletAdapter.applyPersistedSession(authToken: String?, walletUriBase: Uri?) {
    this.authToken = authToken
    setPrivateWalletUriBase(walletUriBase.toBoundWalletUri())
    Log.d(
        TAG,
        "applyPersistedSession authToken=${authToken?.take(8)} walletUriBase=${walletUriBase.toBoundWalletUri()}",
    )
}

internal fun MobileWalletAdapter.clearWalletSession() {
    authToken = null
    setPrivateWalletUriBase(null)
}

private fun MobileWalletAdapter.setPrivateWalletUriBase(uri: Uri?) {
    runCatching {
        val field = MobileWalletAdapter::class.java.getDeclaredField("walletUriBase")
        field.isAccessible = true
        field.set(this, uri)
    }.onFailure { e ->
        Log.w(TAG, "Failed to restore walletUriBase on adapter", e)
    }
}
