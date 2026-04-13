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
import net.winedownwednesday.web.data.models.BlogPost
import net.winedownwednesday.web.data.models.ChangePasswordRequest
import net.winedownwednesday.web.data.models.EmailPasswordRequest
import net.winedownwednesday.web.data.models.FcmInstanceRegistrationRequest
import net.winedownwednesday.web.data.models.FeaturedWinesResponse
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

    private val _events = MutableStateFlow<List<Event>?>(null)
    val events = _events.asStateFlow()

    private val _blogPosts = MutableStateFlow<List<BlogPost>?>(listOf())
    val blogPosts = _blogPosts.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>?>(listOf())
    val episodes = _episodes.asStateFlow()

    private val _aboutItems = MutableStateFlow<List<AboutItem>?>(listOf())
    val aboutItems = _aboutItems.asStateFlow()

    private val _wineList = MutableStateFlow<List<Wine>?>(listOf())
    val wineList = _wineList.asStateFlow()
    
    private val _featuredWinesResponse = MutableStateFlow<FeaturedWinesResponse?>(null)
    val featuredWinesResponse = _featuredWinesResponse.asStateFlow()

    private val _memberSpotlight = MutableStateFlow<Member?>(null)
    val memberSpotlight = _memberSpotlight.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _profileData = MutableStateFlow<UserProfileData?>(null)
    val profileData: StateFlow<UserProfileData?> = _profileData.asStateFlow()

    init {
        repositoryScope.launch {
            // Primary: single batch call replaces 8 individual requests
            val initialData = remoteDataSource.fetchInitialData()
            if (initialData != null) {
                _members.value = initialData.members
                _events.value = initialData.events
                _episodes.value = initialData.episodes
                _wineList.value = initialData.wines
                _aboutItems.value = initialData.aboutItems
                _memberSpotlight.value = initialData.memberSpotlight
                _featuredWinesResponse.value = initialData.featuredWines
                if (initialData.blogPosts != null &&
                    initialData.blogPosts.posts.isNotEmpty()
                ) {
                    _blogPosts.value = initialData.blogPosts.posts
                }
            } else {
                // Fallback: individual calls if batch fails
                fetchMembers()
                fetchEvents()
                fetchEpisodes()
                fetchBlogPosts()
                fetchAboutItems()
                fetchWines()
                fetchFeaturedWinesResponse()
                fetchMemberSpotlight()
            }
        }
    }

    private suspend fun fetchMembers () {
        try {
            val remoteMemberList = remoteDataSource.fetchMembers()

            if (!remoteMemberList.isNullOrEmpty()) {
                _members.value = remoteMemberList
            }
        } catch (e: Exception) {
            // println("$TAG: Error fetching members.")
        }
    }

    private suspend fun fetchEvents() {
        try {
            val remoteEventList = remoteDataSource.fetchEvents()
            if (remoteEventList != null) {
                _events.value = remoteEventList
            } else {
                _events.value = emptyList()
            }
        } catch (e: Exception) {
            // println("$TAG: Error fetching events.")
            _events.value = emptyList()
        }
    }

    private suspend fun fetchBlogPosts() {
        try {
            val remoteBlogPostsResponse = remoteDataSource.fetchBlogPosts()
            if (remoteBlogPostsResponse != null && remoteBlogPostsResponse.posts.isNotEmpty()) {
                _blogPosts.value = remoteBlogPostsResponse.posts
            }
        } catch (e: Exception) {
            // println("$TAG: Error fetching blog posts: ${e.message}")
        }
    }

    private suspend fun fetchEpisodes() {
        try {
            val remoteEpisodeList = remoteDataSource.fetchEpisodes()
            if (!remoteEpisodeList.isNullOrEmpty()) {
                _episodes.value = remoteEpisodeList
            }
        } catch (e: Exception) {
            // println("$TAG: Error fetching episodes.")
        }
    }

    private suspend fun fetchAboutItems() {
        try {
            val remoteAboutItemList = remoteDataSource.fetchAboutItems()
            if (remoteAboutItemList.isNotEmpty()) {
                _aboutItems.value = remoteAboutItemList
            }
        } catch (e: Exception) {
            // println("$TAG: Error fetching about items.")
        }
    }

    suspend fun fetchWines() {
        try {
            val remoteWineList = remoteDataSource.fetchWines()
            if (!remoteWineList.isNullOrEmpty()){
                _wineList.value = remoteWineList
            }
        } catch (e: Exception) {
            // println("$TAG: Error fetching wine list with message: ${e.message}")
        }
    }

    private suspend fun fetchMemberSpotlight() {
        try {
            val member = remoteDataSource.fetchMemberSpotlight()
            _memberSpotlight.value = member
        } catch (e: Exception) {
            // println("$TAG: Error fetching member spotlight.")
        }
    }

    private suspend fun fetchFeaturedWinesResponse() {
        try {
            val featuredWines = remoteDataSource.fetchFeaturedWines()
            _featuredWinesResponse.value = featuredWines
        } catch (e: Exception) {
            // println("$TAG: Error fetching featured wines.")
        }
    }

    suspend fun sendRSVP(rsvp: RSVPRequest): Boolean {
        return remoteDataSource.postRSVP(rsvp)
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

    suspend fun registerWithEmailPassword(
        request: EmailPasswordRequest
    ): ApiResult<FirebaseAuthResponse> {
        return remoteDataSource.registerWithEmailPassword(request)
    }

    suspend fun signInWithEmailPassword(
        request: EmailPasswordRequest
    ): ApiResult<FirebaseAuthResponse> {
        return remoteDataSource.signInWithEmailPassword(request)
    }

    suspend fun linkPasswordToAccount(request: EmailPasswordRequest): Boolean {
        return remoteDataSource.linkPasswordToAccount(request)
    }

    suspend fun changePassword(request: ChangePasswordRequest): Boolean {
        return remoteDataSource.changePassword(request)
    }

    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return remoteDataSource.sendPasswordResetEmail(email)
    }

    suspend fun fetchStreamToken(): net.winedownwednesday.web.data.models.StreamTokenResponse? {
        return remoteDataSource.fetchStreamToken()
    }

    // ─── Moderation ─────────────────────────────────────────────────────────

    suspend fun blockUser(targetEmail: String): Boolean {
        return remoteDataSource.blockUser(targetEmail)
    }

    suspend fun unblockUser(targetEmail: String): Boolean {
        return remoteDataSource.unblockUser(targetEmail)
    }

    suspend fun flagUser(targetEmail: String, reason: String?, category: String): Boolean {
        return remoteDataSource.flagUser(targetEmail, reason, category)
    }

    suspend fun flagMessage(messageId: String, reason: String?, category: String): Boolean {
        return remoteDataSource.flagMessage(messageId, reason, category)
    }

    suspend fun getBlockedUsers(): List<String> {
        return remoteDataSource.getBlockedUsers()
    }

    suspend fun deleteAccount(confirmPhrase: String): Boolean {
        return remoteDataSource.deleteAccount(confirmPhrase)
    }

    // ─── Wine Reviews ─────────────────────────────────────────────────────────

    suspend fun getWineReviews(wineId: Long):
            net.winedownwednesday.web.data.models.WineReviewsResponse? {
        return remoteDataSource.getWineReviews(wineId)
    }

    suspend fun getMyWineReview(wineId: Long):
            net.winedownwednesday.web.data.models.WineReview? {
        return remoteDataSource.getMyWineReview(wineId)
    }

    suspend fun submitWineReview(
        request: net.winedownwednesday.web.data.models.SubmitReviewRequest
    ): Boolean {
        return remoteDataSource.submitWineReview(request)
    }

    suspend fun deleteMyWineReview(wineId: Long): Boolean {
        return remoteDataSource.deleteMyWineReview(wineId)
    }

    suspend fun flagWineReview(
        request: net.winedownwednesday.web.data.models.FlagReviewRequest
    ): Boolean {
        return remoteDataSource.flagWineReview(request)
    }

    companion object{
        private const val TAG = "AppRepository"
    }
}