package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable
import net.winedownwednesday.web.ImageBitmapSerializer
import org.jetbrains.skia.Bitmap

@Serializable
data class NotificationPreferences(
    val eventReminders: Boolean? = true,
    val chatMessages: Boolean? = true,
    val communityUpdates: Boolean? = true,
    val rsvpConfirmations: Boolean? = true,
    val marketing: Boolean? = true
)

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
    val blockedEmails: List<String>? = emptyList(),
    // ── Profile parity with Member model ──
    val profession: String? = null,
    val company: String? = null,
    val interests: List<String>? = null,
    val favoriteWines: List<String>? = null,
    // ── Onboarding ──
    val profileComplete: Boolean? = false,
    val notificationPreferences: NotificationPreferences? = NotificationPreferences(),
) {
    /**
     * Whether this user has completed the minimum
     * onboarding requirements: name, email verified,
     * and phone number.
     */
    val isOnboardingComplete: Boolean
        get() = !name.isNullOrBlank()
                && !email.isNullOrBlank()
                && !phone.isNullOrBlank()
                && isVerified == true
}
