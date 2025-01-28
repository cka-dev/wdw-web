package net.winedownwednesday.web.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.winedownwednesday.web.FirebaseBridge

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        try {
            FirebaseBridge.initFirebase()
        } catch (e: Exception) {
            println("Error while initializing Firebase: $e")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            AppNavigation()
        }
    }
}