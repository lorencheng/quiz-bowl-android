package com.quizbowl.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quizbowl.app.ui.bonus.BonusScreen
import com.quizbowl.app.ui.home.HomeScreen
import com.quizbowl.app.ui.multiplayer.MultiplayerScreen
import com.quizbowl.app.ui.tossup.TossupScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object TossupPractice : Screen("practice/tossup")
    data object BonusPractice : Screen("practice/bonus")
    data object Multiplayer : Screen("multiplayer")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.TossupPractice.route) {
            TossupScreen(navController = navController)
        }
        composable(Screen.BonusPractice.route) {
            BonusScreen(navController = navController)
        }
        composable(Screen.Multiplayer.route) {
            MultiplayerScreen(navController = navController)
        }
    }
}
