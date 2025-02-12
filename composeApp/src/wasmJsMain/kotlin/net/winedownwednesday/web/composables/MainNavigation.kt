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

    println("App version: 02/09/2025-09:46PM")
    LaunchedEffect(Unit) {
        kotlin.runCatching {
            FirebaseBridge.initFirebase()
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