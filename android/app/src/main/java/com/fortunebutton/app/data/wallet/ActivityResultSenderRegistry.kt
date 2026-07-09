package com.fortunebutton.app.data.wallet

import android.util.Log
import androidx.activity.ComponentActivity
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityResultSenderRegistry @Inject constructor(
    private val walletInteractionTracker: WalletInteractionTracker,
) {
    @Volatile
    private var boundActivity: ComponentActivity? = null

    @Volatile
    private var sender: ActivityResultSender? = null

    fun bind(activity: ComponentActivity, activityResultSender: ActivityResultSender) {
        val replaced = boundActivity != null && boundActivity !== activity
        boundActivity = activity
        sender = activityResultSender
        if (replaced) walletInteractionTracker.onHostActivityReplaced()
    }

    fun clear(activity: ComponentActivity) {
        if (boundActivity === activity) {
            walletInteractionTracker.onHostActivityDestroyed()
            boundActivity = null
            sender = null
        }
    }

    fun current(): ActivityResultSender? = sender
}
