package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

@Serializable
data class FcmInstanceRegistrationRequest(
    val instanceId: String,
    val email: String,
)
