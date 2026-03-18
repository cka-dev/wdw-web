package net.winedownwednesday.web.data

import kotlinx.serialization.Serializable

@Serializable
data class Episode(
    val id: Long,                       // Long: Date.now() timestamps overflow Int
    val title: String,
    val guestName: String,
    val guestPictureUrl: String,
    val description: String,
    val date: String,
    val videoUrl: String
)
