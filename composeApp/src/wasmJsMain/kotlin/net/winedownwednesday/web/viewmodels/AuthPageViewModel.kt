package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.winedownwednesday.web.FirebaseBridge
import net.winedownwednesday.web.FirebaseUser
import net.winedownwednesday.web.PublicKeyCredential
import net.winedownwednesday.web.data.models.AuthenticationResponse
import net.winedownwednesday.web.data.models.FcmInstanceRegistrationRequest
import net.winedownwednesday.web.data.models.FirebaseAuthResponse
import net.winedownwednesday.web.data.models.RSVPRequest
import net.winedownwednesday.web.data.models.RegistrationResponse
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.data.network.ApiResult
import net.winedownwednesday.web.data.repositories.AppRepository
import net.winedownwednesday.web.myWebAuthnBridge
import net.winedownwednesday.web.toBase64Url
import kotlin.coroutines.resumeWithException

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

    private val _fcmToken = MutableStateFlow<String?>(null)
    private val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _isFetchingProfile = MutableStateFlow(false)
    val isFetchingProfile: StateFlow<Boolean> = _isFetchingProfile.asStateFlow()

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                FirebaseBridge.waitUntilInitialized().await()
            } catch (e: Exception) {
//                println("Error initializing Firebase: ${e.message}")
            }

            try {
                observeAuthState()
            } catch (e: Exception) {
//                println("Error observing auth state: ${e.message}")
            }

            try {
                FirebaseBridge.getCurrentUser()?.let { user ->
                    if (user != null) {
                        val firebaseUser = user.toFirebaseUser()
                        _uiState.value = LoginUIState.Authenticated
                        setEmail(firebaseUser.email ?: "")
                        fetchProfile(userEmail = firebaseUser.email ?: "")
                        getAndRegisterFcmToken()
                    } else {
                        _uiState.value = LoginUIState.Idle
                    }
                }
            } catch (e: Exception) {
//                println("AuthStateChanged: Error getting current user: ${e.message}")
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            try {
                FirebaseBridge.observeAuthState { user ->
                    if (user != null) {
                        val firebaseUser = user.toFirebaseUser()
                        _uiState.value = LoginUIState.Authenticated
                        setEmail(firebaseUser.email ?: "")
                        fetchProfile(userEmail = firebaseUser.email ?: "")
                    } else {
                        _uiState.value = LoginUIState.Idle
                    }
                }
            } catch (e: Exception) {
//                println("AuthStateChanged: Error observing auth state: ${e.message}")
            }
        }
    }

    fun fetchProfile(userEmail: String) {
        viewModelScope.launch {
            _isFetchingProfile.value = true
            try {
                val profile = repository.fetchProfileFromServer(userEmail)
                _profileData.value = profile
            } catch (e: Exception) {
                _isFetchingProfile.value = false
            } finally {
                _isFetchingProfile.value = false
            }
        }
    }

    fun saveProfile(userProfileData: UserProfileData, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSavingProfile.value = true
            try {
                val success = repository.saveProfileToServer(userProfileData)
                if (success) {
                    _profileData.value = userProfileData
                }
                onResult(success)
            } catch (e: Exception) {
                onResult(false)
                _isSavingProfile.value = false
            } finally {
                _isSavingProfile.value = false
            }
        }
    }

    fun registerPasskey(email: String) {
        viewModelScope.launch {
            _uiState.value = LoginUIState.Loading

            when (val result = repository.generatePasskeyRegistrationOptions(email)) {
                is ApiResult.Error -> {
                    _uiState.value = LoginUIState.Error(result.message)
                }
                is ApiResult.Success -> {
                    val options = result.data

                    try {
                        val credential = myWebAuthnBridge.startRegistration(
                            challenge = options.challenge,
                            rpId = options.rp.id,
                            rpName = options.rp.name,
                            userId = options.user.id,
                            userName = options.user.name,
                            userDisplayName = options.user.displayName,
                            timeout = options.timeout ?: 60000,
                            attestationType = options.attestation,
                            authenticatorAttachment =
                            options.authenticatorSelection.authenticatorAttachment,
                            residentKey = options.authenticatorSelection.residentKey,
                            requireResidentKey = options.authenticatorSelection.requireResidentKey,
                            userVerification = options.authenticatorSelection.userVerification
                        ).await<PublicKeyCredential>()

                        val registrationResponse = try {
                            credential.toRegistrationResponse()
                        } catch (e: Exception) {
                            throw e
                        }

                        val verified = repository.verifyPasskeyRegistration(
                            registrationResponse, email)
                        if (verified) {
                            _uiState.value = LoginUIState.Authenticated
                        } else {
                            _uiState.value = LoginUIState.Error("Passkey registration failed")
                        }
                    } catch (e: Exception) {
                        _uiState.value = LoginUIState.Error(
                            "Error during registration: ${e.message}")
                    }
                }
            }
        }
    }

    fun authenticateWithPasskey(email: String) {
        viewModelScope.launch {
            _uiState.value = LoginUIState.Loading

            when (val result = repository.generatePasskeyAuthenticationOptions(email)) {
                is ApiResult.Error -> {
                    _uiState.value = LoginUIState.Error(result.message)
                }
                is ApiResult.Success -> {
                    val options = result.data

                    val allowCredentialIds = options.allowCredentials?.joinToString(
                        ",", "[", "]") {
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

                        val verified = repository.verifyPasskeyAuthentication(
                            authenticationResponse, email)
                        if (verified) {
                            _uiState.value = LoginUIState.Authenticated
                        } else {
                            _uiState.value = LoginUIState.Error("Passkey authentication failed")
                        }
                    } catch (e: Exception) {
                        _uiState.value = LoginUIState.Error(
                            "Error during authentication: ${e.message}")
                    }
                }
            }
        }
    }

    fun registerPasskeyV2(email: String){
        viewModelScope.launch {
            _uiState.value = LoginUIState.Loading

            when (val result = repository.generatePasskeyRegistrationOptions(email)) {
                is ApiResult.Error -> {
                    _uiState.value = LoginUIState.Error(result.message)
                }
                is ApiResult.Success -> {
                    val options = result.data

                    try {
                        val credential = myWebAuthnBridge.startRegistration(
                            challenge = options.challenge,
                            rpId = options.rp.id,
                            rpName = options.rp.name,
                            userId = options.user.id,
                            userName = options.user.name,
                            userDisplayName = options.user.displayName,
                            timeout = options.timeout ?: 60000,
                            attestationType = options.attestation,
                            authenticatorAttachment =
                            options.authenticatorSelection.authenticatorAttachment,
                            residentKey = options.authenticatorSelection.residentKey,
                            requireResidentKey = options.authenticatorSelection.requireResidentKey,
                            userVerification = options.authenticatorSelection.userVerification
                        ).await<PublicKeyCredential>()

                        val registrationResponse = try {
                            credential.toRegistrationResponse()
                        } catch (e: Exception) {
                            throw e
                        }

                        val verificationResponse: ApiResult<FirebaseAuthResponse> =
                            repository.verifyPasskeyRegistrationWithToken(
                                registrationResponse, email)
                        if (verificationResponse is ApiResult.Success) {
                            val firebaseAuthResponse = verificationResponse.data
                            _uiState.value = LoginUIState.Authenticated
                            fetchProfile(userEmail = email)
                            FirebaseBridge.signInWithCustomToken(firebaseAuthResponse.token)
                        } else {
                            _uiState.value = LoginUIState.Error("Passkey registration failed")
                        }
                    } catch (e: Exception) {
                        _uiState.value = LoginUIState.Error(
                            "Error during registration: ${e.message}")
                    }
                }
            }
        }
    }

    fun authenticateWithPasskeyV2(email: String) {
        viewModelScope.launch {
            _uiState.value = LoginUIState.Loading

            when (val result = repository.generatePasskeyAuthenticationOptions(email)) {
                is ApiResult.Error -> {
                    _uiState.value = LoginUIState.Error(result.message)
                }
                is ApiResult.Success -> {
                    val options = result.data

                    val allowCredentialIds = options.allowCredentials?.joinToString(
                        ",", "[", "]") {
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

                        val verificationResponse: ApiResult<FirebaseAuthResponse> =
                            repository.verifyPasskeyAuthenticationWithToken(
                                authenticationResponse, email)
                        if (verificationResponse is ApiResult.Success) {
                            val firebaseAuthResponse = verificationResponse.data
                            _uiState.value = LoginUIState.Authenticated
                            fetchProfile(userEmail = email)
                            FirebaseBridge.signInWithCustomToken(firebaseAuthResponse.token)
                        } else {
                            _uiState.value = LoginUIState.Error(
                                "$TAG Passkey authentication failed")
                        }
                    } catch (e: Exception) {
                        _uiState.value = LoginUIState.Error(
                            "$TAG Error during authentication: ${e.message}")
                    }
                }
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
//                println("$TAG: Caught exception: $e")
                throw e
            }

        return authenticationResponse
    }

    fun simulateAuthentication(email: String) {
        viewModelScope.launch {
            delay(500)
            _uiState.value = LoginUIState.Authenticated
            fetchProfile(userEmail = email)
        }
    }

    fun simulateAuthenticationError(email: String) {
        viewModelScope.launch {
            delay(500)
            _uiState.value = LoginUIState.Error("Email is not registered. Please double check" +
                    " for typos or register a new account.")
        }
    }

    fun simulateRegistrationError(email: String) {
        viewModelScope.launch {
            delay(500)
            _uiState.value = LoginUIState.Error("An account already exists with that email." +
                    "Please log in instead.")
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

    suspend fun logout() {
            try {
                unRegisterFcmInstanceId(
                    fcmToken = fcmToken.value ?: "",
                    email = email.value
                )
            } catch (e: Exception) {
//                println("Error unregistering FCM instance ID: $e")
            }

            try {
                firebaseSignOut()
//                println("User signed out successfully.")
            } catch (e: Exception) {
//                println("Error signing out: $e")
            }

            _uiState.value = LoginUIState.Idle
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
//            println("$TAG: Error saving RSVP: ${e.message}")
            onResult(false)
        }
    }

    fun requestNotificationPermissionAndGetToken() {
        viewModelScope.launch {
            try {
                val permissionResultJsAny: JsAny? =
                    FirebaseBridge.requestNotificationPermission().await()
                val permissionResult = permissionResultJsAny?.unsafeCast<JsAny>()

                if (permissionResult.toString() == "granted") {
                    getAndRegisterFcmToken()
                } else {
//                    println("Notifications not granted, permissionResult=$permissionResult")
                }
            } catch (e: Exception) {
//                println("Error while requesting permission or getting token: $e")
            }
        }
    }

    private fun getAndRegisterFcmToken() {
        viewModelScope.launch {
            val tokenJsAny: JsAny? = FirebaseBridge.getFcmToken().await()
            val token = tokenJsAny?.unsafeCast<JsAny>()
            _fcmToken.value = token.toString()
            if (token != null) {
                try {
                    registerFcmInstanceId(token.toString())
                } catch (e: Exception) {
//                    println("Error registering FCM instance ID: $e")
                }
            }
        }
    }

    private fun registerFcmInstanceId(token: String) {
        viewModelScope.launch {
            val requestBody = FcmInstanceRegistrationRequest(
                instanceId = token,
                email = email.value
            )
            val request= repository.registerFcmInstanceId(requestBody)
            if (!request) {
                println("$TAG: Error registering FCM instance ID")
            }
        }
    }

    private suspend fun unRegisterFcmInstanceId(fcmToken: String, email: String) {
        val requestBody = FcmInstanceRegistrationRequest(
            instanceId = fcmToken,
            email = email
        )
        val request = repository.unRegisterFcmInstanceId(requestBody)
        if (!request) {
            println("$TAG: Error unregistering FCM instance ID")
        }
    }

    private suspend fun firebaseSignOut(): JsAny? = suspendCancellableCoroutine { continuation ->
        FirebaseBridge.signOut().then(
            onFulfilled = { result: JsAny? ->
                continuation.resumeWith(Result.success(result))
                null
            },
            onRejected = { error ->
                val exception = Exception("Firebase sign out failed: $error")
                continuation.resumeWithException(exception)
                null
            }
        ).catch { error ->
            val exception = Exception("Error processing Firebase sign out Promise: $error")
            continuation.resumeWithException(exception)
            null
        }

        continuation.invokeOnCancellation {
//            println("Sign out coroutine cancelled")
        }
    }

    fun sendVerificationEmail(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.sendEmailVerification(email)
                onResult(success)
            } catch (e: Exception) {
//                println("$TAG, Error sending verification email: ${e.message}")
                onResult(false)
            }
        }
    }

    private fun JsAny.toFirebaseUser(): FirebaseUser = this.unsafeCast<FirebaseUser>()

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            val currentEmail = email.value
            val currentToken = fcmToken.value ?: ""
            if (currentEmail.isNotBlank() && currentToken.isNotBlank()) {
                try {
                    unRegisterFcmInstanceId(
                        fcmToken = currentToken,
                        email = currentEmail
                    )
                } catch (e: Exception) {
//                    println("Error unregistering FCM instance ID in onCleared: ${e.message}")
                }
            } else {
//                println("FCM token or email missing during onCleared; skipping unregister.")
            }
            firebaseSignOut()
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