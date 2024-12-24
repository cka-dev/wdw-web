package net.winedownwednesday.web

import androidx.compose.runtime.Composable
import net.winedownwednesday.web.composables.MainNavigation

@Composable
fun App() {
    WineDownWebApp()
}

@Composable
fun WineDownWebApp() {
    MainNavigation()
}