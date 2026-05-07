package net.winedownwednesday.web.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

@JsFun(
    """() => {
    try {
        if (!window.wdwFirebaseBridge ||
            !window.wdwFirebaseBridge.appCheck) return '';
        return '';
    } catch(e) { return ''; }
}"""
)
private external fun jsCheckAppCheckReady(): JsString

/**
 * Synchronously returns the last cached App Check token.
 * The token is kept fresh by firebase-bridge.js with
 * isTokenAutoRefreshEnabled: true.
 */
@JsFun(
    """() => {
    try {
        if (!window.wdwFirebaseBridge ||
            !window.wdwFirebaseBridge._lastAppCheckToken) return '';
        return window.wdwFirebaseBridge._lastAppCheckToken;
    } catch(e) { return ''; }
}"""
)
private external fun jsGetCachedAppCheckToken(): JsString

object JsonInstanceProvider {
    val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
        encodeDefaults = true
    }
}

object KtorClientInstance {
    val httpClient = HttpClient() {
        install(ContentNegotiation) {
            json(JsonInstanceProvider.json)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.NONE
        }
        defaultRequest {
            val token = jsGetCachedAppCheckToken().toString()
            if (token.isNotEmpty()) {
                headers.append("X-Firebase-AppCheck", token)
            }
        }
    }
}