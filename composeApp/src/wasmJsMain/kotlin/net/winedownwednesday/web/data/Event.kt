package net.winedownwednesday.web.data

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: Int,
    val name: String,
    val date: String,
    val time: String?,
    val eventType: EventType,
    val description: String,
    val imageUrl: String,
    val wineSelection: String?,
    val wineSelector: String?,
    val registrationLink: String?,
    val location: String,
    val additionalInfo: String?,
    val gallery: List<MediaItem>
)


