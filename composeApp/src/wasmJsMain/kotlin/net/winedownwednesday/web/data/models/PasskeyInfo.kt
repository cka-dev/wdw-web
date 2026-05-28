package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

/**
 * Public-safe passkey info returned from the server.
 * Never includes publicKey or counter.
 */
@Serializable
data class PasskeyInfo(
    val id: String,
    val label: String = "Passkey",
    val platform: String = "unknown",
    val createdAt: String? = null,
    val lastUsedAt: String? = null,
)

/**
 * Response from GET /getPasskeys.
 */
@Serializable
data class PasskeysResponse(
    val passkeys: List<PasskeyInfo>,
    val passkeyCount: Int,
)

/**
 * Response from POST /deletePasskey.
 */
@Serializable
data class DeletePasskeyResponse(
    val success: Boolean,
    val passkeyCount: Int,
)
