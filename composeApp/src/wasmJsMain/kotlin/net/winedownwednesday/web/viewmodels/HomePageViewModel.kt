package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.data.repositories.AppRepository

class HomePageViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _upcomingEvents = MutableStateFlow<List<Event>>(emptyList())
    val upcomingEvents = _upcomingEvents.asStateFlow()

    private val _eventsLoaded = MutableStateFlow(false)
    val eventsLoaded = _eventsLoaded.asStateFlow()

    private val _featuredWines = MutableStateFlow<List<Wine>>(emptyList())
    val featuredWines = _featuredWines.asStateFlow()

    private val _highlightedMember = MutableStateFlow<Member?>(null)
    val highlightedMember = _highlightedMember.asStateFlow()

    private val _campaignName = MutableStateFlow("Featured Wines")
    val campaignName = _campaignName.asStateFlow()

    private val _campaignDescription = MutableStateFlow("")
    val campaignDescription = _campaignDescription.asStateFlow()

    init {
        viewModelScope.launch {
            repository.events.collect { events ->
                if (events != null) {
                    val today = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date
                    _upcomingEvents.value = events.filter {
                        stringToDate(it.date) >= today
                    }.sortedBy { it.date }
                }
                _eventsLoaded.value = true
            }
        }

        viewModelScope.launch {
            try {
                val featured = repository.fetchFeaturedWines()
                if (featured != null) {
                    _featuredWines.value = featured.wines
                    _campaignName.value = featured.campaignName
                    _campaignDescription.value = featured.description
                }
            } catch (e: Exception) {
                println("$TAG: Error fetching featured wines: ${e.message}")
                _featuredWines.value = emptyList()
                _campaignName.value = "Featured Wines"
            }
        }

        viewModelScope.launch {
            try {
                val member = repository.fetchMemberSpotlight()
                _highlightedMember.value = member
            } catch (e: Exception) {
                println("$TAG: Error fetching member spotlight: ${e.message}")
                _highlightedMember.value = null
            }
        }
    }

    private fun stringToDate(date: String): LocalDate {
        val (year, month, day) = date.split(", ").map { it.toInt() }
        return LocalDate(year, month, day)
    }

    companion object {
        private const val TAG = "HomePageViewModel"
    }
}
