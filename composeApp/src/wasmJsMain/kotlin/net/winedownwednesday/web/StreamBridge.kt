package net.winedownwednesday.web

import kotlin.js.Promise

@JsName("wdwStreamBridge")
external object StreamBridge {
    fun connectUser(apiKey: String, userToken: String, userId: String): Promise<JsAny?>
    fun queryChannels(): Promise<JsArray<JsChatChannel>>
    fun getMessages(channelId: String): Promise<JsArray<JsChatMessage>>
    fun sendMessage(channelId: String, text: String): Promise<JsChatMessage?>
    fun sendReply(channelId: String, parentMessageId: String, text: String): Promise<JsChatMessage?>
    fun getThreadReplies(channelId: String, parentMessageId: String): Promise<JsArray<JsChatMessage>>
    fun deleteMessage(messageId: String): Promise<JsBoolean>
    fun editMessage(messageId: String, newText: String): Promise<JsBoolean>
    fun isAdmin(): Boolean
    fun addReaction(messageId: String, reactionType: String): Promise<JsBoolean>
    fun removeReaction(messageId: String, reactionType: String): Promise<JsBoolean>
    fun startTyping(channelId: String): Promise<JsBoolean>
    fun stopTyping(channelId: String): Promise<JsBoolean>
    fun markRead(channelId: String): Promise<JsBoolean>
    fun sendImageMessage(channelId: String, text: String, file: org.w3c.files.File): Promise<JsChatMessage?>
    fun sendFileMessage(channelId: String, text: String, file: org.w3c.files.File): Promise<JsChatMessage?>
    fun onMessageNew(callback: (JsChatEvent) -> Unit)
    fun searchUsers(query: String): Promise<JsArray<JsStreamUser>>
    fun queryAllUsers(): Promise<JsArray<JsStreamUser>>
    fun createDirectChannel(otherUserId: String): Promise<JsChatChannel?>
    fun joinCommunityChannel(): Promise<JsChatChannel?>
    fun flagMessage(messageId: String): Promise<JsBoolean>
    fun pinMessage(messageId: String): Promise<JsBoolean>
    fun unpinMessage(messageId: String): Promise<JsBoolean>
    fun getPinnedMessages(channelId: String): Promise<JsArray<JsChatMessage>>
    fun sendGiphyMessage(channelId: String, gifUrl: String, gifTitle: String): Promise<JsChatMessage?>
    fun getChannelMembers(channelId: String): Promise<JsArray<JsChannelMember>>
    fun addDevice(token: String, pushProvider: String): Promise<JsBoolean>
    fun removeDevice(token: String): Promise<JsBoolean>
    fun disconnectUser(): Promise<JsAny?>
}

external interface JsChatEvent : JsAny {
    val type: String
    val userId: String
    val userName: String
    val channelId: String
}

external interface JsStreamUser : JsAny {
    val id: String
    val name: String
    val image: String
}

external interface JsChatChannel : JsAny {
    val id: String
    val cid: String
    val name: String
    val image: String
    val lastMessage: String
    val unreadCount: Int
    val isDirectMessage: Boolean
    val otherUserOnline: Boolean
}

external interface JsChatMessage : JsAny {
    val id: String
    val text: String
    val userId: String
    val userName: String
    val userImage: String
    val createdAt: String
    val parentId: String
    val replyCount: Int
    val quotedText: String
    val quotedUserName: String
    val reactions: String  // JSON string: { "like": { "count": 2, "own": true } }
    val attachments: String // JSON string of attachments array
    val isDeleted: Boolean
    val readStatus: String // "sent", "delivered", "read"
    val isMentioned: Boolean
    val userOnline: Boolean
    val pinned: Boolean
}

external interface JsChannelMember : JsAny {
    val userId: String
    val userName: String
    val userImage: String
    val role: String
    val online: Boolean
    val lastActive: String
}

// Drag & drop file helpers
@JsName("setupDragDrop")
external fun setupDragDrop()

@JsName("isDraggingOver")
external fun isDraggingOver(): JsBoolean

@JsName("getDroppedFile")
external fun getDroppedFile(): org.w3c.files.File?
