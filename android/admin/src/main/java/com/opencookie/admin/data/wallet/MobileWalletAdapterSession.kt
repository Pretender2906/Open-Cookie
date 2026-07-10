package com.opencookie.admin.data.wallet

import android.net.Uri
import android.util.Log
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter

private const val TAG = "WalletAdapterSession"

internal fun Uri?.toBoundWalletUri(): Uri? = this?.takeIf { it.scheme == "https" }

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
