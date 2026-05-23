package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

/**
 * A single release highlight item.
 */
@Serializable
data class WhatsNewItem(
    val emoji: String = "",
    val title: String = "",
    val description: String = "",
)

/**
 * Per-platform "What's New" content returned by getInitialData.
 * Null when no content exists for the requested platform.
 */
@Serializable
data class WhatsNew(
    val version: String = "",
    val title: String = "",
    val items: List<WhatsNewItem> = emptyList(),
)
