package net.winedownwednesday.web.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
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
    val location: String,
    val additionalInfo: String?,
    val gallery: List<MediaItem>
)


