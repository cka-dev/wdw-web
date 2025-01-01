package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.data.repositories.AppRepository

class PodcastsPageViewModel(
    private val repository: AppRepository
): ViewModel() {

    private val _episodes = MutableStateFlow<List<Episode>?>(emptyList())
    val episodes = _episodes.asStateFlow()

    private val _selectedEpisode = MutableStateFlow<Episode?>(null)
    val selectedEpisode = _selectedEpisode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        viewModelScope.launch {
            loadEpisodes()
        }
    }

    private suspend fun loadEpisodes() {
        try {
            repository.episodes.collect { fetchedEpisodes ->
                _episodes.value = fetchedEpisodes
            }
        } catch (e: Exception) {
            println("Error loading episodes: ${e.message}")
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedEpisode(episode: Episode){
        _selectedEpisode.value = episode
    }

    fun clearSelectedEpisode() {
        _selectedEpisode.value = null
    }
}

fun Episode.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val lowerQuery = query.lowercase()
    return title.lowercase().contains(lowerQuery)
            || guestName.lowercase().contains(lowerQuery)
            || description.lowercase().contains(lowerQuery)
            || date.lowercase().contains(lowerQuery)
}