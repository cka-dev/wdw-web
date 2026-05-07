package net.winedownwednesday.web.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import net.winedownwednesday.web.FirebaseBridge

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
            level = LogLevel.NONE // Disabled API request/response logging
        }
    }.also { client ->
        client.requestPipeline.intercept(
            io.ktor.client.request.HttpRequestPipeline.State
        ) {
            try {
                val token = FirebaseBridge
                    .getAppCheckToken()
                    .await<JsString?>()
                    ?.toString()
                if (!token.isNullOrEmpty()) {
                    context.headers.append(
                        "X-Firebase-AppCheck",
                        token
                    )
                }
            } catch (_: Throwable) {
                // App Check unavailable — proceed without token
            }
        }
    }
}