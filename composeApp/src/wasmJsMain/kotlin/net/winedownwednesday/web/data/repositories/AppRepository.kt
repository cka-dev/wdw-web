package net.winedownwednesday.web.data.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
import net.winedownwednesday.web.data.models.RegistrationResponse
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.data.network.ApiResult
import net.winedownwednesday.web.data.network.RemoteDataSource
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Single

@Single
class AppRepository (
    @InjectedParam
    private val remoteDataSource: RemoteDataSource
) {

    private val _members = MutableStateFlow<List<Member?>>(listOf())
    val members = _members.asStateFlow()

    private val _events = MutableStateFlow<List<Event>?>(listOf())
    val events = _events.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>?>(listOf())
    val episodes = _episodes.asStateFlow()

    private val _aboutItems = MutableStateFlow<List<AboutItem>?>(listOf())
    val aboutItems = _aboutItems.asStateFlow()

    private val _wineList = MutableStateFlow<List<Wine>?>(listOf())
    val wineList = _wineList.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _profileData = MutableStateFlow<UserProfileData?>(null)
    val profileData: StateFlow<UserProfileData?> = _profileData.asStateFlow()

    init {
        repositoryScope.launch {
            fetchMembers()
            fetchEvents()
            fetchEpisodes()
            fetchAboutItems()
            fetchWines()
        }
    }

    private suspend fun fetchMembers () {
        try {
            val remoteMemberList = remoteDataSource.fetchMembers()

            if (!remoteMemberList.isNullOrEmpty()) {
                _members.value = remoteMemberList
            }
        } catch (e: Exception) {
            println("$TAG: Error fetching members.")
        }
    }

    private suspend fun fetchEvents() {
        try {
            val remoteEventList = remoteDataSource.fetchEvents()
            if (!remoteEventList.isNullOrEmpty()) {
                _events.value = remoteEventList
            }
        } catch (e: Exception) {
            println("$TAG: Error fetching events.")
        }
    }

    private suspend fun fetchEpisodes() {
        try {
            val remoteEpisodeList = remoteDataSource.fetchEpisodes()
            if (!remoteEpisodeList.isNullOrEmpty()) {
                _episodes.value = remoteEpisodeList
            }
        } catch (e: Exception) {
            println("$TAG: Error fetching episodes.")
        }
    }

    private suspend fun fetchAboutItems() {
        try {
            val remoteAboutItemList = remoteDataSource.fetchAboutItems()
            if (remoteAboutItemList.isNotEmpty()) {
                _aboutItems.value = remoteAboutItemList
            }
        } catch (e: Exception) {
            println("$TAG: Error fetching about items.")
        }
    }

    private suspend fun fetchWines() {
        try {
            val remoteWineList = remoteDataSource.fetchWines()
            if (!remoteWineList.isNullOrEmpty()){
                _wineList.value = remoteWineList
            }
        } catch (e: Exception) {
            println("$TAG: Error fetching wine list with message: ${e.message}")
        }
    }

    suspend fun sendRSVP(rsvpRequest: RSVPRequest): Boolean {
        return remoteDataSource.postRSVP(rsvpRequest)
    }

    suspend fun generatePasskeyRegistrationOptions(email: String):
            ApiResult<PublicKeyCredentialCreationOptions> {
        return remoteDataSource.generatePasskeyRegistrationOptions(email)
    }

    suspend fun verifyPasskeyRegistration(credential: RegistrationResponse, email: String):
            Boolean {
        return remoteDataSource.verifyPasskeyRegistration(credential, email)
    }

    suspend fun generatePasskeyAuthenticationOptions(email: String):
            ApiResult<PublicKeyCredentialRequestOptions> {
        return remoteDataSource.generatePasskeyAuthenticationOptions(email)
    }

    suspend fun verifyPasskeyAuthentication(credential:
                                            AuthenticationResponse,email: String): Boolean {
        return remoteDataSource.verifyPasskeyAuthentication(credential, email)
    }

    suspend fun fetchProfileFromServer(userEmail: String):
            UserProfileData? {
        return remoteDataSource.fetchUserProfile(userEmail)
    }

    suspend fun saveProfileToServer(userProfileData: UserProfileData): Boolean {
        return remoteDataSource.updateProfile(userProfileData)
    }

    suspend fun addRsvpToEvent(rsvp: RSVPRequest): Boolean {
        return remoteDataSource.addRsvpToEvent(rsvp)
    }

    suspend fun registerFcmInstanceId(request: FcmInstanceRegistrationRequest): Boolean {
        return remoteDataSource.registerFcmInstanceId(request)
    }

    suspend fun unRegisterFcmInstanceId(request: FcmInstanceRegistrationRequest): Boolean {
        return remoteDataSource.unRegisterFcmInstanceId(request)
    }

    suspend fun sendEmailVerification(email: String): Boolean {
        return remoteDataSource.sendEmailVerification(email)
    }

    suspend fun verifyPasskeyRegistrationWithToken(
        credential: RegistrationResponse,
        email: String
    ): ApiResult<FirebaseAuthResponse> {
        return remoteDataSource.verifyPasskeyRegistrationWithToken(credential, email)
    }

    suspend fun verifyPasskeyAuthenticationWithToken(
        credential: AuthenticationResponse,
        email: String
    ): ApiResult<FirebaseAuthResponse> {
        return remoteDataSource.verifyPasskeyAuthenticationWithToken(credential, email)
    }

    companion object{
        private const val TAG = "AppRepository"
    }
}