package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.winedownwednesday.web.data.network.JsonInstanceProvider
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.winedownwednesday.web.AiBridge
import net.winedownwednesday.web.JsChatChannel
import net.winedownwednesday.web.data.network.CloudFunctionUrls
import net.winedownwednesday.web.JsChatMessage
import net.winedownwednesday.web.JsStreamUser
import net.winedownwednesday.web.StreamBridge
import net.winedownwednesday.web.data.models.StreamTokenResponse
import net.winedownwednesday.web.data.repositories.AppRepository

class MessagingViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _streamToken = MutableStateFlow<StreamTokenResponse?>(null)
    val streamToken = _streamToken.asStateFlow()

    private val _channels = MutableStateFlow<List<JsChatChannel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _searchResults = MutableStateFlow<List<JsStreamUser>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId = _selectedChannelId.asStateFlow()

    private val _messages = MutableStateFlow<List<JsChatMessage>>(emptyList())

    // Blocked user IDs (declared early so `messages` can reference it)
    private val _blockedEmails = MutableStateFlow<List<String>>(emptyList())
    val blockedEmails = _blockedEmails.asStateFlow()

    // Derived: automatically hides messages from blocked users
    val messages = combine(
        _messages, _blockedEmails
    ) { msgs, blocked ->
        if (blocked.isEmpty()) msgs
        else msgs.filter { it.userId !in blocked }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting = _isConnecting.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private var _fcmToken: String? = null

    // Throttle: prevent event-driven refreshes from hammering the API
    private var _lastRefreshTime: Long = 0L
    private var _refreshJob: kotlinx.coroutines.Job? = null
    private var _threadRefreshJob: kotlinx.coroutines.Job? = null
    private var _vinoReplyRefreshJob: kotlinx.coroutines.Job? = null
    private val REFRESH_THROTTLE_MS = 3000L

    // Message cache: avoid redundant network calls when switching channels
    private val messageCache = mutableMapOf<String, List<JsChatMessage>>()

    // Reply state: holds the message being replied to
    private val _replyingTo = MutableStateFlow<JsChatMessage?>(null)
    val replyingTo = _replyingTo.asStateFlow()

    // Edit state: holds the message being edited
    private val _editingMessage = MutableStateFlow<JsChatMessage?>(null)
    val editingMessage = _editingMessage.asStateFlow()

    // Thread state: holds the parent message and its replies
    private val _threadParentMessage = MutableStateFlow<JsChatMessage?>(null)
    val threadParentMessage = _threadParentMessage.asStateFlow()

    private val _threadReplies = MutableStateFlow<List<JsChatMessage>>(emptyList())
    val threadReplies = _threadReplies.asStateFlow()

    // Tracks whether focus is on the main input (false) or the thread input (true)
    private val _threadInputFocused = MutableStateFlow(false)
    val threadInputFocused = _threadInputFocused.asStateFlow()

    fun setThreadInputFocused(focused: Boolean) {
        _threadInputFocused.value = focused
    }

    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers = _typingUsers.asStateFlow()

    private val _channelSearchQuery = MutableStateFlow("")
    val channelSearchQuery = _channelSearchQuery.asStateFlow()

    private val _messageSearchQuery = MutableStateFlow("")
    val messageSearchQuery = _messageSearchQuery.asStateFlow()

    // Members data for profile popover
    val members = appRepository.members

    fun setChannelSearchQuery(query: String) {
        _channelSearchQuery.value = query
    }

    fun setMessageSearchQuery(query: String) {
        _messageSearchQuery.value = query
    }

    fun connectToChat() {
        if (_streamToken.value != null) {
            // Already connected/initialized
            return
        }
        viewModelScope.launch {
            _isConnecting.value = true
            _error.value = null
            try {
                val response = appRepository.fetchStreamToken()
                if (response != null) {
                    _streamToken.value = response
                    
                    // Initialize StreamBridge
                    StreamBridge.connectUser(
                        apiKey = response.apiKey,
                        userToken = response.token,
                        userId = response.userId
                    ).await<JsAny?>()

                    // Ensure user is in the community channel
                    StreamBridge.joinCommunityChannel().await<JsChatChannel?>()

                    _isAdmin.value = StreamBridge.isAdmin()

                    // Load channels
                    loadChannels()

                    // Set up real-time listener
                    StreamBridge.onMessageNew { event ->
                        handleNewMessageEvent(event)
                    }

                    // Auto-register device for push if permission already granted
                    tryRegisterStreamDevice()

                    // Set up foreground notification handler so pushes show
                    // even when the app tab is open
                    net.winedownwednesday.web.FirebaseBridge.setupForegroundNotifications()
                } else {
                    _error.value = "Failed to fetch chat token"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error connecting to chat"
            } finally {
                _isConnecting.value = false
            }
        }
    }

    private fun loadChannels() {
        viewModelScope.launch {
            try {
                val jsChannels = StreamBridge.queryChannels().await<JsArray<JsChatChannel>>()
                val list = mutableListOf<JsChatChannel>()
                for (i in 0 until jsChannels.length) {
                    list.add(jsChannels[i]!!)
                }
                _channels.value = list
            } catch (e: Exception) {
            }
        }
    }

    fun selectChannel(channelId: String) {
        if (channelId.isBlank()) {
            _selectedChannelId.value = null
            _messages.value = emptyList()
            _messageSearchQuery.value = ""
            return
        }
        // Clear AI state from previous channel
        clearSmartReplies()
        clearAllTranslations()
        dismissCatchUp()
        clearRewriteState()
        clearVinoCards()

        _selectedChannelId.value = channelId
        loadMessages(channelId)
    }

    /**
     * Tries to register the FCM device with Stream for push notifications.
     * Only succeeds if browser notification permission is already granted.
     */
    private fun tryRegisterStreamDevice() {
        viewModelScope.launch {
            try {
                val fcmTokenJs = net.winedownwednesday.web.FirebaseBridge.getFcmToken()
                    .await<JsString?>()
                val fcmToken = fcmTokenJs?.toString()
                if (fcmToken != null) {
                    StreamBridge.addDevice(fcmToken, "firebase")
                        .await<JsBoolean>()
                    _fcmToken = fcmToken
                    _notificationsEnabled.value = true
                }
            } catch (_: Exception) {
                // Permission not granted yet or token unavailable — that's OK.
                // User can enable via the bell icon.
            }
        }
    }

    /**
     * Requests notification permission and registers with Stream for push.
     * Called when the user clicks the notification bell to enable.
     */
    fun enableNotifications() {
        viewModelScope.launch {
            try {
                val permission = net.winedownwednesday.web.FirebaseBridge
                    .requestNotificationPermission().await<JsString>().toString()
                if (permission == "granted") {
                    val fcmTokenJs = net.winedownwednesday.web.FirebaseBridge.getFcmToken()
                        .await<JsString?>()
                    val fcmToken = fcmTokenJs?.toString()
                    if (fcmToken != null) {
                        StreamBridge.addDevice(fcmToken, "firebase")
                            .await<JsBoolean>()
                        _fcmToken = fcmToken
                        _notificationsEnabled.value = true
                    }
                } else {
                            //"($permission)")
                }
            } catch (e: Exception) {
            }
        }
    }

    fun disableNotifications() {
        viewModelScope.launch {
            try {
                val token = _fcmToken
                if (token != null) {
                    StreamBridge.removeDevice(token).await<JsBoolean>()
                    _notificationsEnabled.value = false
                    _fcmToken = null
                }
            } catch (e: Exception) {
            }
        }
    }

    fun toggleNotifications() {
        if (_notificationsEnabled.value) {
            disableNotifications()
        } else {
            enableNotifications()
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                val jsUsers = if (query.isBlank()) {
                    StreamBridge.queryAllUsers().await<JsArray<JsStreamUser>>()
                } else {
                    StreamBridge.searchUsers(query).await<JsArray<JsStreamUser>>()
                }
                val list = mutableListOf<JsStreamUser>()
                for (i in 0 until jsUsers.length) {
                    list.add(jsUsers[i]!!)
                }
                _searchResults.value = list
            } catch (e: Exception) {
            }
        }
    }

    fun loadAllUsers() {
        searchUsers("")
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun flagMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val success = appRepository.flagMessage(
                    messageId = messageId,
                    reason = null,
                    category = "OTHER"
                )
                if (success) {
                    showModerationFeedback("Message reported. Thank you.")
                } else {
                    showModerationFeedback("Failed to report message. Please try again.")
                }
            } catch (e: Exception) {
                showModerationFeedback("Error reporting message: ${e.message}")
            }
        }
    }

    /**
     * Opens a DM channel with the Vino bot, creating it if needed.
     */
    fun openVinoDm() {
        startDirectMessage(VINO_BOT_ID)
    }

    /**
     * Returns true if the currently selected channel is a DM with Vino.
     */
    fun isVinoDmChannel(): Boolean {
        val channelId = _selectedChannelId.value ?: return false
        val channel = _channels.value.find { it.id == channelId } ?: return false
        return channel.isDirectMessage && channel.otherUserId == VINO_BOT_ID
    }

    /**
     * Truncates (clears) the Vino DM channel — wipes all messages on Stream
     * and resets local state so the UI immediately shows an empty conversation.
     */
    fun clearVinoChat() {
        val channelId = _selectedChannelId.value ?: return
        viewModelScope.launch {
            try {
                StreamBridge.truncateChannel(channelId).await<JsBoolean>()
                // Clear local state immediately so the UI reflects the change
                _messages.value = emptyList()
                _vinoCardsByMessageId.value = emptyMap()
                _pendingVinoCards = emptyList()
            } catch (e: Exception) {
                // Silently ignore — the channel will refresh on next load
            }
        }
    }

    fun startDirectMessage(userId: String) {
        viewModelScope.launch {
            try {
                val channel = StreamBridge
                    .createDirectChannel(userId).await<JsChatChannel?>()
                if (channel != null) {
                    val currentList = _channels.value.toMutableList()
                    val existing = currentList.find { it.id == channel.id }
                    if (existing == null) {
                        currentList.add(0, channel)
                    } else {
                        currentList.remove(existing)
                        currentList.add(0, existing)
                    }
                    _channels.value = currentList
                    selectChannel(channel.id)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun loadMessages(channelId: String) {
        viewModelScope.launch {
            loadMessagesInternal(channelId)
        }
    }

    /**
     * Suspend version of loadMessages — used internally when the caller
     * needs to await completion before acting on the updated message list.
     */
    private suspend fun loadMessagesInternal(channelId: String) {
        // Serve from cache immediately if available
        messageCache[channelId]?.let { cached ->
            _messages.value = cached
        }

        // Then fetch fresh from network
        try {
            val jsMessages = StreamBridge
                .getMessages(channelId).await<JsArray<JsChatMessage>>()
            val list = mutableListOf<JsChatMessage>()
            for (i in 0 until jsMessages.length) {
                list.add(jsMessages[i]!!)
            }
            _messages.value = list
            messageCache[channelId] = list

            // Mark channel as read since we are viewing it
            try {
                StreamBridge.markRead(channelId).await<JsBoolean>()
            } catch (e: Exception) {
                // Ignore silent read receipt errors
            }
        } catch (e: Exception) {
        }
    }

    private var typingJob: kotlinx.coroutines.Job? = null

    fun sendMessage(text: String, file: org.w3c.files.File? = null) {
        val channelId = _selectedChannelId.value ?: return
        if (text.isBlank() && file == null) return

        stopTyping() // Immediately stop typing when sending
        typingJob?.cancel()

        viewModelScope.launch {
            try {
                val replyTarget = _replyingTo.value
                val newMessage = if (file != null) {
                    val fileType = file.type
                    if (fileType.startsWith("image/")) {
                        StreamBridge.sendImageMessage(channelId, text, file, null).await<JsChatMessage?>()
                    } else {
                        StreamBridge.sendFileMessage(channelId, text, file, null).await<JsChatMessage?>()
                    }
                } else if (replyTarget != null) {
                    _replyingTo.value = null
                    StreamBridge.sendReply(channelId, replyTarget.id, text)
                        .await<JsChatMessage?>()
                } else {
                    StreamBridge.sendMessage(channelId, text).await<JsChatMessage?>()
                }
                if (newMessage != null) {
                    _messages.update { it + newMessage }
                    // Trigger Vino if @mentioned, OR if this is a Vino DM (always respond)
                    if (isVinoMention(text) || isVinoDmChannel()) {
                        // Client-side cooldown: prevent rapid-fire @Vino messages
                        val now = kotlin.time.Clock.System
                            .now().toEpochMilliseconds()
                        if (now - _lastVinoSentAt < 3_000L) {
                            showModerationFeedback(
                                "Give Vino a moment to think... 🍷"
                            )
                            return@launch
                        }
                        _lastVinoSentAt = now
                        val channelCid = _channels.value
                            .find { it.id == channelId }?.cid ?: "messaging:$channelId"
                        // In a direct Vino DM, never thread replies — pass null so the
                        // server uses channel history for context but replies at top level.
                        // For @mentions in regular DMs, keep threading under the message.
                        val parentId = if (isVinoDmChannel()) null else newMessage.id
                        chatWithVino(
                            messageText = text,
                            parentMessageId = parentId,
                            channelCid = channelCid
                        )
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun setReplyingTo(message: JsChatMessage?) {
        _replyingTo.value = message
    }

    fun addReaction(messageId: String, reactionType: String) {
        viewModelScope.launch {
            try {
                StreamBridge.addReaction(messageId, reactionType).await<JsBoolean>()
                // Small delay to let Stream's backend process the reaction
                kotlinx.coroutines.delay(500)
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
            }
        }
    }

    fun removeReaction(messageId: String, reactionType: String) {
        viewModelScope.launch {
            try {
                StreamBridge.removeReaction(messageId, reactionType).await<JsBoolean>()
                kotlinx.coroutines.delay(500)
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                StreamBridge.deleteMessage(messageId).await<JsBoolean>()
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
            }
        }
    }

    fun pinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                StreamBridge.pinMessage(messageId).await<JsBoolean>()
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
            }
        }
    }

    fun unpinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                StreamBridge.unpinMessage(messageId).await<JsBoolean>()
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
            }
        }
    }

    fun setEditingMessage(message: JsChatMessage?) {
        _editingMessage.value = message
        // Clear reply state when entering edit mode
        if (message != null) _replyingTo.value = null
    }

    fun cancelEditing() {
        _editingMessage.value = null
    }

    fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            try {
                StreamBridge.editMessage(messageId, newText).await<JsBoolean>()
                _editingMessage.value = null
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
            }
        }
    }

    fun sendGiphyMessage(gifUrl: String, gifTitle: String, parentId: String? = null) {
        val channelId = _selectedChannelId.value ?: return
        viewModelScope.launch {
            try {
                val newMessage = StreamBridge.sendGiphyMessage(channelId, gifUrl, gifTitle, parentId)
                    .await<JsChatMessage?>()
                if (newMessage != null) {
                    if (parentId != null) {
                        // Reload thread replies
                        val replies = StreamBridge.getThreadReplies(channelId, parentId)
                            .await<JsArray<JsChatMessage>>()
                        _threadReplies.value = (0 until replies.length).map { replies[it]!! }
                        loadMessages(channelId)
                        
                        val wasParentVino = _threadParentMessage.value?.userId == "vino-bot"
                        val wasLastReplyVino = _threadReplies.value.lastOrNull()?.userId == "vino-bot"
                        if (wasParentVino || wasLastReplyVino) {
                            chatWithVino(gifTitle, parentId)
                        }
                    } else {
                        _messages.update { it + newMessage }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun forwardMessage(messageText: String, targetChannelId: String) {
        viewModelScope.launch {
            try {
                val forwardedText = "↪️ Forwarded:\n$messageText"
                StreamBridge.sendMessage(targetChannelId, forwardedText).await<JsChatMessage?>()
            } catch (e: Exception) {
            }
        }
    }

    fun notifyTyping() {
        // Only send typing.start if the timer was not already running
        if (typingJob?.isActive != true) {
            startTyping()
        }
        // Always reset the 3-second stop timer
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            stopTyping()
        }
    }

    private fun startTyping() {
        val channelId = _selectedChannelId.value ?: return
        viewModelScope.launch {
            try {
                StreamBridge.startTyping(channelId).await<JsBoolean>()
            } catch (e: Exception) {
                // Ignore silent typing errors
            }
        }
    }

    fun stopTyping() {
        val channelId = _selectedChannelId.value ?: return
        viewModelScope.launch {
            try {
                StreamBridge.stopTyping(channelId).await<JsBoolean>()
            } catch (e: Exception) {
                // Ignore silent typing errors
            }
        }
    }

    fun openThread(parentMessage: JsChatMessage) {
        _threadParentMessage.value = parentMessage
        val channelId = _selectedChannelId.value ?: return
        viewModelScope.launch {
            try {
                val replies = StreamBridge.getThreadReplies(channelId, parentMessage.id)
                    .await<JsArray<JsChatMessage>>()
                _threadReplies.value = (0 until replies.length).map { replies[it]!! }
            } catch (e: Exception) {
            }
        }
    }

    fun closeThread() {
        _threadParentMessage.value = null
        _threadReplies.value = emptyList()
    }

    fun sendThreadReply(text: String, file: org.w3c.files.File? = null) {
        val channelId = _selectedChannelId.value ?: return
        val parentId = _threadParentMessage.value?.id ?: return
        viewModelScope.launch {
            try {
                val newMessage = if (file != null) {
                    val fileType = file.type
                    if (fileType.startsWith("image/")) {
                        StreamBridge.sendImageMessage(channelId, text, file, parentId).await<JsChatMessage?>()
                    } else {
                        StreamBridge.sendFileMessage(channelId, text, file, parentId).await<JsChatMessage?>()
                    }
                } else {
                    StreamBridge.sendReply(channelId, parentId, text).await<JsChatMessage?>()
                }

                if (newMessage != null) {
                    val wasParentVino = _threadParentMessage.value?.userId == "vino-bot"
                    val wasLastReplyVino = _threadReplies.value.lastOrNull()?.userId == "vino-bot"
                    if (isVinoMention(text) || wasParentVino || wasLastReplyVino) {
                        val channelCid = _channels.value
                            .find { it.id == channelId }?.cid ?: "messaging:$channelId"
                        chatWithVino(text, parentId, channelCid)
                    }
                }

                // Reload thread replies
                val replies = StreamBridge.getThreadReplies(channelId, parentId)
                    .await<JsArray<JsChatMessage>>()
                _threadReplies.value = (0 until replies.length).map { replies[it]!! }
                // Also reload main messages to update reply count
                loadMessages(channelId)
            } catch (e: Exception) {
            }
        }
    }

    private fun handleNewMessageEvent(event: net.winedownwednesday.web.JsChatEvent) {
        val currentUserId = _streamToken.value?.userId
        val type = event.type
        
        when (type) {
            "typing.start" -> {
                if (event.userId != currentUserId && event.channelId == _selectedChannelId.value) {
                    val currentList = _typingUsers.value.toMutableList()
                    if (!currentList.contains(event.userName)) {
                        currentList.add(event.userName)
                        _typingUsers.value = currentList
                    }
                }
            }
            "typing.stop" -> {
                if (event.channelId == _selectedChannelId.value) {
                    val currentList = _typingUsers.value.toMutableList()
                    currentList.remove(event.userName)
                    _typingUsers.value = currentList
                }
            }
            "message.read" -> {
                // Read events only need channel list refresh for unread counts,
                // but throttle to avoid 429 storms
                throttledRefresh(refreshMessages = false)
            }
            else -> {
                // For message/reaction events, reload both channels and messages
                throttledRefresh(refreshMessages = true)
                val isThreadEvent = event.parentId?.isNotEmpty() == true
                // Trigger smart replies for incoming messages from others
                if (type == "message.new" &&
                    event.userId != currentUserId &&
                    event.channelId == _selectedChannelId.value
                ) {
                    // Vino replies bypass the throttle — refresh immediately
                    if (event.userId == VINO_BOT_ID) {
                        _vinoReplyRefreshJob?.cancel()
                        _vinoReplyRefreshJob = viewModelScope.launch {
                            _selectedChannelId.value?.let { chId ->
                                // Await the full message load before flushing cards
                                loadMessagesInternal(chId)
                                if (_pendingVinoCards.isNotEmpty() ||
                                    _pendingVinoAction != null
                                ) {
                                    val vinoMsg = _messages.value
                                        .lastOrNull { it.userId == VINO_BOT_ID }
                                    if (vinoMsg != null) {
                                        if (_pendingVinoCards.isNotEmpty()) {
                                            _vinoCardsByMessageId.value =
                                                _vinoCardsByMessageId.value +
                                                mapOf(vinoMsg.id to _pendingVinoCards)
                                            _pendingVinoCards = emptyList()
                                        }
                                        _pendingVinoAction?.let { action ->
                                            _vinoActionsByMessageId.value =
                                                _vinoActionsByMessageId.value +
                                                mapOf(vinoMsg.id to action)
                                            _pendingVinoAction = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (isThreadEvent && event.parentId == _threadParentMessage.value?.id) {
                        triggerThreadSmartReplies()
                    } else if (!isThreadEvent) {
                        triggerSmartReplies()
                    }
                }
            }
        }
    }

    private fun throttledRefresh(refreshMessages: Boolean) {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val withinThrottle = now - _lastRefreshTime < REFRESH_THROTTLE_MS

        // Always refresh thread replies on a dedicated job — never throttled, never canceled
        if (refreshMessages && _threadParentMessage.value != null) {
            _threadRefreshJob?.cancel()
            _threadRefreshJob = viewModelScope.launch {
                _selectedChannelId.value?.let { channelId ->
                    _threadParentMessage.value?.id?.let { parentId ->
                        try {
                            val replies = StreamBridge.getThreadReplies(channelId, parentId)
                                .await<JsArray<JsChatMessage>>()
                            _threadReplies.value = (0 until replies.length).map { replies[it]!! }
                        } catch (e: Exception) {}
                    }
                }
            }
        }

        // Main channel refresh — throttled
        if (withinThrottle) return
        _lastRefreshTime = now
        _refreshJob?.cancel()
        _refreshJob = viewModelScope.launch {
            loadChannels()
            if (refreshMessages) {
                _selectedChannelId.value?.let { loadMessages(it) }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            StreamBridge.disconnectUser().await<JsAny?>()
            _streamToken.value = null
            _channels.value = emptyList()
            _messages.value = emptyList()
            _selectedChannelId.value = null
            _isAdmin.value = false
            _blockedEmails.value = emptyList()
        }
    }

    // ─── Moderation ─────────────────────────────────────────────────────────

    private val _moderationLoading = MutableStateFlow(false)
    val moderationLoading = _moderationLoading.asStateFlow()

    private val _moderationMessage = MutableStateFlow<String?>(null)
    val moderationMessage = _moderationMessage.asStateFlow()

    fun clearModerationMessage() {
        _moderationMessage.value = null
    }

    private fun showModerationFeedback(msg: String) {
        _moderationMessage.value = msg
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (_moderationMessage.value == msg) {
                _moderationMessage.value = null
            }
        }
    }

    // Report dialog state — stored in ViewModel so it survives layout changes
    data class ReportTarget(val userName: String, val userId: String)

    private val _reportDialogTarget = MutableStateFlow<ReportTarget?>(null)
    val reportDialogTarget = _reportDialogTarget.asStateFlow()

    fun openReportDialog(userName: String, userId: String) {
        _reportDialogTarget.value = ReportTarget(userName, userId)
    }

    fun dismissReportDialog() {
        _reportDialogTarget.value = null
    }

    private val _blockedUserProfiles = MutableStateFlow<List<JsStreamUser>>(emptyList())
    val blockedUserProfiles = _blockedUserProfiles.asStateFlow()

    fun fetchBlockedUserProfiles() {
        viewModelScope.launch {
            val ids = _blockedEmails.value
            if (ids.isEmpty()) {
                _blockedUserProfiles.value = emptyList()
                return@launch
            }
            val csv = ids.joinToString(",")
            val users = StreamBridge.queryUsersByIds(csv).await<JsArray<JsStreamUser>>()
            val result = mutableListOf<JsStreamUser>()
            for (i in 0 until users.length) {
                result.add(users[i]!!)
            }
            _blockedUserProfiles.value = result
        }
    }

    fun fetchBlockedUsers() {
        viewModelScope.launch {
            _blockedEmails.value = appRepository.getBlockedUsers()
        }
    }

    fun blockUser(targetUserId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _moderationLoading.value = true
            val success = appRepository.blockUser(targetUserId)
            if (success) {
                _blockedEmails.update { it + targetUserId }
                kotlinx.coroutines.delay(500)
                _selectedChannelId.value?.let { loadMessages(it) }
                showModerationFeedback("User blocked successfully.")
            } else {
                showModerationFeedback("Failed to block user. Please try again.")
            }
            _moderationLoading.value = false
            onComplete(success)
        }
    }

    fun unblockUser(targetUserId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _moderationLoading.value = true
            val success = appRepository.unblockUser(targetUserId)
            if (success) {
                _blockedEmails.update { it - targetUserId }
                kotlinx.coroutines.delay(500)
                _selectedChannelId.value?.let { loadMessages(it) }
                showModerationFeedback("User unblocked.")
            } else {
                showModerationFeedback("Failed to unblock user. Please try again.")
            }
            _moderationLoading.value = false
            onComplete(success)
        }
    }

    fun flagUser(
        targetUserId: String,
        reason: String?,
        category: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _moderationLoading.value = true
            val success = appRepository.flagUser(targetUserId, reason, category)
            _moderationLoading.value = false
            if (success) {
                showModerationFeedback("Report submitted. Thank you.")
            } else {
                showModerationFeedback("Failed to submit report. Please try again.")
            }
            onComplete(success)
        }
    }

    // ─── Vino AI Bot ────────────────────────────────────────────────────────

    private val _vinoResponding = MutableStateFlow(false)
    val vinoResponding = _vinoResponding.asStateFlow()

    // Cloud Function URLs for Vino bot (v2 functions use Cloud Run URLs)
    // These will be set after deployment; using cloudfunctions.net pattern for now
    companion object {
        const val VINO_BOT_ID = "vino-bot"
    }

    // ─── Vino Cards ──────────────────────────────────────────────────────────

    /**
     * Structured card data returned by the Vino bot alongside its text reply.
     * Cards are rendered inline inside the Vino message bubble in the UI.
     */
    sealed class VinoCard {
        data class EventCard(
            val name: String = "",
            val date: String = "",
            val time: String = "",
            val location: String = "",
            val description: String = ""
        ) : VinoCard()

        data class WineCard(
            val name: String = "",
            val year: String = "",
            val wine_type: String = "",
            val region: String = ""
        ) : VinoCard()
    }

    // Cards keyed by the Vino message ID that generated them.
    // Persists for the whole session so past messages keep their cards.
    private val _vinoCardsByMessageId =
        MutableStateFlow<Map<String, List<VinoCard>>>(emptyMap())
    val vinoCardsByMessageId = _vinoCardsByMessageId.asStateFlow()

    // Pending cards waiting to be keyed to the next arriving Vino message.
    private var _pendingVinoCards: List<VinoCard> = emptyList()

    // The topic domain of Vino's most recent response ("wines", "events",
    // "both", or "general"). Used to seed domain-aware smart replies.
    private val _lastVinoTopic = MutableStateFlow("general")
    val lastVinoTopic = _lastVinoTopic.asStateFlow()

    // Timestamp of the last @Vino message sent (epoch ms).
    // Used for client-side rate-limit cooldown.
    private var _lastVinoSentAt: Long = 0L

    // ─── Vino Actions (RSVP etc.) ──────────────────────────────────────────

    /**
     * A structured action returned by the Vino bot that the client must execute
     * after explicit user confirmation (e.g. RSVP).
     */
    sealed class VinoAction {
        data class RsvpPending(
            val eventId: Long,
            val eventName: String,
            val eventDate: String,
            val guestsCount: Int,
            val isUpdate: Boolean = false
        ) : VinoAction()
    }

    // Actions keyed by Vino message ID — persists for the session.
    private val _vinoActionsByMessageId =
        MutableStateFlow<Map<String, VinoAction>>(emptyMap())
    val vinoActionsByMessageId = _vinoActionsByMessageId.asStateFlow()

    // Pending action holding slot, flushed to the map on message.new.
    private var _pendingVinoAction: VinoAction? = null

    // True while an RSVP submission is in flight (per message ID).
    private val _vinoRsvpLoading =
        MutableStateFlow<Set<String>>(emptySet())
    val vinoRsvpLoading = _vinoRsvpLoading.asStateFlow()

    /** Dismiss (remove) a Vino action card without executing the action. */
    fun dismissVinoRsvp(messageId: String) {
        _vinoActionsByMessageId.value =
            _vinoActionsByMessageId.value - messageId
    }

    /**
     * Submit an RSVP on behalf of the authenticated user.
     * Calls the existing addRsvpToEvent backend endpoint, then dismisses the
     * confirmation card and shows appropriate feedback.
     *
     * @param messageId  The Vino message ID that holds the confirmation card.
     * @param eventId    Firestore event document ID.
     * @param guestsCount Number of additional guests (0 = just the user).
     */
    fun submitVinoRsvp(
        messageId: String,
        eventId: Long,
        guestsCount: Int
    ) {
        viewModelScope.launch {
            _vinoRsvpLoading.update { it + messageId }
            try {
                // Resolve the user's email from Firebase Auth
                val userEmail = net.winedownwednesday.web.FirebaseBridge
                    .getCurrentUser()
                    ?.unsafeCast<net.winedownwednesday.web.FirebaseUser>()
                    ?.email
                if (userEmail.isNullOrBlank()) {
                    showModerationFeedback(
                        "Couldn’t confirm your identity — please sign in again."
                    )
                    return@launch
                }

                val rsvp = net.winedownwednesday.web.data.models.RSVPRequest(
                    eventId = eventId,
                    firstName = "",
                    lastName = "",
                    email = userEmail,
                    phoneNumber = "",
                    allowUpdates = false,
                    guestsCount = guestsCount
                )
                val isUpdate = (_vinoActionsByMessageId.value[messageId]
                    as? VinoAction.RsvpPending)?.isUpdate ?: false
                val success = appRepository.addRsvpToEvent(rsvp)
                if (success) {
                    dismissVinoRsvp(messageId)
                    showModerationFeedback(
                        if (isUpdate) "Guest count updated 🍷"
                        else "You're registered! See you there 🍷"
                    )
                } else {
                    showModerationFeedback(
                        "Couldn't complete your RSVP. Try again or visit Gatherings."
                    )
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                when {
                    msg.contains("GUEST_LIMIT") ->
                        showModerationFeedback(
                            "You’ve attended 3 events as a guest. " +
                            "Accept your membership to continue RSVP’ing."
                        )
                    msg.contains("MEMBERSHIP_PENDING") ->
                        showModerationFeedback(
                            "Your membership invitation is pending. " +
                            "Complete it to RSVP."
                        )
                    else ->
                        showModerationFeedback(
                            "RSVP failed — please try again. 🍷"
                        )
                }
            } finally {
                _vinoRsvpLoading.update { it - messageId }
            }
        }
    }

    fun clearVinoCards() {
        _vinoCardsByMessageId.value = emptyMap()
        _pendingVinoCards = emptyList()
        _vinoActionsByMessageId.value = emptyMap()
        _pendingVinoAction = null
        _lastVinoTopic.value = "general"
    }

    /**
     * Checks if a message is directed at the Vino bot.
     */
    private fun isVinoMention(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("@vino") || lower.contains("@bot")
    }

    /**
     * Sends a message to the Vino bot via Cloud Function.
     * The bot will respond in-channel via Stream Chat.
     */
    fun chatWithVino(
        messageText: String,
        parentMessageId: String? = null,
        channelCid: String? = null
    ) {
        val channelId = _selectedChannelId.value ?: return
        if (messageText.isBlank()) return
        // Use provided cid, or fall back to looking it up, or default to messaging type
        val cid = channelCid
            ?: _channels.value.find { it.id == channelId }?.cid
            ?: "messaging:$channelId"

        viewModelScope.launch {
            _vinoResponding.value = true
            try {
                val idToken = net.winedownwednesday.web.FirebaseBridge
                    .getIdToken().await<JsString?>()?.toString()
                if (idToken != null) {
                    // Snapshot last 10 messages the client already has — sent to server
                    // so context detection never needs a second Stream API call.
                    val recentMsgs = _messages.value.takeLast(10)
                    val historyArray = recentMsgs.map { msg ->
                        val name = msg.userName.replace("\"", "").replace("\n", " ")
                        val text = msg.text.replace("\"", "").replace("\n", " ")
                        "{\"name\":\"$name\",\"text\":\"$text\"}"
                    }
                    val recentHistoryJson = "[${historyArray.joinToString(",")}]"

                    val rawResponse = AiBridge.chatWithBot(
                        channelId = cid,
                        messageText = messageText,
                        parentMessageId = parentMessageId,
                        idToken = idToken,
                        functionUrl = CloudFunctionUrls.CHAT_WITH_BOT,
                        recentHistory = recentHistoryJson
                    ).await<JsString>().toString()

                    // Parse response and extract Vino Cards + detectedTopic
                    try {
                        val jsonEl = JsonInstanceProvider.json.parseToJsonElement(rawResponse)
                        val obj = jsonEl.jsonObject

                        // Check for server-side rate limit or quota errors
                        val isRateLimited = obj["rateLimited"]
                            ?.jsonPrimitive?.content == "true"
                        if (isRateLimited) {
                            val retryAfter = obj["retryAfter"]
                                ?.jsonPrimitive?.content?.toIntOrNull() ?: 60
                            showModerationFeedback(
                                "Vino needs a breather 🍷 Try again in " +
                                "${retryAfter}s."
                            )
                            return@launch
                        }

                        // Store detected topic for domain-aware smart replies
                        val topic = obj["detectedTopic"]
                            ?.jsonPrimitive?.content ?: "general"
                        _lastVinoTopic.value = topic

                        val cardsArray = obj["cards"]?.jsonArray
                        if (cardsArray != null && cardsArray.isNotEmpty()) {
                            val cards = cardsArray.mapNotNull { el ->
                                val cardObj = el.jsonObject
                                val str = { key: String ->
                                    cardObj[key]?.jsonPrimitive?.content ?: ""
                                }
                                when (cardObj["type"]?.jsonPrimitive?.content) {
                                    "event" -> VinoCard.EventCard(
                                        name = str("name"),
                                        date = str("date"),
                                        time = str("time"),
                                        location = str("location"),
                                        description = str("description")
                                    )
                                    "wine" -> VinoCard.WineCard(
                                        name = str("name"),
                                        year = str("year"),
                                        wine_type = str("wine_type"),
                                        region = str("region")
                                    )
                                    else -> null
                                }
                            }
                            if (cards.isNotEmpty()) {
                                // Hold cards in pending slot — keyed to the
                                // Vino message ID once message.new fires.
                                _pendingVinoCards = cards
                            }
                        }

                        // Parse optional RSVP action
                        val actionObj = obj["action"]?.jsonObject
                        if (actionObj != null &&
                            actionObj["type"]?.jsonPrimitive?.content == "rsvp_pending"
                        ) {
                            val str = { key: String ->
                                actionObj[key]?.jsonPrimitive?.content ?: ""
                            }
                            val eventIdStr = str("eventId")
                            val eid = eventIdStr.toLongOrNull()
                            if (eid != null) {
                                _pendingVinoAction = VinoAction.RsvpPending(
                                    eventId = eid,
                                    eventName = str("eventName"),
                                    eventDate = str("eventDate"),
                                    guestsCount = str("guestsCount")
                                        .toIntOrNull() ?: 0,
                                    isUpdate = actionObj["isUpdate"]
                                        ?.jsonPrimitive?.content == "true"
                                )
                            }
                        }
                    } catch (_: Exception) {
                        // Parsing failed — cards remain empty, text still posts via Stream
                    }
                    // Bot text response comes through Stream’s real-time event system,
                    // which triggers handleNewMessageEvent → loadMessages
                }
            } catch (e: Exception) {
                // Handle Gemini quota / 503 errors specifically
                val msg = e.message ?: ""
                if (msg.contains("503") || msg.contains("quota") ||
                    msg.contains("breather")
                ) {
                    showModerationFeedback(
                        "I'm getting a lot of questions right now — " +
                        "give me a moment! 🍷"
                    )
                } else {
                    showModerationFeedback(
                        "Vino is having trouble right now. Try again! 🍷"
                    )
                }
            } finally {
                _vinoResponding.value = false
            }
        }
    }

    /**
     * Checks if on-device AI is available (Chrome Prompt API).
     */
    fun isOnDeviceAiAvailable(): Boolean {
        return try {
            AiBridge.isOnDeviceAvailable()
        } catch (e: Exception) {
            false
        }
    }

    // ─── Phase 2: Smart Replies ─────────────────────────────────────────────

    private val _smartReplies = MutableStateFlow<List<String>>(emptyList())
    val smartReplies = _smartReplies.asStateFlow()

    private var smartReplyJob: Job? = null

    /**
     * Generates 3 smart reply suggestions based on recent messages.
     * Debounced: called 500ms after the last incoming message.
     */
    fun triggerSmartReplies() {
        smartReplyJob?.cancel()
        smartReplyJob = viewModelScope.launch {
            delay(500)
            try {
                val msgs = _messages.value.takeLast(5)
                if (msgs.isEmpty()) return@launch

                val contextArray = msgs.map { msg ->
                    "{\"name\":\"${msg.userName.replace("\"", "")}\"," +
                        "\"text\":\"${msg.text.replace("\"", "").replace("\n", " ")}\"}"
                }
                // Inject last Vino topic so smart replies stay domain-relevant
                val vinoTopic = _lastVinoTopic.value
                val topicHint = if (vinoTopic != "general") {
                    ",\"vinoContext\":\"Vino just answered about $vinoTopic. " +
                    "Generate replies related to $vinoTopic.\""
                } else ""
                val contextJson =
                    "{\"messages\":[${contextArray.joinToString(",")}]$topicHint}"

                val idToken = net.winedownwednesday.web.FirebaseBridge
                    .getIdToken().await<JsString?>()?.toString() ?: return@launch

                val result = AiBridge.generateSmartReplies(
                    contextJson = contextJson,
                    idToken = idToken,
                    functionUrl = CloudFunctionUrls.AI_INFER
                ).await<JsString>().toString()

                // Parse JSON array: ["reply1", "reply2", "reply3"]
                val cleaned = result.trim()
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```").trim()
                val parsed = JsonInstanceProvider.json.decodeFromString<List<String>>(cleaned)
                _smartReplies.value = parsed.take(3)
            } catch (e: Exception) {
                _smartReplies.value = emptyList()
            }
        }
    }

    fun clearSmartReplies() {
        smartReplyJob?.cancel()
        _smartReplies.value = emptyList()
    }

    fun sendSmartReply(reply: String) {
        clearSmartReplies()
        sendMessage(reply)
    }

    // ─── Thread Smart Replies ────────────────────────────────────────────────

    private val _threadSmartReplies = MutableStateFlow<List<String>>(emptyList())
    val threadSmartReplies = _threadSmartReplies.asStateFlow()

    private var threadSmartReplyJob: Job? = null

    fun triggerThreadSmartReplies() {
        threadSmartReplyJob?.cancel()
        threadSmartReplyJob = viewModelScope.launch {
            delay(500)
            try {
                val msgs = _threadReplies.value.takeLast(5).ifEmpty {
                    listOf(_threadParentMessage.value ?: return@launch)
                }

                val contextArray = msgs.map { msg ->
                    "{\"name\":\"${msg.userName.replace("\"", "")}\"," +
                        "\"text\":\"${msg.text.replace("\"", "").replace("\n", " ")}\"}"
                }
                val contextJson = "{\"messages\":[${contextArray.joinToString(",")}]}"

                val idToken = net.winedownwednesday.web.FirebaseBridge
                    .getIdToken().await<JsString?>()?.toString() ?: return@launch

                val result = AiBridge.generateSmartReplies(
                    contextJson = contextJson,
                    idToken = idToken,
                    functionUrl = CloudFunctionUrls.AI_INFER
                ).await<JsString>().toString()

                val cleaned = result.trim()
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```").trim()
                val parsed = JsonInstanceProvider.json.decodeFromString<List<String>>(cleaned)
                _threadSmartReplies.value = parsed.take(3)
            } catch (e: Exception) {
                _threadSmartReplies.value = emptyList()
            }
        }
    }

    fun clearThreadSmartReplies() {
        threadSmartReplyJob?.cancel()
        _threadSmartReplies.value = emptyList()
    }

    fun sendThreadSmartReply(reply: String) {
        clearThreadSmartReplies()
        sendThreadReply(reply)
    }

    // ─── Phase 3: Message Drafting ──────────────────────────────────────────

    sealed class AiRewriteState {
        data object Idle : AiRewriteState()
        data object Loading : AiRewriteState()
        data class Done(val original: String, val rewritten: String) : AiRewriteState()
    }

    private val _aiRewriteState = MutableStateFlow<AiRewriteState>(AiRewriteState.Idle)
    val aiRewriteState = _aiRewriteState.asStateFlow()

    /**
     * Rewrites the current message draft using AI.
     * @param text Current input text
     * @param instruction One of: improve, casual, formal, expand, shorten, wine_flair
     * @return The rewritten text via callback
     */
    fun rewriteMessage(
        text: String,
        instruction: String,
        onResult: (String) -> Unit
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _aiRewriteState.value = AiRewriteState.Loading
            try {
                val idToken = net.winedownwednesday.web.FirebaseBridge
                    .getIdToken().await<JsString?>()?.toString() ?: return@launch

                val result = AiBridge.rewriteMessage(
                    text = text,
                    instruction = instruction,
                    idToken = idToken,
                    functionUrl = CloudFunctionUrls.AI_INFER
                ).await<JsString>().toString()

                _aiRewriteState.value = AiRewriteState.Done(
                    original = text,
                    rewritten = result
                )
                onResult(result)
            } catch (e: Exception) {
                _aiRewriteState.value = AiRewriteState.Idle
                showModerationFeedback("AI rewrite failed. Try again! ✨")
            }
        }
    }

    fun undoRewrite(): String? {
        val state = _aiRewriteState.value
        _aiRewriteState.value = AiRewriteState.Idle
        return if (state is AiRewriteState.Done) state.original else null
    }

    fun clearRewriteState() {
        _aiRewriteState.value = AiRewriteState.Idle
    }

    // ─── Phase 4: "What Did I Miss?" Catch-Up ───────────────────────────────

    @Serializable
    data class CatchUpSummary(
        val key_topics: List<String> = emptyList(),
        val decisions: List<String> = emptyList(),
        val mentions_of_you: List<String> = emptyList(),
        val action_items: List<String> = emptyList(),
        val vibe: String = ""
    )

    sealed class CatchUpState {
        data object Hidden : CatchUpState()
        data class Available(val count: Int) : CatchUpState()
        data object Loading : CatchUpState()
        data class Ready(val summary: CatchUpSummary) : CatchUpState()
        data class Error(val message: String) : CatchUpState()
    }

    private val _catchUpState = MutableStateFlow<CatchUpState>(CatchUpState.Hidden)
    val catchUpState = _catchUpState.asStateFlow()

    fun setCatchUpAvailable(unreadCount: Int) {
        if (unreadCount >= 20 && _catchUpState.value is CatchUpState.Hidden) {
            _catchUpState.value = CatchUpState.Available(unreadCount)
        }
    }

    fun summarizeThread() {
        val channelId = _selectedChannelId.value ?: return
        viewModelScope.launch {
            _catchUpState.value = CatchUpState.Loading
            try {
                val idToken = net.winedownwednesday.web.FirebaseBridge
                    .getIdToken().await<JsString?>()?.toString() ?: return@launch

                val result = AiBridge.summarizeThread(
                    channelId = channelId,
                    since = null,
                    idToken = idToken,
                    functionUrl = CloudFunctionUrls.SUMMARIZE_THREAD
                ).await<JsString>().toString()

                val parsed = JsonInstanceProvider.json.decodeFromString<CatchUpSummary>(result)
                _catchUpState.value = CatchUpState.Ready(parsed)
            } catch (e: Exception) {
                _catchUpState.value = CatchUpState.Error(
                    "Couldn't summarize. Try again! ✨"
                )
            }
        }
    }

    fun dismissCatchUp() {
        _catchUpState.value = CatchUpState.Hidden
    }

    // ─── Phase 5: Translation ───────────────────────────────────────────────

    private val _translations = MutableStateFlow<Map<String, TranslationResult>>(emptyMap())
    val translations = _translations.asStateFlow()

    data class TranslationResult(
        val translatedText: String,
        val sourceLanguage: String
    )

    fun translateMessage(messageId: String, text: String) {
        viewModelScope.launch {
            // Mark as loading with placeholder
            val current = _translations.value.toMutableMap()
            current[messageId] = TranslationResult("Translating...", "")
            _translations.value = current

            try {
                val idToken = net.winedownwednesday.web.FirebaseBridge
                    .getIdToken().await<JsString?>()?.toString() ?: return@launch

                // Detect language
                val sourceLang = AiBridge.detectLanguage(text)
                    .await<JsString>().toString()

                // Translate to English
                val translated = AiBridge.translateText(
                    text = text,
                    sourceLang = sourceLang,
                    targetLang = "en",
                    idToken = idToken,
                    functionUrl = CloudFunctionUrls.AI_INFER
                ).await<JsString>().toString()

                val langName = when (sourceLang) {
                    "es" -> "Spanish"
                    "fr" -> "French"
                    "pt" -> "Portuguese"
                    "it" -> "Italian"
                    "de" -> "German"
                    "ja" -> "Japanese"
                    "ko" -> "Korean"
                    "zh" -> "Chinese"
                    else -> sourceLang.uppercase()
                }

                val updated = _translations.value.toMutableMap()
                updated[messageId] = TranslationResult(translated, langName)
                _translations.value = updated
            } catch (e: Exception) {
                val updated = _translations.value.toMutableMap()
                updated.remove(messageId)
                _translations.value = updated
                showModerationFeedback("Translation failed. Try again! 🌐")
            }
        }
    }

    fun clearTranslation(messageId: String) {
        val updated = _translations.value.toMutableMap()
        updated.remove(messageId)
        _translations.value = updated
    }

    fun clearAllTranslations() {
        _translations.value = emptyMap()
    }
}

