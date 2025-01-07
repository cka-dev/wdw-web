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
import net.winedownwednesday.web.data.models.RSVPRequest
import net.winedownwednesday.web.data.repositories.AppRepository

class EventsPageViewModel(
    private val repository: AppRepository
) : ViewModel() {
    private val _allEvents = MutableStateFlow<List<Event>?>(emptyList())
    val allEvents = _allEvents.asStateFlow()

    private val _upcomingEvents = MutableStateFlow<List<Event>?>(emptyList())
    val upcomingEvents = _upcomingEvents.asStateFlow()

    private val _pastEvents = MutableStateFlow<List<Event>?>(emptyList())
    val pastEvents = _pastEvents.asStateFlow()

    private val _pastEventsByYear = MutableStateFlow<Map<Int, List<Event>>>(emptyMap())
    val pastEventsByYear = _pastEventsByYear.asStateFlow()

    private val _selectedEvent = MutableStateFlow<Event?>(null)
    val selectedEvent = _selectedEvent.asStateFlow()

    private val today: LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    init {
        loadEvents()
    }

    private fun loadEvents() {
        viewModelScope.launch {
            try {
                repository.events.collect { fetchedEvents ->
                    _allEvents.value = fetchedEvents
                    fetchedEvents?.let { updateUpcomingPastEvents(it) }
                }
            } catch (e: Exception) {
                println("$TAG: Error loading events: ${e.message}")
            }
        }
    }

    private fun updateUpcomingPastEvents(eventsToProcess: List<Event>) {
        _upcomingEvents.value = eventsToProcess.filter {
            val eventDate = stringToDate(it.date)
            eventDate > today || eventDate == today
        }.sortedBy { it.date }

        _pastEvents.value = eventsToProcess.filter {
            stringToDate(it.date) < today
        }

        _pastEventsByYear.value = _pastEvents.value?.groupBy {
            stringToDate(it.date).year
        } ?: emptyMap()
    }

    private fun stringToDate(date: String): LocalDate {
        val (year, month, day) = date.split(", ").map { it.toInt() }
        return LocalDate(year, month, day)
    }

    fun setSelectedEvent(event: Event?) {
        _selectedEvent.value = event
    }

    fun clearSelectedEvent() {
        _selectedEvent.value = null
    }

    fun submitRSVP(rsvp: RSVPRequest, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.sendRSVP(rsvp)
            onResult(success)
        }
    }

    fun mimicSubmitRSVP(rsvp: RSVPRequest, onResult: (Boolean) -> Unit){
        onResult(false)
    }

    fun validateAndSubmitRSVP(
        rsvp: RSVPRequest,
        onResult: (Boolean, Map<RSVPField, String>) -> Unit
    ) {
        viewModelScope.launch {
            val validation = RsvpValidator.validate(rsvp)
            if (!validation.isValid) {
                onResult(false, validation.errors)
            } else {
                val success = repository.sendRSVP(rsvp)
                onResult(success, emptyMap())
            }
        }
    }

    fun mimicValidateAndSubmitRSVP(
        rsvp: RSVPRequest,
        onResult: (Boolean, Map<RSVPField, String>) -> Unit
    ) {
        onResult(true, emptyMap())
    }

    companion object {
        private const val TAG = "EventsPageViewModel"
    }
}

object RsvpValidator {

    private val EMAIL_REGEX = Regex(
        "[a-zA-Z0-9+._%\\-]{1,256}@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

    fun validate(rsvp: RSVPRequest): RsvpValidationResult {
        val errors = mutableMapOf<RSVPField, String>()

        if (rsvp.firstName.isBlank()) {
            errors[RSVPField.FIRST_NAME] = "First name is required"
        }
        if (rsvp.lastName.isBlank()) {
            errors[RSVPField.LAST_NAME] = "Last name is required"
        }
        if (rsvp.email.isBlank()) {
            errors[RSVPField.EMAIL] = "Email is required"
        } else if (!rsvp.email.matches(EMAIL_REGEX)) {
            errors[RSVPField.EMAIL] = "Invalid email format"
        }
        if (rsvp.phoneNumber.isBlank()) {
            errors[RSVPField.PHONE] = "Phone number is required"
        }
        if (rsvp.guestsCount < 1 || rsvp.guestsCount > 10) {
            errors[RSVPField.GUESTS] = "Number of guests must be between 1 and 10"
        }

        return if (errors.isEmpty()) {
            RsvpValidationResult(true, emptyMap())
        } else {
            RsvpValidationResult(false, errors)
        }
    }
}

enum class RSVPField {
    FIRST_NAME, LAST_NAME, EMAIL, PHONE, GUESTS
}

data class RsvpValidationResult(
    val isValid: Boolean,
    val errors: Map<RSVPField, String>
)