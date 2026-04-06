package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.winedownwednesday.web.FirebaseBridge
import net.winedownwednesday.web.FirebaseUser
import net.winedownwednesday.web.PublicKeyCredential
import net.winedownwednesday.web.data.models.AuthenticationResponse
import net.winedownwednesday.web.data.models.ChangePasswordRequest
import net.winedownwednesday.web.data.models.EmailPasswordRequest
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

//    private val userName

    init {
        viewModelScope.launch {
            try {
                FirebaseBridge.waitUntilInitialized().await<JsAny?>()
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
                    val firebaseUser = user.toFirebaseUser()
                    val userEmail = firebaseUser.email
                    if (!userEmail.isNullOrBlank()) {
                        setEmail(userEmail)
                        fetchProfile(userEmail = userEmail)
                        getAndRegisterFcmToken()
                        _uiState.value = LoginUIState.Authenticated
                    }
                }
            } catch (e: Exception) {
//                println("AuthStateChanged: Error getting current user: ${e.message}")
            }
        }
    }

    private fun observeAuthState() {
        FirebaseBridge.observeAuthState { user ->
            viewModelScope.launch {
                if (user != null) {
                    val firebaseUser = user.toFirebaseUser()
                    val userEmail = firebaseUser.email
                    if (!userEmail.isNullOrBlank()) {
                        setEmail(userEmail)
                        if (_profileData.value == null) {
                            fetchProfile(userEmail = userEmail)
                        }
                        _uiState.value = LoginUIState.Authenticated
                    }
                } else {
                    if (_uiState.value is LoginUIState.Authenticated) {
                        _uiState.value = LoginUIState.Idle
                        _profileData.value = null
                    }
                }
            }
        }
    }

    fun fetchProfile(userEmail: String) {
        if (userEmail.isBlank()) return
        viewModelScope.launch {
            _isFetchingProfile.value = true
            try {
                val profile = repository.fetchProfileFromServer(userEmail)
                _profileData.value = profile
            } catch (e: Exception) {
                // println("$TAG: fetchProfile EXCEPTION: ${e.message}")
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

    suspend fun signInWithCustomToken(token: String, email: String) {
        if (email.isBlank()) return
        try {
            FirebaseBridge.signInWithCustomToken(token).await<JsAny?>()
            val profile = repository.fetchProfileFromServer(email)
            _profileData.value = profile
            setEmail(email)
            _uiState.value = LoginUIState.Authenticated
        } catch (e: Exception) {
            _uiState.value = LoginUIState.Error("Sign in failed: ${e.message}")
        }
    }

    fun registerPasskeyV2(email: String){
        if (email.isBlank()) {
            _uiState.value = LoginUIState.Error("Email is required")
            return
        }
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
                            signInWithCustomToken(verificationResponse.data.token, email)
                        } else {
                            val msg = (verificationResponse as? ApiResult.Error)?.message ?: "Passkey registration failed"
                            _uiState.value = LoginUIState.Error(msg)
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
        if (email.isBlank()) {
            _uiState.value = LoginUIState.Error("Email is required")
            return
        }
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
                            signInWithCustomToken(verificationResponse.data.token, email)
                        } else {
                            val msg = (verificationResponse as? ApiResult.Error)?.message ?: "Passkey authentication failed"
                            _uiState.value = LoginUIState.Error(msg)
                        }
                    } catch (e: Exception) {
                        _uiState.value = LoginUIState.Error(
                            "Error during authentication: ${e.message}")
                    }
                }
            }
        }
    }

    fun registerWithEmailPassword(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            _uiState.value = LoginUIState.Loading
            val result = repository.registerWithEmailPassword(EmailPasswordRequest(email, password))
            when (result) {
                is ApiResult.Success -> {
                    signInWithCustomToken(result.data.token, email)
                }
                is ApiResult.Error -> {
                    _uiState.value = LoginUIState.Error(result.message)
                }
            }
        }
    }

    fun signInWithEmailPassword(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            _uiState.value = LoginUIState.Loading
            val result = repository.signInWithEmailPassword(EmailPasswordRequest(email, password))
            when (result) {
                is ApiResult.Success -> {
                    signInWithCustomToken(result.data.token, email)
                }
                is ApiResult.Error -> {
                    _uiState.value = LoginUIState.Error(result.message)
                }
            }
        }
    }

    fun linkPasswordToAccount(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val email = _email.value
            if (email.isBlank() || password.isBlank()) {
                onResult(false)
                return@launch
            }
            val success = repository.linkPasswordToAccount(EmailPasswordRequest(email, password))
            if (success) {
                fetchProfile(email)
            }
            onResult(success)
        }
    }

    fun changePassword(currentPassword: String?, newPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val email = _email.value
            if (email.isBlank() || newPassword.isBlank()) {
                onResult(false)
                return@launch
            }
            val success = repository.changePassword(ChangePasswordRequest(email, currentPassword, newPassword))
            onResult(success)
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean) -> Unit) {
        if (email.isBlank()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val success = repository.sendPasswordResetEmail(email)
            onResult(success)
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

    fun resetToIdle() {
        _uiState.value = LoginUIState.Idle
    }

    fun checkIsNewUser(isNewUser: Boolean) {
        _isNewUser.value = isNewUser
    }

    suspend fun logout() {
            try {
                val currentEmail = email.value
                val currentToken = fcmToken.value
                if (!currentEmail.isNullOrBlank() && !currentToken.isNullOrBlank()) {
                    unRegisterFcmInstanceId(
                        fcmToken = currentToken,
                        email = currentEmail
                    )
                }
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
            _profileData.value = null
    }

    fun setEmail(email: String) {
        _email.value = email
    }

    fun hasUserRsvped(eventId: Long?): Boolean {
        val currentProfile = _profileData.value ?: return false
        if (eventId == null) return false
        return currentProfile.eventRsvps?.containsKey(eventId) ?: false
    }

    fun getRsvpForEvent(eventId: Long?): RSVPRequest? {
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
                    FirebaseBridge.requestNotificationPermission().await<JsString?>()
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
            val tokenJsAny: JsAny? = FirebaseBridge.getFcmToken().await<JsAny?>()
            val token = tokenJsAny?.unsafeCast<JsAny>()
            val tokenStr = token.toString()
            if (token != null && tokenStr.isNotBlank()) {
                _fcmToken.value = tokenStr
                try {
                    val currentEmail = email.value
                    if (currentEmail.isNotBlank()) {
                        registerFcmInstanceId(tokenStr, currentEmail)
                    }
                } catch (e: Exception) {
//                    println("Error registering FCM instance ID: $e")
                }
            }
        }
    }

    private fun registerFcmInstanceId(token: String, emailAddr: String) {
        viewModelScope.launch {
            val requestBody = FcmInstanceRegistrationRequest(
                instanceId = token,
                email = emailAddr
            )
            val request= repository.registerFcmInstanceId(requestBody)
            if (!request) {
                // println("$TAG: Error registering FCM instance ID")
            }
        }
    }

    private suspend fun unRegisterFcmInstanceId(fcmToken: String, email: String) {
        if (fcmToken.isBlank() || email.isBlank()) return
        val requestBody = FcmInstanceRegistrationRequest(
            instanceId = fcmToken,
            email = email
        )
        val request = repository.unRegisterFcmInstanceId(requestBody)
        if (!request) {
            // println("$TAG: Error unregistering FCM instance ID")
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
        if (email.isBlank()) {
            onResult(false)
            return
        }
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
        // Note: we intentionally do NOT sign out here.
        // Firebase auth session should persist across ViewModel GC;
        // only an explicit call to logout() should end the session.
    }

    fun deleteAccount(
        confirmPhrase: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.deleteAccount(confirmPhrase)
            if (success) {
                firebaseSignOut()
            }
            onComplete(success)
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