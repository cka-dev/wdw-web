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
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import net.winedownwednesday.web.data.AboutItem
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.data.models.AuthenticationResponse
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
            println("Error fetching episodes: ${e.message}")
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
            println("Error fetching about items: ${e.message}")
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
            println("Error fetching wines: ${e.message}")
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
            println("Error fetching members: ${e.message}")
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
            _error.value = e.message ?: "Unknown error occurred"
            println("Error fetching events: ${e.message}")
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
            println("Error posting RSVP: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun generatePasskeyRegistrationOptions(email: String): PublicKeyCredentialCreationOptions? {
        _isLoading.value = true
        return try {
            client.post("$SERVER_URL/generatePasskeyRegistrationOptions") {
                contentType(ContentType.Application.Json)
                setBody(RegistrationOptionsRequest(email))
            }.body()
        } catch (e: Exception) {
            _error.value = e.message
            println("Error generating registration options: ${e.message}")
            null
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun verifyPasskeyRegistration(credential: RegistrationResponse, email: String): Boolean {
        _isLoading.value = true
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/verifyPasskeyRegistration") {
                contentType(ContentType.Application.Json)
                setBody(VerifyRegistrationRequest(credential, email))
            }
            response.body<Map<String, Boolean>>()["verified"] ?: false
        } catch (e: Exception) {
            _error.value = e.message
            println("Error verifying registration: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun generatePasskeyAuthenticationOptions(email: String): PublicKeyCredentialRequestOptions? {
        _isLoading.value = true
        return try {
            client.post("$SERVER_URL/generatePasskeyAuthenticationOptions") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }.body()
        } catch (e: Exception) {
            _error.value = e.message
            println("Error generating authentication options: ${e.message}")
            null
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun verifyPasskeyAuthentication(credential: AuthenticationResponse, email: String): Boolean {
        _isLoading.value = true
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/verifyPasskeyAuthentication") {
                contentType(ContentType.Application.Json)
                setBody(VerifyAuthenticationRequest(credential, email))
            }
            response.body<Map<String, Boolean>>()["verified"] ?: false
        } catch (e: Exception) {
            _error.value = e.message
            println("Error verifying authentication: ${e.message}")
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
            println("Received JSON from server: $jsonString")
            Json.decodeFromString<UserProfileData>(jsonString)
        } catch (e: Exception) {
            println("Error fetching profile: ${e.message}")
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
            println("Error updating profile: ${e.message}")
            false
        }
    }

    companion object{
        private const val SERVER_URL =
            "https://us-central1-wdw-app-52a3c.cloudfunctions.net"
//        private const val SERVER_URL =
//            "http://127.0.0.1:5001/wdw-app-52a3c/us-central1"
    }
}