package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RSVPRequest(
    val eventId: Long,                  // Long: must match Event.id (Date.now() timestamp)
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val allowUpdates: Boolean,
    val guestsCount: Int
)
