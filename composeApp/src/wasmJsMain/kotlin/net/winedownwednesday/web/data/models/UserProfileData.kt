package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable
import net.winedownwednesday.web.ImageBitmapSerializer
import org.jetbrains.skia.Bitmap


@Serializable
data class UserProfileData(
    val name: String?,
    val email: String?,
    val phone: String?,
    val aboutMe: String?,
    // Client-local only: used to serialize the bitmap for upload.
    // The server never returns this field (always null after save).
    @Serializable(with = ImageBitmapSerializer::class)
    val profileImageBitmap: Bitmap? = null,
    val profileImageUrl: String? = null,
    val birthDate: String? = null,
    val isVerified: Boolean? = false,
    val isMember: Boolean? = false,
    val hasPassword: Boolean = false,
    val hasPasskey: Boolean = false,
    val eventRsvps: Map<Long, RSVPRequest>? = emptyMap(),
    val blockedEmails: List<String>? = emptyList()
)
