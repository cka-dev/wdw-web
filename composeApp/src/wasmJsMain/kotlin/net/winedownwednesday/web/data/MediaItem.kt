package net.winedownwednesday.web.data

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: Long,                       // Long: consistent with entity timestamp IDs
    val type: MediaType,
    val thumbnailUrl: String?,
    val contentUrl: String?,
)
