package net.winedownwednesday.web.data


import kotlinx.serialization.Serializable

@Serializable
data class Wine(
    val id: Int,
    val name: String,
    val type: String,
    val year: Int,
    val country: String,
    val region: String,
    val imageUrl: String,
    val technicalDetails: String,
    val whyWeLovedIt: String? = null
)
