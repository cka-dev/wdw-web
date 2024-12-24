package net.winedownwednesday.web.data

import kotlinx.serialization.Serializable

@Serializable
data class Episode(
    val id: Int,
    val title: String,
    val guestName: String,
    val guestPictureUrl: String,
    val description: String,
    val date: String,
    val videoUrl: String
)
