package net.winedownwednesday.web.data.network

import net.winedownwednesday.web.data.AboutItem
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.data.models.AuthenticationResponse
import net.winedownwednesday.web.data.models.FcmInstanceRegistrationRequest
import net.winedownwednesday.web.data.models.PublicKeyCredentialCreationOptions
import net.winedownwednesday.web.data.models.PublicKeyCredentialRequestOptions
import net.winedownwednesday.web.data.models.RSVPRequest
import net.winedownwednesday.web.data.models.RegistrationResponse
import net.winedownwednesday.web.data.models.UserProfileData

interface ApiService {

    suspend fun fetchEpisodes(): List<Episode>?

    suspend fun fetchAboutItems(): List<AboutItem>

    suspend fun fetchWines(): List<Wine>?

    suspend fun fetchMembers(): List<Member>?

    suspend fun fetchEvents(): List<Event>?

    suspend fun postRSVP(rsvp: RSVPRequest): Boolean

    suspend fun generatePasskeyRegistrationOptions(email: String):
            ApiResult<PublicKeyCredentialCreationOptions>

    suspend fun verifyPasskeyRegistration(
        credential: RegistrationResponse,
        email: String,
    ): Boolean

    suspend fun generatePasskeyAuthenticationOptions(email: String):
            ApiResult<PublicKeyCredentialRequestOptions>

    suspend fun verifyPasskeyAuthentication(
        credential: AuthenticationResponse,
        email: String,
    ): Boolean

    suspend fun fetchUserProfile(userEmail: String): UserProfileData?

    suspend fun updateProfile(profileData: UserProfileData): Boolean

    suspend fun addRsvpToEvent(rsvp: RSVPRequest): Boolean

    suspend fun registerFcmInstanceId(request: FcmInstanceRegistrationRequest): Boolean

    suspend fun unRegisterFcmInstanceId(request: FcmInstanceRegistrationRequest): Boolean
}