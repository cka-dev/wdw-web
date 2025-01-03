package net.winedownwednesday.web.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.winedownwednesday.web.data.AboutItem
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.data.models.RSVPRequest
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


    companion object{
        private const val SERVER_URL =
            "https://us-central1-wdw-app-52a3c.cloudfunctions.net"
    }
}