package com.fortunebutton.app.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Fortune : Screen("fortune")
}
