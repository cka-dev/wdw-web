package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.PublicKeyCredential
import net.winedownwednesday.web.data.models.AuthenticationResponse
import net.winedownwednesday.web.data.models.RegistrationResponse
import net.winedownwednesday.web.data.repositories.AppRepository
import net.winedownwednesday.web.myWebAuthnBridge
import net.winedownwednesday.web.toBase64Url

class AuthPageViewModel(
    private val repository: AppRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUIState>(LoginUIState.Idle)
    val uiState = _uiState.asStateFlow()

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
                    userId = options.user.id.toBase64Url(),
                    userName = options.user.name,
                    userDisplayName = options.user.displayName,
                    timeout = options.timeout ?: 60000,
                    attestationType = options.attestation,
                    authenticatorAttachment = options.authenticatorSelection.authenticatorAttachment,
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
                println("$TAG: Caught exception: $e")
            }
        }
    }

    fun authenticateWithPasskey(email: String) {
        viewModelScope.launch {
            _uiState.value = LoginUIState.Loading
            try {
                val options = repository.generatePasskeyAuthenticationOptions(email)
                    ?: throw Exception("Failed to generate authentication options")

                println("Authentication Options: $options")

                val allowCredentialIds = options.allowCredentials?.joinToString(",", "[", "]") {
                    "\"" + it.id.toBase64Url() + "\""
                } ?: "[]"

                println("Calling WebAuthn.startAuthentication with options: $options")

                try {
                    val credential = myWebAuthnBridge.startAuthentication(
                        challenge = options.challenge,
                        rpId = options.rpId,
                        timeout = options.timeout ?: 60000,
                        userVerification = options.userVerification,
                        allowCredentialIds = allowCredentialIds
                    ).await<PublicKeyCredential>()

                    println("$TAG: Credential: $credential")

                    val authenticationResponse = try {
                        credential.toAuthenticationResponse()
                    } catch (e: Exception) {
                        println("$TAG: Caught exception: $e")
                        throw e
                    }

                    val verified = repository.verifyPasskeyAuthentication(authenticationResponse, email)
                    if (verified) {
                        _uiState.value = LoginUIState.Authenticated
                    } else {
                        _uiState.value = LoginUIState.Error("Passkey authentication failed")
                    }

                } catch (e: Exception) {
                    println("Error during authentication: ${e.message}")
                }

                println("$TAG: Outside of try/catch block")


            } catch (e: Exception) {
                _uiState.value = LoginUIState.Error("Error during authentication: ${e.message}")
            }
        }
    }

    private fun JsAny.toRegistrationResponse(): RegistrationResponse {
        val credential = this.unsafeCast<PublicKeyCredential>()
        val response = credential.response

        return RegistrationResponse(
            id = credential.id,
            rawId = credential.rawId.toBase64Url(),
            type = credential.type,
            response = RegistrationResponse.ResponseData(
                clientDataJSON = response.clientDataJSON.toBase64Url(),
                attestationObject = response.attestationObject?.toBase64Url() ?: "",
            ),
        )
    }

    private fun JsAny.toAuthenticationResponse(): AuthenticationResponse {
        println("$TAG: Converting to AuthenticationResponse")
        val credential = this.unsafeCast<PublicKeyCredential>()

        println("$TAG: Credential: $credential")

        val response = credential.response
        if (response.authenticatorData == null || response.signature == null) {
            println("$TAG: Response is null")
            throw IllegalStateException("AuthenticatorResponse is null")
        }

        println("$TAG: Response: $response")

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
        println("$TAG: AuthenticationResponse: $authenticationResponse")
        return authenticationResponse
    }

    private fun ByteArray.encode(): String {
        return this.joinToString("") { byte ->
            byte.toInt().and(0xFF).toString(16).padStart(2, '0')
        }
    }

    fun checkSecureContext() {
        viewModelScope.launch {
            val isSecure = myWebAuthnBridge.isSecureContext()
            println("Is secure context: $isSecure")
        }
    }

    companion object {
        private const val TAG = "AuthPageViewModel"
    }

}

sealed class LoginUIState {
    object Idle : LoginUIState()
    object Loading : LoginUIState()
    object Authenticated : LoginUIState()
    data class Error(val message: String) : LoginUIState()
}

fun ByteArray.toBase64Url(): String {
    return window.btoa(this.decodeToString())
        .replace('+', '-')
        .replace('/', '_')
        .replace("=", "")
}
