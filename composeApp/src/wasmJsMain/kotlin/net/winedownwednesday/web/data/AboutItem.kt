package net.winedownwednesday.web.data

import kotlinx.serialization.Serializable

@Serializable
data class AboutItem(
    val title: String,
    val content: String,
    val imageUrl: String? = null
)
