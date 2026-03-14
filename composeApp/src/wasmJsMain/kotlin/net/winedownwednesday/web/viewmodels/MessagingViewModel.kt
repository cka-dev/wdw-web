package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.JsChatChannel
import net.winedownwednesday.web.JsChatMessage
import net.winedownwednesday.web.JsStreamUser
import net.winedownwednesday.web.StreamBridge
import net.winedownwednesday.web.data.models.StreamTokenResponse
import net.winedownwednesday.web.data.repositories.AppRepository
import org.koin.core.annotation.Single

@Single
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
    val messages = _messages.asStateFlow()

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
                println("MessagingViewModel: Error loading channels: ${e.message}")
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
                    println("MessagingViewModel: Auto-registered device with Stream push")
                }
            } catch (e: Exception) {
                // Permission not granted yet or token unavailable — that's OK.
                // User can enable via the bell icon.
                println("MessagingViewModel: Could not auto-register push " +
                        "(permission may not be granted): ${e.message}")
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
                        println("MessagingViewModel: Enabled push notifications")
                    }
                } else {
                    println("MessagingViewModel: Notification permission not granted" +
                            "($permission)")
                }
            } catch (e: Exception) {
                println("MessagingViewModel: Error enabling notifications: ${e.message}")
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
                    println("MessagingViewModel: Disabled push notifications")
                }
            } catch (e: Exception) {
                println("MessagingViewModel: Error disabling notifications: ${e.message}")
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
                println("MessagingViewModel: Error searching users: ${e.message}")
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
                val success = StreamBridge.flagMessage(messageId).await<JsBoolean>()
                if (success.toBoolean()) {
                    println("MessagingViewModel: Message $messageId flagged successfully")
                } else {
                    println("MessagingViewModel: Failed to flag message $messageId")
                }
            } catch (e: Exception) {
                println("MessagingViewModel: Error flagging message: ${e.message}")
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
                println("MessagingViewModel: Error starting direct message: ${e.message}")
            }
        }
    }

    private fun loadMessages(channelId: String) {
        viewModelScope.launch {
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
                println("MessagingViewModel: Error loading messages: ${e.message}")
            }
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
                        StreamBridge.sendImageMessage(channelId, text, file).await<JsChatMessage?>()
                    } else {
                        StreamBridge.sendFileMessage(channelId, text, file).await<JsChatMessage?>()
                    }
                } else if (replyTarget != null) {
                    _replyingTo.value = null
                    StreamBridge.sendReply(channelId, replyTarget.id, text)
                        .await<JsChatMessage?>()
                } else {
                    StreamBridge.sendMessage(channelId, text).await<JsChatMessage?>()
                }
                if (newMessage != null) {
                    _messages.value = _messages.value + newMessage
                }
            } catch (e: Exception) {
                println("MessagingViewModel: Error sending message: ${e.message}")
            }
        }
    }

    fun setReplyingTo(message: JsChatMessage?) {
        _replyingTo.value = message
    }

    fun addReaction(messageId: String, reactionType: String) {
        viewModelScope.launch {
            try {
                println("MessagingViewModel: Adding reaction '$reactionType' to message '$messageId'")
                StreamBridge.addReaction(messageId, reactionType).await<JsBoolean>()
                println("MessagingViewModel: Reaction sent, waiting for consistency...")
                // Small delay to let Stream's backend process the reaction
                kotlinx.coroutines.delay(500)
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
                println("MessagingViewModel: Error adding reaction: ${e.message}")
            }
        }
    }

    fun removeReaction(messageId: String, reactionType: String) {
        viewModelScope.launch {
            try {
                println("MessagingViewModel: Removing reaction '$reactionType' from message '$messageId'")
                StreamBridge.removeReaction(messageId, reactionType).await<JsBoolean>()
                kotlinx.coroutines.delay(500)
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
                println("MessagingViewModel: Error removing reaction: ${e.message}")
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                StreamBridge.deleteMessage(messageId).await<JsBoolean>()
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
                println("MessagingViewModel: Error deleting message: ${e.message}")
            }
        }
    }

    fun pinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                StreamBridge.pinMessage(messageId).await<JsBoolean>()
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
                println("MessagingViewModel: Error pinning message: ${e.message}")
            }
        }
    }

    fun unpinMessage(messageId: String) {
        viewModelScope.launch {
            try {
                StreamBridge.unpinMessage(messageId).await<JsBoolean>()
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
                println("MessagingViewModel: Error unpinning message: ${e.message}")
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
                println("MessagingViewModel: Editing message '$messageId' to '$newText'")
                StreamBridge.editMessage(messageId, newText).await<JsBoolean>()
                _editingMessage.value = null
                _selectedChannelId.value?.let { loadMessages(it) }
            } catch (e: Exception) {
                println("MessagingViewModel: Error editing message: ${e.message}")
            }
        }
    }

    fun sendGiphyMessage(gifUrl: String, gifTitle: String) {
        val channelId = _selectedChannelId.value ?: return
        viewModelScope.launch {
            try {
                val newMessage = StreamBridge.sendGiphyMessage(channelId, gifUrl, gifTitle)
                    .await<JsChatMessage?>()
                if (newMessage != null) {
                    _messages.value = _messages.value + newMessage
                }
            } catch (e: Exception) {
                println("MessagingViewModel: Error sending GIF: ${e.message}")
            }
        }
    }

    fun forwardMessage(messageText: String, targetChannelId: String) {
        viewModelScope.launch {
            try {
                val forwardedText = "↪️ Forwarded:\n$messageText"
                StreamBridge.sendMessage(targetChannelId, forwardedText).await<JsChatMessage?>()
            } catch (e: Exception) {
                println("MessagingViewModel: Error forwarding message: ${e.message}")
            }
        }
    }

    fun notifyTyping() {
        if (typingJob?.isActive == true) {
            typingJob?.cancel()
        } else {
            startTyping()
        }
        
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
                println("Error loading thread replies: ${e.message}")
            }
        }
    }

    fun closeThread() {
        _threadParentMessage.value = null
        _threadReplies.value = emptyList()
    }

    fun sendThreadReply(text: String) {
        val channelId = _selectedChannelId.value ?: return
        val parentId = _threadParentMessage.value?.id ?: return
        viewModelScope.launch {
            try {
                StreamBridge.sendReply(channelId, parentId, text).await<JsChatMessage?>()
                // Reload thread replies
                val replies = StreamBridge.getThreadReplies(channelId, parentId)
                    .await<JsArray<JsChatMessage>>()
                _threadReplies.value = (0 until replies.length).map { replies[it]!! }
                // Also reload main messages to update reply count
                loadMessages(channelId)
            } catch (e: Exception) {
                println("Error sending thread reply: ${e.message}")
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
            }
        }
    }

    private fun throttledRefresh(refreshMessages: Boolean) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        if (now - _lastRefreshTime < REFRESH_THROTTLE_MS) {
            // Too soon — skip this refresh
            return
        }
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
        }
    }
}
