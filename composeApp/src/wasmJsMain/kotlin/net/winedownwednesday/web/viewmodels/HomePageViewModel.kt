package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.data.repositories.AppRepository

class HomePageViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _upcomingEvents = MutableStateFlow<List<Event>>(emptyList())
    val upcomingEvents = _upcomingEvents.asStateFlow()

    private val _featuredWines = MutableStateFlow<List<Wine>>(emptyList())
    val featuredWines = _featuredWines.asStateFlow()

    private val _highlightedMember = MutableStateFlow<Member?>(null)
    val highlightedMember = _highlightedMember.asStateFlow()

    private var unfeaturedMembers = mutableListOf<Member>()

    init {
        viewModelScope.launch {
            repository.events.collect { events ->
                if (events != null) {
                    _upcomingEvents.value = events
                }
            }
        }

        viewModelScope.launch {
            repository.wineList.collect { wines ->
                if (wines != null) {
                    _featuredWines.value = wines
                }
            }
        }

        viewModelScope.launch {
            repository.members.collect { members ->
                val nonNullMembers = members.filterNotNull()
                if (nonNullMembers.isNotEmpty()) {
                    unfeaturedMembers = nonNullMembers.toMutableList()
                    pickRandomMember()
                }
            }
        }
    }

    private fun pickRandomMember() {
        if (unfeaturedMembers.isEmpty()) {
            val currentMembers = repository.members.value.filterNotNull()
            unfeaturedMembers = currentMembers.toMutableList()
        }
        if (unfeaturedMembers.isEmpty()) {
            _highlightedMember.value = null
            return
        }
        val randomIndex = (0 until unfeaturedMembers.size).random()
        val chosen = unfeaturedMembers[randomIndex]
        unfeaturedMembers.removeAt(randomIndex)
        _highlightedMember.value = chosen
    }

    fun refreshMemberSpotlight() {
        pickRandomMember()
    }
}
