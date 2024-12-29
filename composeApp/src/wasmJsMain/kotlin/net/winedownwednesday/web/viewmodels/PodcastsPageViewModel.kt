package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.data.Episode
import net.winedownwednesday.web.data.repositories.AppRepository

class PodcastsPageViewModel(
    private val repository: AppRepository
): ViewModel() {

    private val _episodes = MutableStateFlow<List<Episode>?>(emptyList())
    val episodes = _episodes.asStateFlow()

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
}