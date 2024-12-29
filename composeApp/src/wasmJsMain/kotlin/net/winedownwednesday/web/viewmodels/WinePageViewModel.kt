package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.data.repositories.AppRepository

class WinePageViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _wineList = MutableStateFlow<List<Wine>>(emptyList())
    val wineList: StateFlow<List<Wine>> = _wineList

    private val _selectedWine = MutableStateFlow<Wine?>(null)
    val selectedWine: StateFlow<Wine?> = _selectedWine

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        viewModelScope.launch {
            repository.wineList.collect { serverWines ->
                _wineList.value = serverWines ?: emptyList()
            }
        }
    }

    fun setSelectedWine(wine: Wine) {
        _selectedWine.value = wine
    }

    fun clearSelectedWine() {
        _selectedWine.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

fun Wine.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val lowerQuery = query.lowercase()
    return name.lowercase().contains(lowerQuery)
            || type.lowercase().contains(lowerQuery)
            || country.lowercase().contains(lowerQuery)
            || technicalDetails.lowercase().contains(lowerQuery)
            || (whyWeLovedIt?.lowercase()?.contains(lowerQuery) == true)
}
