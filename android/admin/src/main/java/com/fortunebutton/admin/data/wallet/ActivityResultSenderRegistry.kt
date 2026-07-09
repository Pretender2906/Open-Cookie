package com.fortunebutton.admin.data.wallet

import androidx.activity.ComponentActivity
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityResultSenderRegistry @Inject constructor() {
    @Volatile
    private var boundActivity: ComponentActivity? = null
    @Volatile
    private var sender: ActivityResultSender? = null

    fun bind(activity: ComponentActivity, activityResultSender: ActivityResultSender) {
        boundActivity = activity
        sender = activityResultSender
    }

    fun clear(activity: ComponentActivity) {
        if (boundActivity === activity) {
            boundActivity = null
            sender = null
        }
    }

    fun current(): ActivityResultSender? = sender
}
