package net.winedownwednesday.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import net.winedownwednesday.web.di.appModule
import org.koin.core.context.GlobalContext.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(appModule)
    }
    val body = document.body ?: return
    ComposeViewport(body) {
        App()
    }
}