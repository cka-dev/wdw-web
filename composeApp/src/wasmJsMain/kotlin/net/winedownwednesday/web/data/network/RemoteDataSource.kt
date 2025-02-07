package net.winedownwednesday.web.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import net.winedownwednesday.web.FirebaseBridge
import net.winedownwednesday.web.data.AboutItem
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.data.models.AuthenticationResponse
import net.winedownwednesday.web.data.models.FcmInstanceRegistrationRequest
import net.winedownwednesday.web.data.models.FirebaseAuthResponse
import net.winedownwednesday.web.data.models.PublicKeyCredentialCreationOptions
import net.winedownwednesday.web.data.models.PublicKeyCredentialRequestOptions
import net.winedownwednesday.web.data.models.RSVPRequest
import net.winedownwednesday.web.data.models.RegistrationOptionsRequest
import net.winedownwednesday.web.data.models.RegistrationResponse
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.data.models.VerifyAuthenticationRequest
import net.winedownwednesday.web.data.models.VerifyRegistrationRequest
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Single

@Single
class RemoteDataSource (
    @InjectedParam private val client: HttpClient
): ApiService {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    override suspend fun fetchEpisodes(): List<Episode>? {
        _isLoading.value = true
        return try {
            client.get("$SERVER_URL/getEpisodes"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
            e.message?.let { Logger.SIMPLE.log(it) }
            null
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun fetchAboutItems(): List<AboutItem> {
        _isLoading.value = true
        return try {
            client.get("$SERVER_URL/getAboutItems"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
            e.message?.let { Logger.SIMPLE.log(it) }
            listOf()
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun fetchWines(): List<Wine>? {
        _isLoading.value = true
        return try {
            client.get("$SERVER_URL/getWines"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
            e.message?.let { Logger.SIMPLE.log(it) }
            null
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun fetchMembers(): List<Member>? {
        _isLoading.value = true
        try {
            return client.get("$SERVER_URL/getMembers"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
            e.message?.let { Logger.SIMPLE.log(it) }
        } finally {
            _isLoading.value = false
        }
        return listOf()
    }

    override suspend fun fetchEvents(): List<Event>? {
        _isLoading.value = true
        return try {
            client.get("$SERVER_URL/getEvents"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
//            println("Error fetching events: ${e.message}")
            _error.value = e.message ?: "Unknown error occurred"
            e.message?.let { Logger.SIMPLE.log(it) }
            null
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun postRSVP(rsvp: RSVPRequest): Boolean {
        _isLoading.value = true
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/postRsvp") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(rsvp)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            _error.value = e.message
            false
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun generatePasskeyRegistrationOptions(email: String):
            ApiResult<PublicKeyCredentialCreationOptions> {
        return try {
            val response: HttpResponse = client.post(
                "$SERVER_URL/generatePasskeyRegistrationOptions") {
                contentType(ContentType.Application.Json)
                setBody(RegistrationOptionsRequest(email))
            }
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                if (response.status == HttpStatusCode.Conflict) {
                    ApiResult.Error("An account already exists with that email. " +
                            "Please log in instead.")
                } else {
                    ApiResult.Error("Failed to generate registration options: $errorBody")
                }
            } else {
                val options: PublicKeyCredentialCreationOptions = response.body()
                ApiResult.Success(options)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun verifyPasskeyRegistration(
        credential: RegistrationResponse, email: String): Boolean {
        _isLoading.value = true
        return try {
            val response: HttpResponse = client.post(
                "$SERVER_URL/verifyPasskeyRegistration") {
                contentType(ContentType.Application.Json)
                setBody(VerifyRegistrationRequest(credential, email))
            }
            response.body<Map<String, Boolean>>()["verified"] ?: false
        } catch (e: Exception) {
            _error.value = e.message
            false
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun generatePasskeyAuthenticationOptions(
        email: String
    ): ApiResult<PublicKeyCredentialRequestOptions> {
        return try {
            val response: HttpResponse = client.post(
                "$SERVER_URL/generatePasskeyAuthenticationOptions") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }
            if (!response.status.isSuccess()) {
                if (response.status == HttpStatusCode.NotFound) {
                    val errorMessage = "Email is not registered. Please double check for typos" +
                            " or register a new account"
                    ApiResult.Error(errorMessage)
                } else {
//                    println("$TAG: response status: ${response.status}")
                    ApiResult.Error(
                        "Failed to generate authentication options: ${response.bodyAsText()}")
                }
            } else {
                val options: PublicKeyCredentialRequestOptions = response.body()
                ApiResult.Success(options)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error occurred")
        }
    }


    override suspend fun verifyPasskeyAuthentication(
        credential: AuthenticationResponse, email: String): Boolean {
        _isLoading.value = true
        return try {
            val response: HttpResponse = client.post(
                "$SERVER_URL/verifyPasskeyAuthentication") {
                contentType(ContentType.Application.Json)
                setBody(VerifyAuthenticationRequest(credential, email))
            }
            response.body<Map<String, Boolean>>()["verified"] ?: false
        } catch (e: Exception) {
            _error.value = e.message
            false
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun fetchUserProfile(userEmail: String): UserProfileData? {
        return try {
            val response = client.post("$SERVER_URL/fetchUserProfile"){
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("email", userEmail)
                }
            }
            val jsonString = response.bodyAsText()
            Json.decodeFromString<UserProfileData>(jsonString)
        } catch (e: Exception) {
//            println("Error fetching profile: ${e.message}")
            null
        }
    }

    override suspend fun updateProfile(profileData: UserProfileData): Boolean {
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/updateUserProfile") {
                contentType(ContentType.Application.Json)
                setBody(profileData)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addRsvpToEvent(rsvp: RSVPRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/addRsvpToEvent") {
                contentType(ContentType.Application.Json)
                setBody(rsvp)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun registerFcmInstanceId(request: FcmInstanceRegistrationRequest): Boolean {
        try {
            val response = client.post("$SERVER_URL/registerFcmInstanceId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                return true
            } else {
//                println("$TAG:Error registering FCM token: ${response.status}")
            }
        } catch (e: Exception) {
//            println("$TAG:Exception while registering FCM token: $e")
        }
        return false
    }

    override suspend fun unRegisterFcmInstanceId(request: FcmInstanceRegistrationRequest): Boolean {
        try {
            val response = client.post("$SERVER_URL/unRegisterFcmInstanceId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                return true
            } else {
//                println("$TAG: Error unregistering FCM token: ${response.status}")
            }
        } catch (e: Exception) {
//            println("$TAG:Exception whilst unregistering FCM token: $e")
        }
        return false
    }

    override suspend fun sendEmailVerification(email: String): Boolean {
        try {
            val response = client.post("$SERVER_URL/sendEmailVerification") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }
            if (response.status.isSuccess()) {
                return true
            } else {
//                println("$TAG, Error sending verification email: ${response.status}")
            }
        } catch (e: Exception) {
//            println("$TAG, Exception while sending verification email: $e")
        } finally {
        }
        return false
    }

    suspend fun generatePasskeyAuthenticationOptions2(
        email: String
    ): ApiResult<PublicKeyCredentialRequestOptions> {
        return try {
            val response: HttpResponse = client.post(
                "$SERVER_URL/generatePasskeyAuthenticationOptions2") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }
            if (!response.status.isSuccess()) {
                if (response.status == HttpStatusCode.NotFound) {
                    val errorMessage = "Email is not registered. Please double check for typos" +
                            " or register a new account"
                    ApiResult.Error(errorMessage)
                } else {
//                    println("$TAG: response status: ${response.status}")
                    ApiResult.Error(
                        "Failed to generate authentication options: ${response.bodyAsText()}")
                }
            } else {
                val options: PublicKeyCredentialRequestOptions = response.body()
                ApiResult.Success(options)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun generatePasskeyRegistrationOptions2(email: String):
            ApiResult<PublicKeyCredentialCreationOptions> {
        return try {
            val response: HttpResponse = client.post(
                "$SERVER_URL/generatePasskeyRegistrationOptions2") {
                contentType(ContentType.Application.Json)
                setBody(RegistrationOptionsRequest(email))
            }
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                if (response.status == HttpStatusCode.Conflict) {
                    ApiResult.Error("An account already exists with that email. " +
                            "Please log in instead.")
                } else {
                    ApiResult.Error("Failed to generate registration options: $errorBody")
                }
            } else {
                val options: PublicKeyCredentialCreationOptions = response.body()
                ApiResult.Success(options)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun verifyPasskeyRegistrationWithToken(
        credential: RegistrationResponse, email: String): ApiResult<FirebaseAuthResponse> {
        _isLoading.value = true
        return try {
            val response: HttpResponse = client.post(
                "$SERVER_URL/verifyPasskeyRegistrationWithFirebaseAuth") {
                contentType(ContentType.Application.Json)
                setBody(VerifyRegistrationRequest(credential, email))
            }
            if (response.status.isSuccess()) {
                val authResponse: FirebaseAuthResponse = response.body()
                ApiResult.Success(authResponse)
            } else {
                ApiResult.Error("Passkey registration failed: ${response.status}")
            }
        } catch (e: Exception) {
            _error.value = e.message
            ApiResult.Error(e.message ?: "Unknown error during registration verification")
        } finally {
            _isLoading.value = false
        }
    }


    override suspend fun verifyPasskeyAuthenticationWithToken(
        credential: AuthenticationResponse, email: String): ApiResult<FirebaseAuthResponse> {
        _isLoading.value = true
        return try {
            val response: HttpResponse = client.post(
                "$SERVER_URL/verifyPasskeyAuthenticationWithFirebaseAuth") {
                contentType(ContentType.Application.Json)
                setBody(VerifyAuthenticationRequest(credential, email))
            }

            if (response.status.isSuccess()) {
                val authResponse : FirebaseAuthResponse = response.body()
                try {
                    FirebaseBridge.signInWithCustomToken(authResponse.token)
//                    println("$TAG: Signed in with custom token")
                } catch (e: Exception) {
//                    println("$TAG: Error signing in with custom token: ${e.message}")
                }
                ApiResult.Success(authResponse)
            }
            else{
                ApiResult.Error(
                    "Passkey authentication failed with server response: ${response.status}")
            }

        } catch (e: Exception) {
            _error.value = e.message
            ApiResult.Error(e.message ?: "Unknown error during authentication verification")
        } finally {
            _isLoading.value = false
        }
    }

    companion object{
        private const val SERVER_URL =
            "https://us-central1-wdw-app-52a3c.cloudfunctions.net"
//        private const val SERVER_URL =
//            "http://127.0.0.1:5001/wdw-app-52a3c/us-central1"

        private const val TAG = "RemoteDataSource"
    }
}

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}