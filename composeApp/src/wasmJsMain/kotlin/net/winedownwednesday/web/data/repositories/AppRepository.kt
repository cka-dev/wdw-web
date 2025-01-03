package net.winedownwednesday.web.data.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.data.AboutItem
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
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
            } else {
                println("$TAG: Returned member list is empty.")
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
            } else {
                println("$TAG: Returned event list is empty.")
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
            } else {
                println("$TAG: Returned episode list is empty.")
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
            } else {
                println("$TAG: Returned about item list is empty.")
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
            } else {
                println("$TAG: Returned wine list is empty.")
            }
        } catch (e: Exception) {
            println("$TAG: Error fetching wine list with message: ${e.message}")
        }
    }

    companion object{
        private const val TAG = "AppRepository"
    }
}