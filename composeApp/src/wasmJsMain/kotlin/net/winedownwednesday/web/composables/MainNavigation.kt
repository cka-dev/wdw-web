package net.winedownwednesday.web.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import net.winedownwednesday.web.FirebaseBridge

@Composable
fun MainNavigation() {
    LaunchedEffect(Unit) {
        kotlin.runCatching {
            FirebaseBridge.initFirebase()
        }
    }
    AppNavigation()
}