package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import net.winedownwednesday.web.data.network.JsonInstanceProvider
import net.winedownwednesday.web.AiBridgeExt
import net.winedownwednesday.web.FirebaseBridge
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.models.RSVPRequest
import net.winedownwednesday.web.data.repositories.AppRepository
import net.winedownwednesday.web.utils.toEventLocalDate

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

    /** Set by external callers (e.g. Vino card tap) to auto-open a specific event by name. */
    private val _pendingEventName = MutableStateFlow<String?>(null)
    val pendingEventName = _pendingEventName.asStateFlow()

    // ─── UI State (retained across navigation) ──────────────────────────────

    private val _showUpcoming = MutableStateFlow(true)
    val showUpcoming: StateFlow<Boolean> = _showUpcoming.asStateFlow()

    fun setShowUpcoming(show: Boolean) { _showUpcoming.value = show }


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
            } catch (_: Exception) { }
        }
    }

    private fun updateUpcomingPastEvents(eventsToProcess: List<Event>) {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        _upcomingEvents.value = eventsToProcess.filter {
            val eventDate = it.date.toEventLocalDate() ?: return@filter false
            eventDate >= today
        }.sortedBy { it.date.toEventLocalDate() }

        _pastEvents.value = eventsToProcess.filter {
            val eventDate = it.date.toEventLocalDate() ?: return@filter false
            eventDate < today
        }

        _pastEventsByYear.value = _pastEvents.value?.groupBy {
            it.date.toEventLocalDate()?.year ?: 0
        } ?: emptyMap()
    }


    fun setSelectedEvent(event: Event?) {
        _selectedEvent.value = event
    }



    fun setPendingEventName(name: String) {
        _pendingEventName.value = name
    }

    fun clearPendingEventName() {
        _pendingEventName.value = null
    }

    fun addRsvpToEvent(rsvp: RSVPRequest, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.addRsvpToEvent(rsvp)
                onResult(success)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }



    // ─── Vino Event Suggestions ───────────────────────────────────────────────

    private val _vinoEventSuggestions =
        MutableStateFlow<List<EventSuggestion>>(emptyList())
    val vinoEventSuggestions: StateFlow<List<EventSuggestion>> =
        _vinoEventSuggestions.asStateFlow()

    private val _isFetchingEventRecs = MutableStateFlow(false)
    val isFetchingEventRecs: StateFlow<Boolean> =
        _isFetchingEventRecs.asStateFlow()

    fun fetchVinoEventSuggestions() {
        if (_isFetchingEventRecs.value) return
        viewModelScope.launch {
            _isFetchingEventRecs.value = true
            try {
                val idToken = FirebaseBridge
                    .getIdToken()
                    .await<JsString?>()?.toString()
                    ?: return@launch
                val url = "https://us-central1-wdw-app-52a3c.cloudfunctions.net/recommendEvents"
                val raw = AiBridgeExt
                    .callAuthenticatedApi(url, "{}", idToken)
                    .await<JsString>()
                    .toString()
                val decoded = JsonInstanceProvider.json
                    .decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(raw)
                val suggestionsJson = decoded["suggestions"]?.toString() ?: "[]"
                _vinoEventSuggestions.value =
                    JsonInstanceProvider.json.decodeFromString(suggestionsJson)
            } catch (_: Exception) {
                // Silent fail
            } finally {
                _isFetchingEventRecs.value = false
            }
        }
    }
}

@Serializable
data class EventSuggestion(
    val name: String = "",
    val reason: String = ""
)
