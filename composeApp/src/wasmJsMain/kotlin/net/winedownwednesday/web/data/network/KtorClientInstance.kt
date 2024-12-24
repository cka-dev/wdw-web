package net.winedownwednesday.web.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object JsonInstanceProvider {
    val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }
}

object KtorClientInstance {
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(JsonInstanceProvider.json)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
}