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
import net.winedownwednesday.web.data.network.CloudFunctionUrls.SERVER_URL
import io.ktor.http.isSuccess
import kotlinx.coroutines.await
import net.winedownwednesday.web.FirebaseBridge
import net.winedownwednesday.web.data.AboutItem
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.data.models.AuthenticationResponse
import net.winedownwednesday.web.data.models.BlogPostsResponse
import net.winedownwednesday.web.data.models.ChangePasswordRequest
import net.winedownwednesday.web.data.models.EmailPasswordRequest
import net.winedownwednesday.web.data.models.FcmInstanceRegistrationRequest
import net.winedownwednesday.web.data.models.FeaturedWinesResponse
import net.winedownwednesday.web.data.models.FirebaseAuthResponse
import net.winedownwednesday.web.data.models.InitialDataResponse
import net.winedownwednesday.web.data.models.PublicKeyCredentialCreationOptions
import net.winedownwednesday.web.data.models.PublicKeyCredentialRequestOptions
import net.winedownwednesday.web.data.models.RSVPRequest
import net.winedownwednesday.web.data.models.RegistrationOptionsRequest
import net.winedownwednesday.web.data.models.RegistrationResponse
import net.winedownwednesday.web.data.models.StreamTokenResponse
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.data.models.UserProfileRequest
import net.winedownwednesday.web.data.models.VerifyAuthenticationRequest
import net.winedownwednesday.web.data.models.VerifyRegistrationRequest

@JsFun("(msg) => console.error(msg)")
private external fun consoleError(msg: JsString)

