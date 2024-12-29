package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.data.AboutItem
import net.winedownwednesday.web.data.models.AboutSection
import net.winedownwednesday.web.data.repositories.AppRepository
import wdw_web.composeapp.generated.resources.Res
import wdw_web.composeapp.generated.resources.wdw_web_community_engagement
import wdw_web.composeapp.generated.resources.wdw_web_contact_us
import wdw_web.composeapp.generated.resources.wdw_web_events
import wdw_web.composeapp.generated.resources.wdw_web_history
import wdw_web.composeapp.generated.resources.wdw_web_membership
import wdw_web.composeapp.generated.resources.wdw_web_vision

class AboutPageViewModel(
    private val repository: AppRepository
): ViewModel() {
    private val aboutImageMap = mapOf(
        "Vision" to Res.drawable.wdw_web_vision,
        "History" to Res.drawable.wdw_web_history,
        "Membership" to Res.drawable.wdw_web_membership,
        "Events" to Res.drawable.wdw_web_events,
        "Community Engagement" to Res.drawable.wdw_web_community_engagement,
        "Contact Us" to Res.drawable.wdw_web_contact_us
    )

    private val _aboutSections = MutableStateFlow<List<AboutSection>>(emptyList())
    val aboutSections = _aboutSections.asStateFlow()

    init {
        viewModelScope.launch {
            mapAboutItemsToAboutSections()
        }
    }

    private suspend fun mapAboutItemsToAboutSections() {
        try {
            repository.aboutItems.collect { fetchedAboutItems ->
                val mappedSections = mapAboutItemsToSections(fetchedAboutItems)
                _aboutSections.value = mappedSections
            }
        } catch (e: Exception) {
            println("Error loading about items: ${e.message}")
        }
    }

    private fun mapAboutItemsToSections(items: List<AboutItem>?): List<AboutSection> {
        if (items.isNullOrEmpty()) return emptyList()

        return items.mapIndexed { index, item ->
            val localImage = aboutImageMap[item.title]
            val imageOnLeft = (index % 2 == 0)
            AboutSection(
                title = item.title,
                body = item.content,
                imageRes = localImage,
                imageOnLeft = imageOnLeft
            )
        }
    }
}