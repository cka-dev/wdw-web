package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CancelRsvpRequest(
    val eventId: Long,
    val email: String
)
