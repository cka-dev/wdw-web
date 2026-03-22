package net.winedownwednesday.web.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import net.winedownwednesday.web.FirebaseBridge

@Composable
fun MainNavigation() {
    LaunchedEffect(Unit) {
        // println("App version: 03/18/2026-10:54AM")
        kotlin.runCatching {
            FirebaseBridge.initFirebase()
        }
    }
    AppNavigation()
}