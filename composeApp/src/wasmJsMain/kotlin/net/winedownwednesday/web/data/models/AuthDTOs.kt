package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PublicKeyCredentialCreationOptions(
    val rp: RelyingParty,
    val user: User,
    val challenge: String,
    val pubKeyCredParams: List<PubKeyCredParam>,
    val timeout: Int? = 60000,
    val excludeCredentials: List<Credential>? = emptyList(),
    val authenticatorSelection: AuthenticatorSelectionCriteria,
    val attestation: String? = "none",
    val extensions: Map<String, String>? = null,
) {
    @Serializable
    data class RelyingParty(
        val id: String,
        val name: String,
    )

    @Serializable
    data class User(
        val id: String,
        val name: String,
        val displayName: String,
    )

    @Serializable
    data class PubKeyCredParam(
        val type: String,
        val alg: Int,
    )

    @Serializable
    data class Credential(
        val id: ByteArray,
        val type: String,
        val transports: List<String>? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Credential

            if (!id.contentEquals(other.id)) return false
            if (type != other.type) return false
            if (transports != other.transports) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.contentHashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + (transports?.hashCode() ?: 0)
            return result
        }
    }

    @Serializable
    data class AuthenticatorSelectionCriteria(
        val authenticatorAttachment: String? = null,
        val residentKey: String? = null,
        val userVerification: String? = null,
        val requireResidentKey: Boolean? = null,
    )
}

@Serializable
data class PublicKeyCredentialRequestOptions(
    val challenge: String,
    val timeout: Int? = 60000,
    val rpId: String,
    val allowCredentials: List<Credential>? = emptyList(),
    val userVerification: String? = null,
    val extensions: Map<String, String>? = null,
) {
    @Serializable
    data class Credential(
        val id: ByteArray,
        val type: String,
        val transports: List<String>? = null,
    )
}

@Serializable
data class AuthenticationResponse(
    val id: String,
    val rawId: String,
    val type: String,
    val response: ResponseData,
) {
    @Serializable
    data class ResponseData(
        val clientDataJSON: String,
        val authenticatorData: String,
        val signature: String,
        val userHandle: String?,
    )
}

@Serializable
data class RegistrationResponse(
    val id: String,
    val rawId: String,
    val type: String,
    val response: ResponseData,
) {
    @Serializable
    data class ResponseData(
        val clientDataJSON: String,
        val attestationObject: String,
    )
}

@Serializable
data class VerifyRegistrationRequest(
    val credential: RegistrationResponse,
    val email: String
)

@Serializable
data class VerifyAuthenticationRequest(
    val credential: AuthenticationResponse,
    val email: String
)

@Serializable
data class RegistrationOptionsRequest(
    val email: String,
)

@Serializable
data class FirebaseAuthResponse(
    val token: String,
    val verified: Boolean,
)

