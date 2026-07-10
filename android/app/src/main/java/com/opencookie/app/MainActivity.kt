package com.opencookie.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.opencookie.app.data.AppReadiness
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.data.wallet.ActivityResultSenderRegistry
import com.opencookie.app.data.wallet.WalletInteractionTracker
import com.opencookie.app.navigation.AppNavHost
import com.opencookie.app.ui.theme.OpenCookieTheme
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appSession: AppSession
    @Inject lateinit var appReadiness: AppReadiness
    @Inject lateinit var activityResultSenderRegistry: ActivityResultSenderRegistry
    @Inject lateinit var walletInteractionTracker: WalletInteractionTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        activityResultSenderRegistry.bind(this, ActivityResultSender(this))

        setContent {
            OpenCookieTheme {
                AppNavHost(appSession = appSession, appReadiness = appReadiness)
            }
        }
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            activityResultSenderRegistry.clear(this)
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        walletInteractionTracker.onActivityResumed()
    }

    companion object {
        private const val TAG = "OpenCookie"
    }
}
