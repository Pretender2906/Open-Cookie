package com.opencookie.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.opencookie.app.data.AppReadiness
import com.opencookie.app.data.session.AppSession
import com.opencookie.app.ui.screens.cookie.CookieScreen
import com.opencookie.app.ui.screens.onboarding.ConnectWalletScreen
import com.opencookie.app.ui.screens.profile.ProfileScreen

@Composable
fun AppNavHost(
    appSession: AppSession,
    appReadiness: AppReadiness,
) {
    val navController = rememberNavController()
    val sessionState by appSession.state.collectAsState()

    val startDestination = remember {
        if (appSession.state.value.isWalletConnected) Screen.Home.route else Screen.Onboarding.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            ConnectWalletScreen(
                onConnected = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Home.route) {
            CookieScreen(
                onProfileClick = { navController.navigate(Screen.Profile.route) },
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onDisconnected = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }

    LaunchedEffect(Unit) {
        appReadiness.awaitReady()
        if (sessionState.isWalletConnected &&
            navController.currentDestination?.route == Screen.Onboarding.route
        ) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        }
    }
}
