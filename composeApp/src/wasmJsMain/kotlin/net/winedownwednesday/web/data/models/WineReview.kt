package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

@Serializable
data class WineReview(
    val wineId: Long,
    val userEmail: String,
    val userName: String,
    val rating: Int,
    val reviewText: String? = null,
    val flagged: Boolean = false,
    val moderationNote: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class WineReviewsResponse(
    val reviews: List<WineReview>,
    val hasMore: Boolean
)

@Serializable
data class SubmitReviewRequest(
    val wineId: Long,
    val rating: Int,
    val reviewText: String? = null,
    val userName: String? = null
)

@Serializable
data class FlagReviewRequest(
    val wineId: Long,
    val reviewerEmail: String,
    val reason: String? = null
)
