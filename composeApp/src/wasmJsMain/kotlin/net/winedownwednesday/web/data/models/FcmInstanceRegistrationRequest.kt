package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

@Serializable
data class FcmInstanceRegistrationRequest(
    val instanceId: String,
    val email: String,
    val platform: String? = null,
    val appVersion: String? = null,
    val deviceModel: String? = null,
    val osVersion: String? = null,
)