class RemoteDataSource (
    private val client: HttpClient
): ApiService {

    private fun logError(endpoint: String, e: Exception) {
        consoleError(
            "$TAG $endpoint failed: ${e.message}".toJsString()
        )
    }

    companion object {
        private const val TAG = "[RemoteDataSource]"
    }

    override suspend fun fetchInitialData(): InitialDataResponse? {
        return try {
            client.get("$SERVER_URL/getInitialData"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            logError("fetchInitialData", e)
            null
        }
    }

    override suspend fun fetchEpisodes(): List<Episode>? {
        return try {
            client.get("$SERVER_URL/getEpisodes"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            logError("fetchEpisodes", e)
            null
        }
    }

    override suspend fun fetchBlogPosts(): BlogPostsResponse? {
        return try {
            client.get("$SERVER_URL/getBlogPosts"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            logError("fetchBlogPosts", e)
            null
        }
    }

    override suspend fun fetchAboutItems(): List<AboutItem> {
        return try {
            client.get("$SERVER_URL/getAboutItems"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            logError("fetchAboutItems", e)
            listOf()
        }
    }

    override suspend fun fetchWines(): List<Wine>? {
        return try {
            client.get("$SERVER_URL/getWines"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            logError("fetchWines", e)
            null
        }
    }

    override suspend fun fetchFeaturedWines(): FeaturedWinesResponse? {
        return try {
            client.get("$SERVER_URL/getFeaturedWines"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            logError("fetchFeaturedWines", e)
            null
        }
    }

    override suspend fun fetchMembers(): List<Member>? {
        try {
            return client.get("$SERVER_URL/getMembers"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            logError("fetchMembers", e)
        }
        return listOf()
    }

    override suspend fun fetchEvents(): List<Event>? {
        return try {
            client.get("$SERVER_URL/getEvents"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            logError("fetchEvents", e)
            null
        }
    }

    override suspend fun fetchMemberSpotlight(): Member? {
        return try {
            client.get("$SERVER_URL/getMemberSpotlight"){
                headers {
                    append(HttpHeaders.AccessControlAllowOrigin, "*")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body()
        } catch (e: Exception) {
            logError("fetchMemberSpotlight", e)
            null
        }
    }

    override suspend fun postRSVP(rsvp: RSVPRequest): Boolean {
        return try {
            val response: HttpResponse = client.post("$SERVER_URL/postRsvp") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(rsvp)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logError("postRSVP", e)
            false
        }
    }

    override suspend fun generatePasskeyRegistrationOptions(email: String):
            ApiResult<PublicKeyCredentialCreationOptions> {
        return try {
            val response: HttpResponse = client.post(
                CloudFunctionUrls.GENERATE_PASSKEY_REGISTRATION) {
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
            logError("generatePasskeyRegistrationOptions", e)
            ApiResult.Error("Unknown error occurred")
        }
    }

    override suspend fun verifyPasskeyRegistration(
        credential: RegistrationResponse, email: String): Boolean {
        return try {
            val response: HttpResponse = client.post(
                CloudFunctionUrls.VERIFY_PASSKEY_REGISTRATION) {
                contentType(ContentType.Application.Json)
                setBody(VerifyRegistrationRequest(credential, email))
            }
            response.body<Map<String, Boolean>>()["verified"] ?: false
        } catch (e: Exception) {
            logError("verifyPasskeyRegistration", e)
            false
        }
    }

    override suspend fun generatePasskeyAuthenticationOptions(
        email: String
    ): ApiResult<PublicKeyCredentialRequestOptions> {
        return try {
            val response: HttpResponse = client.post(
                CloudFunctionUrls.GENERATE_PASSKEY_AUTH) {
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
            logError("generatePasskeyAuthenticationOptions", e)
            ApiResult.Error("Unknown error occurred")
        }
    }


    override suspend fun verifyPasskeyAuthentication(
        credential: AuthenticationResponse, email: String): Boolean {
        return try {
            val response: HttpResponse = client.post(
                CloudFunctionUrls.VERIFY_PASSKEY_AUTH) {
                contentType(ContentType.Application.Json)
                setBody(VerifyAuthenticationRequest(credential, email))
            }
            response.body<Map<String, Boolean>>()["verified"] ?: false
        } catch (e: Exception) {
            logError("verifyPasskeyAuthentication", e)
            false
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
                try {
                    val profile = JsonInstanceProvider.json.decodeFromString<UserProfileData>(jsonString)
                    profile
                } catch (parseError: Exception) {
                    logError("fetchUserProfile", parseError)
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            logError("fetchUserProfile", e)
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
            logError("updateProfile", e)
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
            logError("addRsvpToEvent", e)
            false
        }
    }

    override suspend fun registerFcmInstanceId(request: FcmInstanceRegistrationRequest): Boolean
    {
        try {
            val response = client.post("$SERVER_URL/registerFcmInstanceId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                return true
            }
        } catch (e: Exception) {
            logError("registerFcmInstanceId", e)
        }
        return false
    }

    override suspend fun unRegisterFcmInstanceId(request: FcmInstanceRegistrationRequest): Boolean
    {
        try {
            val response = client.post("$SERVER_URL/unRegisterFcmInstanceId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                return true
            }
        } catch (e: Exception) {
            logError("unRegisterFcmInstanceId", e)
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
            logError("sendEmailVerification", e)
        }
        return false
    }

    override suspend fun verifyPasskeyRegistrationWithToken(
        credential: RegistrationResponse, email: String): ApiResult<FirebaseAuthResponse> {
        return try {
            val response: HttpResponse = client.post(
                "https://verifypasskeyregistrationwithfirebaseauth-iktff5ztia-uc.a.run.app"
            ) {
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
            logError("verifyPasskeyRegistrationWithToken", e)
            ApiResult.Error("Unknown error during registration verification")
        }
    }


    override suspend fun verifyPasskeyAuthenticationWithToken(
        credential: AuthenticationResponse, email: String): ApiResult<FirebaseAuthResponse> {
        return try {
            val response: HttpResponse = client.post(
                "https://verifypasskeyauthenticationwithfirebaseauth-iktff5ztia-uc.a.run.app"
            ) {
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
            logError("verifyPasskeyAuthenticationWithToken", e)
            ApiResult.Error("Unknown error during authentication verification")
        }
    }

    override suspend fun registerWithEmailPassword(
        request: EmailPasswordRequest
    ): ApiResult<FirebaseAuthResponse> {
        return try {
            val response: HttpResponse =
                client.post("$SERVER_URL/registerWithEmailPassword")
                {
                    contentType(ContentType.Application.Json)
                    setBody(request)
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body())
            } else {
                ApiResult.Error("Registration failed")
            }
        } catch (e: Exception) {
            logError("registerWithEmailPassword", e)
            ApiResult.Error("Registration failed")
        }
    }

    override suspend fun signInWithEmailPassword(
        request: EmailPasswordRequest): ApiResult<FirebaseAuthResponse>
    {
        return try {
            val response: HttpResponse = client
                .post("$SERVER_URL/signInWithEmailPassword") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body())
            } else {
                ApiResult.Error("Sign in failed")
            }
        } catch (e: Exception) {
            logError("signInWithEmailPassword", e)
            ApiResult.Error("Sign in failed")
        }
    }

    override suspend fun linkPasswordToAccount(request: EmailPasswordRequest): Boolean {
        return try {
            val response: HttpResponse = client
                .post("$SERVER_URL/linkPasswordToAccount") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logError("linkPasswordToAccount", e)
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
            logError("changePassword", e)
            false
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            val response: HttpResponse = client
                .post("$SERVER_URL/sendPasswordResetEmail") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("email" to email))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logError("sendPasswordResetEmail", e)
            false
        }
    }

    override suspend fun fetchStreamToken(): StreamTokenResponse? {
        return try {
            val idToken = FirebaseBridge.getIdToken().await<JsAny?>().toString()
            val response: HttpResponse = client
                .post("https://generatestreamtoken-iktff5ztia-uc.a.run.app") {
                    header(HttpHeaders.Authorization, "Bearer $idToken")
                    contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            logError("fetchStreamToken", e)
            null
        }
    }

    // ─── Moderation ─────────────────────────────────────────────────────────

    override suspend fun blockUser(targetEmail: String): Boolean {
        return try {
            val idToken = FirebaseBridge.getIdToken().await<JsAny?>().toString()
            val response: HttpResponse = client.post(
                "https://blockuser-iktff5ztia-uc.a.run.app"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf("targetUserId" to targetEmail))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logError("blockUser", e)
            false
        }
    }

    override suspend fun unblockUser(targetEmail: String): Boolean {
        return try {
            val idToken = FirebaseBridge.getIdToken().await<JsAny?>().toString()
            val response: HttpResponse = client.post(
                "https://unblockuser-iktff5ztia-uc.a.run.app"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf("targetUserId" to targetEmail))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logError("unblockUser", e)
            false
        }
    }

    override suspend fun flagUser(
        targetEmail: String,
        reason: String?,
        category: String
    ): Boolean {
        return try {
            val idToken = FirebaseBridge.getIdToken().await<JsAny?>().toString()
            val response: HttpResponse = client.post(
                "https://flaguser-iktff5ztia-uc.a.run.app"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "targetUserId" to targetEmail,
                    "reason" to (reason ?: ""),
                    "category" to category
                ))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logError("flagUser", e)
            false
        }
    }

    override suspend fun flagMessage(
        messageId: String,
        reason: String?,
        category: String
    ): Boolean {
        return try {
            val idToken = FirebaseBridge.getIdToken().await<JsAny?>().toString()
            val response: HttpResponse = client.post(
                "https://flagmessage-iktff5ztia-uc.a.run.app"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "messageId" to messageId,
                    "reason" to (reason ?: ""),
                    "category" to category
                ))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logError("flagMessage", e)
            false
        }
    }

    override suspend fun getBlockedUsers(): List<String> {
        return try {
            val idToken = FirebaseBridge.getIdToken().await<JsAny?>().toString()
            val response: HttpResponse = client.get(
                "https://getblockedusers-iktff5ztia-uc.a.run.app"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
            }
            if (response.status.isSuccess()) {
                val body = response.body<kotlinx.serialization.json.JsonObject>()
                // Prefer blockedUserIds (Stream IDs) for client-side matching
                val arr = body["blockedUserIds"] as? kotlinx.serialization.json.JsonArray
                    ?: body["blockedEmails"] as? kotlinx.serialization.json.JsonArray
                arr?.mapNotNull {
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logError("getBlockedUsers", e)
            emptyList()
        }
    }

    override suspend fun deleteAccount(
        confirmPhrase: String
    ): Boolean {
        return try {
            val idToken = FirebaseBridge.getIdToken()
                .await<JsAny?>().toString()
            val response: HttpResponse = client.post(
                "https://deleteaccount-iktff5ztia-uc.a.run.app"
            ) {
                header(
                    HttpHeaders.Authorization,
                    "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(
                    kotlinx.serialization.json.buildJsonObject {
                        put("confirmPhrase",
                            kotlinx.serialization.json.JsonPrimitive(
                                confirmPhrase))
                    }.toString()
                )
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logError("deleteAccount", e)
            false
        }
    }

    // ─── Wine Reviews ─────────────────────────────────────────────────────────

    override suspend fun getWineReviews(wineId: Long):
            net.winedownwednesday.web.data.models.WineReviewsResponse? {
        return try {
            val response: HttpResponse = client.get(
                "$SERVER_URL/getWineReviews?wineId=$wineId"
            )
            if (response.status.isSuccess()) {
                response.body()
            } else null
        } catch (e: Exception) {
            logError("getWineReviews", e)
            null
        }
    }

    override suspend fun getMyWineReview(wineId: Long):
            net.winedownwednesday.web.data.models.WineReview? {
        return try {
            val idToken = FirebaseBridge.getIdToken().await<JsAny?>().toString()
            val response: HttpResponse = client.get(
                "$SERVER_URL/getMyWineReview?wineId=$wineId"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
            }
            if (response.status.isSuccess()) {
                val text = response.bodyAsText()
                if (text == "null" || text.isBlank()) null
                else kotlinx.serialization.json.Json.decodeFromString(text)
            } else null
        } catch (e: Exception) {
            logError("getMyWineReview", e)
            null
        }
    }

    override suspend fun submitWineReview(
        request: net.winedownwednesday.web.data.models.SubmitReviewRequest
    ): Boolean {
        return try {
            val idToken = FirebaseBridge.getIdToken().await<JsAny?>().toString()
            val response: HttpResponse = client.post(
                "$SERVER_URL/submitWineReview"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logError("submitWineReview", e)
            false
        }
    }

    override suspend fun deleteMyWineReview(wineId: Long): Boolean {
        return try {
            val idToken = FirebaseBridge.getIdToken().await<JsAny?>().toString()
            val response: HttpResponse = client.post(
                "$SERVER_URL/deleteMyWineReview"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf("wineId" to wineId))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logError("deleteMyWineReview", e)
            false
        }
    }

    override suspend fun flagWineReview(
        request: net.winedownwednesday.web.data.models.FlagReviewRequest
    ): Boolean {
        val idToken = FirebaseBridge.getIdToken().await<JsAny?>().toString()
        val response: HttpResponse = client.post(
            "$SERVER_URL/flagWineReview"
        ) {
            header(HttpHeaders.Authorization, "Bearer $idToken")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            // Try to extract the error message from JSON
            val msg = try {
                kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(body)["error"]
            } catch (e: Exception) {
                logError("flagWineReview/parse", e)
                null
            }
            throw Exception(msg ?: "Failed to flag review (${response.status.value})")
        }
        return true
    }

}

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}