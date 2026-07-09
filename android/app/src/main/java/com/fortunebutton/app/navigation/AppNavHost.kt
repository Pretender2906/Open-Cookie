package com.fortunebutton.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fortunebutton.app.data.AppReadiness
import com.fortunebutton.app.data.session.AppSession
import com.fortunebutton.app.ui.screens.fortune.FortuneScreen
import com.fortunebutton.app.ui.screens.onboarding.ConnectWalletScreen

@Composable
fun AppNavHost(
    appSession: AppSession,
    appReadiness: AppReadiness,
) {
    val navController = rememberNavController()
    val sessionState by appSession.state.collectAsState()

    val startDestination = remember {
        if (appSession.state.value.isWalletConnected) Screen.Fortune.route else Screen.Onboarding.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            ConnectWalletScreen(
                onConnected = {
                    navController.navigate(Screen.Fortune.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Fortune.route) {
            FortuneScreen()
        }
    }

    LaunchedEffect(Unit) {
        appReadiness.awaitReady()
        if (sessionState.isWalletConnected &&
            navController.currentDestination?.route == Screen.Onboarding.route
        ) {
            navController.navigate(Screen.Fortune.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        }
    }
}
