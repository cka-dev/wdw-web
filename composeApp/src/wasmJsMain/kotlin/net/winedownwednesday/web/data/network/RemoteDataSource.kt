package net.winedownwednesday.web.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
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
import net.winedownwednesday.web.data.models.ChangePasswordRequest
import net.winedownwednesday.web.data.models.EmailPasswordRequest
import net.winedownwednesday.web.data.models.FcmInstanceRegistrationRequest
import net.winedownwednesday.web.data.models.FirebaseAuthResponse
import net.winedownwednesday.web.data.models.PublicKeyCredentialCreationOptions
import net.winedownwednesday.web.data.models.PublicKeyCredentialRequestOptions
import net.winedownwednesday.web.data.models.RSVPRequest
import net.winedownwednesday.web.data.models.RegistrationOptionsRequest
import net.winedownwednesday.web.data.models.RegistrationResponse
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.data.models.FeaturedWinesResponse
import net.winedownwednesday.web.data.models.BlogPostsResponse
import net.winedownwednesday.web.data.models.BlogPost
import net.winedownwednesday.web.data.models.UserProfileRequest
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
            null
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun fetchBlogPosts(): BlogPostsResponse? {
        _isLoading.value = true
        return try {
            client.get("$SERVER_URL/getBlogPosts"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
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
            null
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun fetchFeaturedWines(): FeaturedWinesResponse? {
        _isLoading.value = true
        return try {
            client.get("$SERVER_URL/getFeaturedWines"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
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
            _error.value = e.message ?: "Unknown error occurred"
            null
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun fetchMemberSpotlight(): Member? {
        _isLoading.value = true
        return try {
            client.get("$SERVER_URL/getMemberSpotlight"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
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
                if (response.status == HttpStatusCode.Conflict) {
                    ApiResult.Error("An account already exists with that email. " +
                            "Please log in instead.")
                } else {
                    ApiResult.Error("Failed to generate registration options")
                }
            } else {
                val options: PublicKeyCredentialCreationOptions = response.body()
                ApiResult.Success(options)
            }
        } catch (e: Exception) {
            ApiResult.Error("Unknown error occurred")
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
                    ApiResult.Error("Failed to generate authentication options")
                }
            } else {
                val options: PublicKeyCredentialRequestOptions = response.body()
                ApiResult.Success(options)
            }
        } catch (e: Exception) {
            ApiResult.Error("Unknown error occurred")
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
                header("x-user-email", userEmail)
                url { parameters.append("email", userEmail) }
                setBody(UserProfileRequest(userEmail))
            }
            if (response.status.isSuccess()) {
                val jsonString = response.bodyAsText()
                Json.decodeFromString<UserProfileData>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
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
            }
        } catch (e: Exception) {
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
            }
        } catch (e: Exception) {
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
            }
        } catch (e: Exception) {
        }
        return false
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
                ApiResult.Error("Passkey registration failed")
            }
        } catch (e: Exception) {
            _error.value = e.message
            ApiResult.Error("Unknown error during registration verification")
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
                ApiResult.Success(authResponse)
            }
            else{
                ApiResult.Error("Passkey auth failed")
            }

        } catch (e: Exception) {
            _error.value = e.message
            ApiResult.Error("Unknown error during authentication verification")
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun registerWithEmailPassword(request: EmailPasswordRequest): ApiResult<FirebaseAuthResponse> {
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/registerWithEmailPassword") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body())
            } else {
                ApiResult.Error("Registration failed")
            }
        } catch (e: Exception) {
            ApiResult.Error("Registration failed")
        }
    }

    override suspend fun signInWithEmailPassword(request: EmailPasswordRequest): ApiResult<FirebaseAuthResponse> {
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/signInWithEmailPassword") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body())
            } else {
                ApiResult.Error("Sign in failed")
            }
        } catch (e: Exception) {
            ApiResult.Error("Sign in failed")
        }
    }

    override suspend fun linkPasswordToAccount(request: EmailPasswordRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/linkPasswordToAccount") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun changePassword(request: ChangePasswordRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/changePassword") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/sendPasswordResetEmail") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    companion object{
        private const val SERVER_URL =
            "https://us-central1-wdw-app-52a3c.cloudfunctions.net"

        private const val TAG = "RemoteDataSource"
    }
}

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}