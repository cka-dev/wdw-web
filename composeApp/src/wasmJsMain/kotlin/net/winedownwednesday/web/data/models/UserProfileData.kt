package net.winedownwednesday.web.data.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.winedownwednesday.web.ImageBitmapSerializer
import org.jetbrains.skia.Bitmap

@OptIn(ExperimentalSerializationApi::class)
//@JsonIgnoreUnknownKeys
@Serializable
data class UserProfileData(
    val name: String?,
    val email: String?,
    val phone: String?,
    val aboutMe: String?,
    @Serializable(with = ImageBitmapSerializer::class)
    val profileImageBitmap: Bitmap? = null,
    val profileImageUrl: String? = null,
    val birthDate: String? = null,
    val isVerified: Boolean? = false,
    val isMember: Boolean? = false
)
