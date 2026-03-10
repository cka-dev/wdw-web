package net.winedownwednesday.web.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlogPostsResponse(
    val posts: List<BlogPost> = emptyList(),
    val hasMore: Boolean = false,
    val lastId: String? = null
)

@Serializable
data class BlogPost(
    val id: String = "",
    val title: String = "",
    val slug: String = "",
    val coverImageUrl: String = "",
    val summary: String = "",
    val content: List<ContentBlock> = emptyList(),
    val author: String = "",
    val tags: List<String> = emptyList(),
    val status: String = "draft",
    val createdAt: String = "",
    val updatedAt: String = "",
    val publishedAt: String? = null
)

@Serializable
sealed class ContentBlock {

    @Serializable
    @SerialName("paragraph")
    data class Paragraph(val text: String) : ContentBlock()
    
    @Serializable
    @SerialName("heading")
    data class Heading(val level: Int = 2, val text: String) : ContentBlock()
    
    @Serializable
    @SerialName("image")
    data class Image(val imageUrl: String = "", val caption: String? = null) : ContentBlock()
    
    @Serializable
    @SerialName("quote")
    data class Quote(val text: String) : ContentBlock()
    
    @Serializable
    @SerialName("list")
    data class ListBlock(val ordered: Boolean = false, val items: List<String> = emptyList()) : ContentBlock()
    
    @Serializable
    @SerialName("divider")
    class Divider() : ContentBlock()
}
