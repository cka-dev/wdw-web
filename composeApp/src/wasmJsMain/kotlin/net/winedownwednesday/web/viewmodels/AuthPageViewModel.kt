package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.PublicKeyCredential
import net.winedownwednesday.web.data.models.AuthenticationResponse
import net.winedownwednesday.web.data.models.RSVPRequest
import net.winedownwednesday.web.data.models.RegistrationResponse
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.data.repositories.AppRepository
import net.winedownwednesday.web.myWebAuthnBridge
import net.winedownwednesday.web.toBase64Url

class AuthPageViewModel(
    private val repository: AppRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUIState>(LoginUIState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _isNewUser = MutableStateFlow(false)
    val isNewUser = _isNewUser.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _profileData = MutableStateFlow<UserProfileData?>(null)
    val profileData: StateFlow<UserProfileData?> = _profileData.asStateFlow()

    fun fetchProfile(userEmail: String) {
        viewModelScope.launch {
            val dto = repository.fetchProfileFromServer(userEmail = userEmail)
            _profileData.value = dto
        }
    }

    fun saveProfile(userProfileData: UserProfileData, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.saveProfileToServer(userProfileData)
            if (success) {
                _profileData.value = userProfileData
            }
            onResult(success)
        }
    }

    fun registerPasskey(email: String) {
        viewModelScope.launch {
            _uiState.value = LoginUIState.Loading
            try {
                val options = repository.generatePasskeyRegistrationOptions(email)
                    ?: throw Exception("Failed to generate registration options")

                val credential = myWebAuthnBridge.startRegistration(
                    challenge = options.challenge,
                    rpId = options.rp.id,
                    rpName = options.rp.name,
                    userId = options.user.id,
                    userName = options.user.name,
                    userDisplayName = options.user.displayName,
                    timeout = options.timeout ?: 60000,
                    attestationType = options.attestation,
                    authenticatorAttachment = options
                        .authenticatorSelection.authenticatorAttachment,
                    residentKey = options.authenticatorSelection.residentKey,
                    requireResidentKey = options.authenticatorSelection.requireResidentKey,
                    userVerification = options.authenticatorSelection.userVerification
                ).await<PublicKeyCredential>()

                val registrationResponse = try {
                    credential.toRegistrationResponse()
                } catch (e: Exception) {
                    println("$TAG: Caught exception: $e")
                    throw e
                }

                val verified = repository.verifyPasskeyRegistration(registrationResponse, email)
                if (verified) {
                    _uiState.value = LoginUIState.Authenticated
                } else {
                    _uiState.value = LoginUIState.Error("Passkey registration failed")
                }
            } catch (e: Exception) {
                _uiState.value = LoginUIState.Error("Error during registration: ${e.message}")
            }
        }
    }

    fun authenticateWithPasskey(email: String) {
        viewModelScope.launch {
            _uiState.value = LoginUIState.Loading
            try {
                val options = repository.generatePasskeyAuthenticationOptions(email)
                    ?: throw Exception("Failed to generate authentication options")

                val allowCredentialIds =
                    options.allowCredentials?.joinToString(",", "[", "]") {
                    "\"" + it.id.toBase64Url() + "\""
                } ?: "[]"

                try {
                    val credential = myWebAuthnBridge.startAuthentication(
                        challenge = options.challenge,
                        rpId = options.rpId,
                        timeout = options.timeout ?: 60000,
                        userVerification = options.userVerification,
                        allowCredentialIds = allowCredentialIds
                    ).await<PublicKeyCredential>()

                    val authenticationResponse = try {
                        credential.toAuthenticationResponse()
                    } catch (e: Exception) {
                        throw e
                    }

                    val verified = repository
                        .verifyPasskeyAuthentication(authenticationResponse, email)
                    if (verified) {
                        _uiState.value = LoginUIState.Authenticated
                    } else {
                        _uiState.value = LoginUIState.Error("Passkey authentication failed")
                    }

                } catch (e: Exception) {
                    println("Error during authentication: ${e.message}")
                }

            } catch (e: Exception) {
                _uiState.value = LoginUIState.Error("Error during authentication: ${e.message}")
            }
        }
    }

    private fun JsAny.toRegistrationResponse(): RegistrationResponse {
        val credential = this as? PublicKeyCredential ?: run {
            throw IllegalStateException(
                "Expected PublicKeyCredential but was ${this::class.simpleName}")
        }

        val response = credential.response

        return RegistrationResponse(
            id = credential.id,
            rawId = credential.rawId.toBase64Url(),
            type = "public-key",
            response = RegistrationResponse.ResponseData(
                clientDataJSON = response.clientDataJSON.toBase64Url(),
                attestationObject = response.attestationObject?.toBase64Url() ?: ""
            )
        )
    }

    private fun JsAny.toAuthenticationResponse(): AuthenticationResponse {
        val credential = this.unsafeCast<PublicKeyCredential>()

        val response = credential.response
        if (response.authenticatorData == null || response.signature == null) {
            throw IllegalStateException("AuthenticatorResponse is null")
        }

        val authenticationResponse=
            try {
                AuthenticationResponse(
                    id = credential.id,
                    rawId = credential.rawId.toBase64Url(),
                    type = "public-key",
                    response = AuthenticationResponse.ResponseData(
                        clientDataJSON = response.clientDataJSON.toBase64Url(),
                        authenticatorData = response.authenticatorData?.toBase64Url() ?: "",
                        signature = response.signature?.toBase64Url() ?: "",
                        userHandle = response.userHandle?.toBase64Url(),
                    ),
                )
            } catch (e: Exception) {
                println("$TAG: Caught exception: $e")
                throw e
            }

        return authenticationResponse
    }

    fun simulateAuthentication() {
        viewModelScope.launch {
            delay(500)
            _uiState.value = LoginUIState.Authenticated
        }
    }

    fun simulateRegistration() {
        viewModelScope.launch {
            delay(500)
            _uiState.value = LoginUIState.Authenticated
        }
    }

    fun checkIsNewUser(isNewUser: Boolean) {
        viewModelScope.launch {
            _isNewUser.value = isNewUser
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = LoginUIState.Idle
        }
    }

    fun setEmail(email: String) {
        viewModelScope.launch {
            _email.value = email
        }
    }

    fun hasUserRsvped(eventId: Int?): Boolean {
        val currentProfile = _profileData.value ?: return false
        if (eventId == null) return false
        return currentProfile.eventRsvps?.containsKey(eventId) ?: false
    }

    fun getRsvpForEvent(eventId: Int?): RSVPRequest? {
        if (eventId == null) return null
        return _profileData.value?.eventRsvps?.get(eventId)
    }

    fun saveRsvpInProfile(
        rsvp: RSVPRequest,
        onResult: (Boolean) -> Unit
    ) {
        val oldProfile = _profileData.value ?: return
        val newMap = oldProfile.eventRsvps?.toMutableMap()?.apply {
            put(rsvp.eventId, rsvp)
        }
        val updatedProfile = oldProfile.copy(eventRsvps = newMap)
        _profileData.value = updatedProfile
        try{
            saveProfile(updatedProfile) { success ->
                if (!success) {
                    _profileData.value = oldProfile
                }
            }
            onResult(true)
        } catch (e: Exception) {
            println("$TAG: Error saving RSVP: ${e.message}")
            onResult(false)
        }
    }

    companion object {
        private const val TAG = "AuthPageViewModel"
    }

}

sealed class LoginUIState {
    data object Idle : LoginUIState()
    data object Loading : LoginUIState()
    data object Authenticated : LoginUIState()
    data class Error(val message: String) : LoginUIState()
}