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
    ComposeViewport(document.body!!) {
        App()
    }
}