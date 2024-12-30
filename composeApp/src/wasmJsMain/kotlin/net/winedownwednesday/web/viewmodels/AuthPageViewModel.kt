package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.winedownwednesday.web.data.repositories.AppRepository

class AuthPageViewModel(
    private val repository: AppRepository
): ViewModel() {
    private val _showAuthCard = MutableStateFlow<Boolean>(false)
    val showAuthCard = _showAuthCard.asStateFlow()

    fun setShowAuthCardState(value: Boolean){
        _showAuthCard.value = value
    }
}