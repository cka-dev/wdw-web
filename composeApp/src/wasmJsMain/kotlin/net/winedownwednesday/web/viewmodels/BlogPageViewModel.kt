package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import net.winedownwednesday.web.AiBridgeExt
import net.winedownwednesday.web.FirebaseBridge
import net.winedownwednesday.web.data.models.BlogPost
import net.winedownwednesday.web.data.repositories.AppRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class BlogPageViewModel(
    private val appRepository: AppRepository,
) : ViewModel() {
    val blogPosts: StateFlow<List<BlogPost>?> = appRepository.blogPosts

    /** Derived: true while blogPosts has not been populated yet. */
    val isLoading: StateFlow<Boolean> = appRepository.blogPosts
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // ─── TL;DR Summarization ───────────────────────────────────────────────────

    /** Map of postId → bullet-point summary string */
    private val _summaries = MutableStateFlow<Map<String, String>>(emptyMap())
    val summaries: StateFlow<Map<String, String>> = _summaries.asStateFlow()

    /** Set of postIds currently being summarized */
    private val _summarizing = MutableStateFlow<Set<String>>(emptySet())
    val summarizing: StateFlow<Set<String>> = _summarizing.asStateFlow()

    fun summarizePost(postId: String, bodyText: String) {
        if (_summarizing.value.contains(postId)) return
        if (_summaries.value.containsKey(postId)) return


        viewModelScope.launch {
            _summarizing.value = _summarizing.value + postId
            try {
                val idToken = FirebaseBridge
                    .getIdToken()
                    .await<JsString?>()?.toString()
                    ?: return@launch

                val bodyJson = buildJsonObject {
                    put("task", "summarize")
                    put("context", buildJsonObject {
                        put("text", bodyText.take(8000)) // cap at 8k chars
                        put("maxBullets", 3)
                    })
                }.toString()

                val url = "https://us-central1-wdw-app-52a3c.cloudfunctions.net/aiInfer"
                val raw = AiBridgeExt
                    .callAuthenticatedApi(url, bodyJson, idToken)
                    .await<JsString>()
                    .toString()

                // Response: { "result": "• Bullet1\n• Bullet2\n• Bullet3" }
                val result = kotlinx.serialization.json.Json
                    .parseToJsonElement(raw)
                    .jsonObject["result"]
                    ?.let {
                        kotlinx.serialization.json.Json.decodeFromJsonElement(
                            kotlinx.serialization.json.JsonPrimitive.serializer(), it
                        ).content
                    } ?: return@launch

                _summaries.value = _summaries.value + (postId to result)
            } catch (_: Exception) {
                // Silent fail — TL;DR is non-critical
            } finally {
                _summarizing.value = _summarizing.value - postId
            }
        }
    }

    fun clearSummary(postId: String) {
        _summaries.value = _summaries.value - postId
    }
}
