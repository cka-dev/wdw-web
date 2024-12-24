package net.winedownwednesday.web.data

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: Int,
    val type: MediaType,
    val thumbnailUrl: String?,
    val contentUrl: String?,
)
