package net.winedownwednesday.web.data

import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val id: Long,                       // Long: Date.now() timestamps overflow Int
    val name: String,
    val role: String,
    val memberType: MembershipType,
    val profilePictureUrl: String,
    val email: String,
    val phoneNumber: String,
    val birthday: String,
    val profession: String,
    val company: String,
    val business: String?,
    val interests: List<String>,
    val favoriteWines: List<String>,
    val spotlightReason: String? = null
)

enum class MembershipType{
    LEADER,
    MEMBER,
    GUEST
}
