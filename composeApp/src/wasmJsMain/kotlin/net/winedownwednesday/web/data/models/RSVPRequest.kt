package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RSVPRequest(
    val eventId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val allowUpdates: Boolean,
    val guestsCount: Int
)
