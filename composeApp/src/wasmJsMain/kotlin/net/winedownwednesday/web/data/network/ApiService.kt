package net.winedownwednesday.web.data.network

import net.winedownwednesday.web.data.AboutItem
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.data.models.RSVPRequest

interface ApiService {

    suspend fun fetchEpisodes(): List<Episode>?

    suspend fun fetchAboutItems(): List<AboutItem>

    suspend fun fetchWines(): List<Wine>?

    suspend fun fetchMembers(): List<Member>?

    suspend fun fetchEvents(): List<Event>?

    suspend fun postRSVP(rsvp: RSVPRequest): Boolean

//    suspend fun getRegistrationOptions(
//        request: RegistrationOptionsRequest,
//    ): RegistrationOptionsResponse?
//
//    suspend fun verifyRegistration(
//        request: RegistrationVerificationRequest,
//    ): RegistrationVerificationResponse?
//
//    suspend fun getAuthenticationOptions(
//        request: AuthenticationOptionsRequest,
//    ): AuthenticationOptionsResponse?
//
//    suspend fun verifyAuthentication(
//        request: AuthenticationVerificationRequest,
//    ): AuthenticationVerificationResponse?

}