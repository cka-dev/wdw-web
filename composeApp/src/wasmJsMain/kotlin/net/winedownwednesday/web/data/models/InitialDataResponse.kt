package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable
import net.winedownwednesday.web.data.AboutItem
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine

/**
 * Combined response from the /getInitialData batch endpoint.
 * Replaces 8 individual API calls with a single request on app load.
 */
@Serializable
data class InitialDataResponse(
    val members: List<Member> = emptyList(),
    val events: List<Event> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val wines: List<Wine> = emptyList(),
    val aboutItems: List<AboutItem> = emptyList(),
    val memberSpotlight: Member? = null,
    val featuredWines: FeaturedWinesResponse? = null,
    val blogPosts: BlogPostsResponse? = null,
)
