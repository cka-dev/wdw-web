package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable
import net.winedownwednesday.web.data.Wine

@Serializable
data class FeaturedWinesResponse(
    val campaignName: String = "Featured Wines",
    val description: String = "",
    val wines: List<Wine> = emptyList()
)
