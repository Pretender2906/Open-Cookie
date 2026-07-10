package com.opencookie.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencookie.admin.data.wallet.ActivityResultSenderRegistry
import com.opencookie.admin.ui.AdminViewModel
import com.opencookie.admin.ui.screens.AdminMainScreen
import com.opencookie.admin.ui.screens.ConnectWalletScreen
import com.opencookie.admin.ui.theme.AdminTheme
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var senderRegistry: ActivityResultSenderRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        senderRegistry.bind(this, ActivityResultSender(this))
        setContent {
            AdminTheme {
                AdminRoot()
            }
        }
    }

    override fun onDestroy() {
        senderRegistry.clear(this)
        super.onDestroy()
    }
}

@Composable
private fun AdminRoot(viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState.isWalletConnected) {
        AdminMainScreen(viewModel)
    } else {
        ConnectWalletScreen(onConnected = {})
    }
}
