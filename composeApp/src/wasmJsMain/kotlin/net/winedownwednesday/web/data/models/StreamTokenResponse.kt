package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

@Serializable
data class StreamTokenResponse(
    val token: String,
    val userId: String,
    val apiKey: String,
    val email: String? = null
)
