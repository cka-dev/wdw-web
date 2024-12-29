package net.winedownwednesday.web.data.models

import org.jetbrains.compose.resources.DrawableResource

data class AboutSection(
    val title: String,
    val body: String,
    val imageRes: DrawableResource? = null,
    val imageOnLeft: Boolean
)