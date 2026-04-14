package net.winedownwednesday.web.viewmodels

import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.winedownwednesday.web.data.network.JsonInstanceProvider
import net.winedownwednesday.web.AiBridgeExt
import net.winedownwednesday.web.FirebaseBridge
import net.winedownwednesday.web.data.Wine
import net.winedownwednesday.web.data.models.FlagReviewRequest
import net.winedownwednesday.web.data.models.SubmitReviewRequest
import net.winedownwednesday.web.data.models.WineReview
import net.winedownwednesday.web.data.repositories.AppRepository
import net.winedownwednesday.web.data.network.CloudFunctionUrls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

class WinePageViewModel(
    private val repository: AppRepository
) : ViewModel() {

    // ─── Wine list state ──────────────────────────────────────────────────────

    val wineList: StateFlow<List<Wine>?> = repository.wineList

    private val _selectedWine = MutableStateFlow<Wine?>(null)
    val selectedWine: StateFlow<Wine?> = _selectedWine.asStateFlow()

    /** Set by external callers (e.g. Vino card tap) to auto-select a wine by name. */
    private val _pendingWineName = MutableStateFlow<String?>(null)
    val pendingWineName: StateFlow<String?> = _pendingWineName.asStateFlow()

    fun setPendingWineName(name: String) { _pendingWineName.value = name }
    fun clearPendingWineName() { _pendingWineName.value = null }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setSelectedWine(wine: Wine?) {
        _selectedWine.value = wine
        if (wine != null) {
            loadReviewsForWine(wine.id)
            loadMyReview(wine.id)
        } else {
            _reviewsForSelectedWine.value = emptyList()
            _myReviewForSelectedWine.value = null
        }
    }

    fun clearSelectedWine() {
        _selectedWine.value = null
        _reviewsForSelectedWine.value = emptyList()
        _myReviewForSelectedWine.value = null
    }

    // ─── Review state ─────────────────────────────────────────────────────────

    private val _reviewsForSelectedWine = MutableStateFlow<List<WineReview>>(emptyList())
    val reviewsForSelectedWine: StateFlow<List<WineReview>> =
        _reviewsForSelectedWine.asStateFlow()

    private val _myReviewForSelectedWine = MutableStateFlow<WineReview?>(null)
    val myReviewForSelectedWine: StateFlow<WineReview?> =
        _myReviewForSelectedWine.asStateFlow()

    private val _isLoadingReviews = MutableStateFlow(false)
    val isLoadingReviews: StateFlow<Boolean> = _isLoadingReviews.asStateFlow()

    private val _hasFetchedReviews = MutableStateFlow(false)
    val hasFetchedReviews: StateFlow<Boolean> = _hasFetchedReviews.asStateFlow()

    private val _isSubmittingReview = MutableStateFlow(false)
    val isSubmittingReview: StateFlow<Boolean> = _isSubmittingReview.asStateFlow()

    private val _isDeletingReview = MutableStateFlow(false)
    val isDeletingReview: StateFlow<Boolean> = _isDeletingReview.asStateFlow()

    private val _reviewSubmitError = MutableStateFlow<String?>(null)
    val reviewSubmitError: StateFlow<String?> = _reviewSubmitError.asStateFlow()

    private val _reviewSubmitSuccess = MutableStateFlow(false)
    val reviewSubmitSuccess: StateFlow<Boolean> = _reviewSubmitSuccess.asStateFlow()

    // ─── Review actions ───────────────────────────────────────────────────────

    fun loadReviewsForWine(wineId: Long) {
        viewModelScope.launch {
            _isLoadingReviews.value = true
            _hasFetchedReviews.value = false
            try {
                val response = repository.getWineReviews(wineId)
                _reviewsForSelectedWine.value = response?.reviews ?: emptyList()
                _hasFetchedReviews.value = true
            } catch (e: Exception) {
                _reviewsForSelectedWine.value = emptyList()
                // Leave hasFetchedReviews false on error
            } finally {
                _isLoadingReviews.value = false
            }
        }
    }

    fun loadMyReview(wineId: Long) {
        viewModelScope.launch {
            try {
                _myReviewForSelectedWine.value = repository.getMyWineReview(wineId)
            } catch (_: Exception) {}
        }
    }

    fun submitReview(wineId: Long, rating: Int, reviewText: String?, userName: String?) {
        viewModelScope.launch {
            _isSubmittingReview.value = true
            _reviewSubmitError.value = null
            _reviewSubmitSuccess.value = false
            try {
                val request = SubmitReviewRequest(
                    wineId = wineId,
                    rating = rating,
                    reviewText = reviewText?.trim()?.takeIf { it.isNotEmpty() },
                    userName = userName?.trim()?.takeIf { it.isNotEmpty() }
                )
                val ok = repository.submitWineReview(request)
                if (ok) {
                    _reviewSubmitSuccess.value = true
                    loadReviewsForWine(wineId)
                    loadMyReview(wineId)
                    // Refresh the master list so aggregate ratings update locally
                    repository.fetchWines()
                    // Re-assign selectedWine to copy fresh rating state
                    _selectedWine.value = repository.wineList.value?.find { it.id == wineId }
                        ?: _selectedWine.value
                } else {
                    _reviewSubmitError.value = "Failed to submit review. Please try again."
                }
            } catch (e: Exception) {
                _reviewSubmitError.value = e.message ?: "An error occurred."
            } finally {
                _isSubmittingReview.value = false
            }
        }
    }

    fun deleteMyReview(wineId: Long) {
        viewModelScope.launch {
            _isDeletingReview.value = true
            try {
                val ok = repository.deleteMyWineReview(wineId)
                if (ok) {
                    _myReviewForSelectedWine.value = null
                    loadReviewsForWine(wineId)
                    repository.fetchWines()
                    _selectedWine.value = repository.wineList.value?.find { it.id == wineId }
                        ?: _selectedWine.value
                }
            } catch (_: Exception) {}
            finally {
                _isDeletingReview.value = false
            }
        }
    }

    private val _isFlaggingReview = MutableStateFlow(false)
    val isFlaggingReview: StateFlow<Boolean> = _isFlaggingReview.asStateFlow()

    private val _flagSuccess = MutableStateFlow(false)
    val flagSuccess: StateFlow<Boolean> = _flagSuccess.asStateFlow()

    private val _flagError = MutableStateFlow<String?>(null)
    val flagError: StateFlow<String?> = _flagError.asStateFlow()

    fun flagReview(wineId: Long, reviewerEmail: String, reason: String?) {
        viewModelScope.launch {
            _isFlaggingReview.value = true
            _flagError.value = null
            _flagSuccess.value = false
            try {
                repository.flagWineReview(
                    FlagReviewRequest(
                        wineId = wineId,
                        reviewerEmail = reviewerEmail,
                        reason = reason?.trim()?.takeIf { it.isNotEmpty() }
                    )
                )
                _flagSuccess.value = true
            } catch (e: Exception) {
                _flagError.value = e.message ?: "Failed to submit flag."
            } finally {
                _isFlaggingReview.value = false
            }
        }
    }

    fun clearFlagFeedback() {
        _flagSuccess.value = false
        _flagError.value = null
    }

    fun clearReviewFeedback() {
        _reviewSubmitError.value = null
        _reviewSubmitSuccess.value = false
    }

    // ─── Vino Recommendations ─────────────────────────────────────────────────

    private val _vinoRecommendations =
        MutableStateFlow<List<WineRecommendation>>(emptyList())
    val vinoRecommendations: StateFlow<List<WineRecommendation>> =
        _vinoRecommendations.asStateFlow()

    private val _isFetchingRecs = MutableStateFlow(false)
    val isFetchingRecs: StateFlow<Boolean> = _isFetchingRecs.asStateFlow()

    fun fetchVinoRecommendations() {
        if (_isFetchingRecs.value) return
        viewModelScope.launch {
            _isFetchingRecs.value = true
            try {
                val idToken = FirebaseBridge
                    .getIdToken()
                    .await<JsString?>()?.toString()
                    ?: return@launch
                val url = CloudFunctionUrls.RECOMMEND_WINES
                val raw = AiBridgeExt
                    .callAuthenticatedApi(url, "{}", idToken)
                    .await<JsString>()
                    .toString()
                val decoded = JsonInstanceProvider.json
                    .decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(raw)
                val recsJson = decoded["recommendations"]?.toString() ?: "[]"
                _vinoRecommendations.value = JsonInstanceProvider.json
                    .decodeFromString(recsJson)
            } catch (_: Exception) {
                // Silent fail — recommendations are non-critical
            } finally {
                _isFetchingRecs.value = false
            }
        }
    }
}

// ─── Wine Recommendation model ────────────────────────────────────────────────

@Serializable
data class WineRecommendation(
    val name: String = "",
    val reason: String = "",
    val wine_type: String = "",
    val region: String = ""
)

// Extension used by CompactScreenWinePage / LargeScreenWinePage filter
fun Wine.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim().lowercase()
    return name.lowercase().contains(q) ||
        type.lowercase().contains(q) ||
        country.lowercase().contains(q) ||
        region.lowercase().contains(q)
}
