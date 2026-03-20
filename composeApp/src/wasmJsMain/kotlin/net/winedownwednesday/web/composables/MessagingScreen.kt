package net.winedownwednesday.web.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import net.winedownwednesday.web.JsChannelMember
import net.winedownwednesday.web.JsChatChannel
import net.winedownwednesday.web.JsChatMessage
import net.winedownwednesday.web.JsStreamUser
import net.winedownwednesday.web.StreamBridge
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.getDroppedFile
import net.winedownwednesday.web.isDraggingOver
import net.winedownwednesday.web.setupDragDrop
import net.winedownwednesday.web.vibrate
import net.winedownwednesday.web.viewmodels.MessagingViewModel
import org.kodein.emoji.compose.EmojiService
import org.kodein.emoji.compose.m3.TextWithNotoImageEmoji
import org.koin.compose.koinInject
import kotlin.math.roundToInt

data class ChatAttachment(
    val type: String,
    val title: String,
    val text: String,
    val titleLink: String,
    val thumbUrl: String,
    val imageUrl: String,
    val fileUrl: String = "",
    val fileSize: Long = 0L,
    val mimeType: String = ""
)

val LocalMembers = androidx.compose.runtime.compositionLocalOf<List<Member?>> { emptyList() }

data class ModerationCallbacks(
    val currentUserId: String? = null,
    val blockedUserIds: List<String> = emptyList(),
    val blockedUserProfiles: List<JsStreamUser> = emptyList(),
    val onBlock: (targetUserId: String) -> Unit = {},
    val onUnblock: (targetUserId: String) -> Unit = {},
    val onReport: (targetUserId: String, reason: String?, category: String) -> Unit = { _, _, _ -> },
    val fetchBlockedUserProfiles: () -> Unit = {}
)

val LocalModeration = androidx.compose.runtime.compositionLocalOf { ModerationCallbacks() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
    isCompactScreen: Boolean
) {
    val viewModel: MessagingViewModel = koinInject()
    val streamToken by viewModel.streamToken.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val error by viewModel.error.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val selectedChannelId by viewModel.selectedChannelId.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val channelSearchQuery by viewModel.channelSearchQuery.collectAsState()
    val messageSearchQuery by viewModel.messageSearchQuery.collectAsState()
    val threadParentMessage by viewModel.threadParentMessage.collectAsState()
    val threadReplies by viewModel.threadReplies.collectAsState()
    val membersList by viewModel.members.collectAsState()
    val blockedEmails by viewModel.blockedEmails.collectAsState()
    val blockedUserProfiles by viewModel.blockedUserProfiles.collectAsState()
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Chat Settings Dialog (hoisted here so it survives layout changes)
    if (showSettingsDialog) {
        LaunchedEffect(Unit) {
            viewModel.fetchBlockedUserProfiles()
        }
        ChatSettingsDialog(
            blockedUsers = blockedUserProfiles,
            onUnblock = { userId ->
                viewModel.unblockUser(userId)
                viewModel.fetchBlockedUserProfiles()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Report dialog (driven by ViewModel state — survives layout/resize changes)
    val reportTarget by viewModel.reportDialogTarget.collectAsState()
    reportTarget?.let { target ->
        ReportUserDialog(
            userName = target.userName,
            onDismiss = { viewModel.dismissReportDialog() },
            onSubmit = { reason, category ->
                viewModel.dismissReportDialog()
                viewModel.flagUser(target.userId, reason, category)
            }
        )
    }

    // Initialize the emoji image service (loads Noto glyphs for Wasm)
    remember { EmojiService.initialize() }

    LaunchedEffect(Unit) {
        viewModel.connectToChat()
        viewModel.fetchBlockedUsers()
    }

    val moderationCallbacks = remember(
        streamToken?.userId,
        blockedEmails,
        blockedUserProfiles
    )
    {
        ModerationCallbacks(
            currentUserId = streamToken?.userId,
            blockedUserIds = blockedEmails,
            blockedUserProfiles = blockedUserProfiles,
            onBlock = { userId -> viewModel.blockUser(userId) },
            onUnblock = { userId -> viewModel.unblockUser(userId) },
            onReport = { userId, reason, category ->
                viewModel.flagUser(userId, reason, category)
            },
            fetchBlockedUserProfiles = { viewModel.fetchBlockedUserProfiles() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CompositionLocalProvider(
            LocalMembers provides membersList,
            LocalModeration provides moderationCallbacks
        ) {
            when {
                isConnecting -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF7F33))
                    }
                }

                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: $error", color = Color.Red)
                    }
                }

                streamToken != null -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        ChatLayout(
                            isCompactScreen = isCompactScreen,
                            channels = channels,
                            messages = messages,
                            selectedChannelId = selectedChannelId,
                            currentUserId = streamToken?.userId,
                            isAdmin = isAdmin,
                            replyingTo = replyingTo,
                            editingMessage = editingMessage,
                            typingUsers = typingUsers,
                            onChannelSelect = { viewModel.selectChannel(it) },
                            onSendMessage = { text, file -> viewModel.sendMessage(text, file) },
                            onReply = { viewModel.setReplyingTo(it) },
                            onDelete = { viewModel.deleteMessage(it) },
                            onCancelReply = { viewModel.setReplyingTo(null) },
                            onReaction = { messageId, type ->
                                viewModel.addReaction(messageId, type)
                            },
                            onFlag = { messageId -> viewModel.flagMessage(messageId) },
                            onEdit = { viewModel.setEditingMessage(it) },
                            onEditMessage = { id, text ->
                                viewModel.editMessage(id, text)
                            },
                            onCancelEditing = { viewModel.cancelEditing() },
                            onTyping = { viewModel.notifyTyping() },
                            onNewChatClick = { showNewChatDialog = true },
                            notificationsEnabled = notificationsEnabled,
                            onEnableNotifications = { viewModel.toggleNotifications() },
                            channelSearchQuery = channelSearchQuery,
                            onChannelSearchQueryChange = { viewModel.setChannelSearchQuery(it) },
                            messageSearchQuery = messageSearchQuery,
                            onMessageSearchQueryChange = { viewModel.setMessageSearchQuery(it) },
                            onOpenThread = { viewModel.openThread(it) },
                            onPin = { viewModel.pinMessage(it) },
                            onUnpin = { viewModel.unpinMessage(it) },
                            onSendGif = { url, title ->
                                viewModel.sendGiphyMessage(url, title)
                            },
                            onForward = { text, channelId ->
                                viewModel.forwardMessage(
                                    text,
                                    channelId
                                )
                            },
                            onOpenSettings = { showSettingsDialog = true },
                            modifier = Modifier
                                .weight(if (threadParentMessage != null) 0.65f else 1f)
                                .fillMaxHeight()
                        )
                        // Thread panel slides in from the right
                        if (threadParentMessage != null) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF333333))
                            )
                            ThreadPanel(
                                parentMessage = threadParentMessage!!,
                                replies = threadReplies,
                                currentUserId = streamToken?.userId,
                                onClose = { viewModel.closeThread() },
                                onSendReply = { viewModel.sendThreadReply(it) },
                                modifier = Modifier
                                    .weight(0.35f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Initializing chat...", color = Color.White)
                    }
                }
            }
        } // CompositionLocalProvider

        // Moderation feedback toast
        val moderationMsg by viewModel.moderationMessage.collectAsState()
        moderationMsg?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = if (msg.contains("Failed") || msg.contains("Error"))
                                Color(0xCC_C0392B.toInt())
                            else Color(0xCC_2E7D32.toInt()),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clickable { viewModel.clearModerationMessage() }
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewChatDialog(
            searchResults = searchResults,
            onSearch = { viewModel.searchUsers(it) },
            onSelectUser = {
                viewModel.startDirectMessage(it)
                showNewChatDialog = false
                viewModel.clearSearch()
            },
            onDismiss = {
                showNewChatDialog = false
                viewModel.clearSearch()
            }
        )
    }
}

@Composable
fun ChatLayout(
    isCompactScreen: Boolean,
    channels: List<JsChatChannel>,
    messages: List<JsChatMessage>,
    selectedChannelId: String?,
    currentUserId: String?,
    isAdmin: Boolean,
    replyingTo: JsChatMessage?,
    editingMessage: JsChatMessage?,
    typingUsers: List<String>,
    onChannelSelect: (String) -> Unit,
    onSendMessage: (String, org.w3c.files.File?) -> Unit,
    onReply: (JsChatMessage) -> Unit,
    onDelete: (String) -> Unit,
    onCancelReply: () -> Unit,
    onReaction: (String, String) -> Unit,
    onFlag: (String) -> Unit,
    onEdit: (JsChatMessage) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onCancelEditing: () -> Unit,
    onTyping: () -> Unit,
    onNewChatClick: () -> Unit,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    channelSearchQuery: String,
    onChannelSearchQueryChange: (String) -> Unit,
    messageSearchQuery: String,
    onMessageSearchQueryChange: (String) -> Unit,
    onOpenThread: (JsChatMessage) -> Unit,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
    onSendGif: (String, String) -> Unit,
    onForward: (String, String) -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var splitRatio by remember { mutableStateOf(0.3f) }

    BoxWithConstraints(modifier = modifier) {
        val totalWidth = constraints.maxWidth.toFloat()

        Row(modifier = Modifier.fillMaxSize()) {
            // Channel Sidebar
            if (!isCompactScreen || (isCompactScreen && selectedChannelId == null)) {
                ChannelSidebar(
                    modifier = Modifier
                        .weight(if (isCompactScreen) 1f else splitRatio)
                        .fillMaxHeight(),
                    channels = channels,
                    selectedChannelId = selectedChannelId,
                    onChannelSelect = onChannelSelect,
                    onNewChatClick = onNewChatClick,
                    notificationsEnabled = notificationsEnabled,
                    onEnableNotifications = onEnableNotifications,
                    searchQuery = channelSearchQuery,
                    onSearchQueryChange = onChannelSearchQueryChange,
                    isCompactScreen = isCompactScreen
                )

                // Draggable divider (only on non-compact screens)
                if (!isCompactScreen) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val delta = dragAmount.x / totalWidth
                                    splitRatio = (splitRatio + delta).coerceIn(0.2f, 0.5f)
                                }
                            }
                    ) {
                        // Visual divider indicator
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .align(Alignment.Center)
                                .background(Color(0xFF333333))
                        )
                    }
                }
            }

            // Chat Area
            if (!isCompactScreen || (isCompactScreen && selectedChannelId != null)) {
                val selectedChannelName =
                    channels.firstOrNull { it.id == selectedChannelId }?.name ?: "Chat"
                Card(
                    modifier = Modifier
                        .weight(if (isCompactScreen) 1f else (1f - splitRatio))
                        .fillMaxHeight()
                        .padding(if (isCompactScreen) 4.dp else 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    elevation = CardDefaults
                        .cardElevation(if (isCompactScreen) 0.dp else 4.dp)
                ) {
                    ChatArea(
                        modifier = Modifier.fillMaxSize(),
                        messages = messages,
                        channelName = selectedChannelName,
                        selectedChannelId = selectedChannelId,
                        currentUserId = currentUserId,
                        isAdmin = isAdmin,
                        replyingTo = replyingTo,
                        editingMessage = editingMessage,
                        typingUsers = typingUsers,
                        onSendMessage = onSendMessage,
                        onReply = onReply,
                        onDelete = onDelete,
                        onCancelReply = onCancelReply,
                        onReaction = onReaction,
                        onFlag = onFlag,
                        onEdit = onEdit,
                        onEditMessage = onEditMessage,
                        onCancelEditing = onCancelEditing,
                        onTyping = onTyping,
                        onBack = if (isCompactScreen) {
                            { onChannelSelect("") }
                        } else null,
                        searchQuery = messageSearchQuery,
                        onSearchQueryChange = onMessageSearchQueryChange,
                        onOpenThread = onOpenThread,
                        onPin = onPin,
                        onUnpin = onUnpin,
                        onSendGif = onSendGif,
                        onForward = onForward,
                        channels = channels,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelSidebar(
    modifier: Modifier,
    channels: List<JsChatChannel>,
    selectedChannelId: String?,
    onChannelSelect: (String) -> Unit,
    onNewChatClick: () -> Unit,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isCompactScreen: Boolean = false
) {
    Card(
        modifier = modifier.padding(if (isCompactScreen) 4.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        elevation = CardDefaults
            .cardElevation(if (isCompactScreen) 0.dp else 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Conversations",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Bell icon for notifications toggle
                IconButton(onClick = onEnableNotifications) {
                    Icon(
                        if (notificationsEnabled) Icons.Default.Notifications
                        else Icons.Default.Notifications,
                        contentDescription = if (notificationsEnabled) "Mute Notifications" else "Enable Notifications",
                        tint = if (notificationsEnabled) Color.Gray else Color(0xFFFF7F33)
                    )
                }

                IconButton(onClick = onNewChatClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = Color.White
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF2A2A2A))
            )

            Spacer(modifier = Modifier.height(8.dp))

            SearchBar(
                label = "Search conversations",
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )

            val filteredChannels = remember(channels, searchQuery) {
                if (searchQuery.isBlank()) channels
                else channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            if (filteredChannels.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (channels.isEmpty()) "No conversations yet" else "No matching conversations",
                        color = Color.Gray
                    )
                }
            } else {
                val groupChats = filteredChannels.filter { !it.isDirectMessage }
                val dms = filteredChannels.filter { it.isDirectMessage }
                val blockedIds = LocalModeration.current.blockedUserIds
                val unblockedDMs = dms.filter { it.otherUserId !in blockedIds }
                val blockedDMs = dms.filter { it.otherUserId in blockedIds }
                var blockedExpanded by remember { mutableStateOf(false) }
                var unblockTarget by remember { mutableStateOf<JsChatChannel?>(null) }
                val moderation = LocalModeration.current

                // Unblock confirmation dialog
                unblockTarget?.let { ch ->
                    AlertDialog(
                        onDismissRequest = { unblockTarget = null },
                        containerColor = Color(0xFF1E1E1E),
                        title = {
                            Text(
                                "Unblock ${ch.name}?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                "You will be able to see their messages again.",
                                color = Color.Gray
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                moderation.onUnblock(ch.otherUserId)
                                unblockTarget = null
                            }) {
                                Text("Unblock", color = Color(0xFFFF7F33))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { unblockTarget = null }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        }
                    )
                }

                LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
                    if (groupChats.isNotEmpty()) {
                        item {
                            Text(
                                "COMMUNITIES",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    top = 16.dp,
                                    bottom = 8.dp
                                )
                            )
                        }
                        items(groupChats) { channel ->
                            ChannelItem(
                                channel = channel,
                                isSelected = channel.id == selectedChannelId,
                                onClick = { onChannelSelect(channel.id) }
                            )
                        }
                    }

                    if (unblockedDMs.isNotEmpty()) {
                        item {
                            Text(
                                "DIRECT MESSAGES",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    top = 16.dp,
                                    bottom = 8.dp
                                )
                            )
                        }
                        items(unblockedDMs) { channel ->
                            ChannelItem(
                                channel = channel,
                                isSelected = channel.id == selectedChannelId,
                                onClick = { onChannelSelect(channel.id) }
                            )
                        }
                    }

                    if (blockedDMs.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { blockedExpanded = !blockedExpanded }
                                    .padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "BLOCKED (${blockedDMs.size})",
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (blockedExpanded)
                                        Icons.Default.KeyboardArrowDown
                                    else Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Toggle",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (blockedExpanded) {
                            items(blockedDMs) { channel ->
                                // Gray out blocked channels
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable { unblockTarget = channel },
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF1A1A1A)
                                    ),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF333333)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                channel.name.take(1).uppercase(),
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                channel.name,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                            Text(
                                                "Blocked",
                                                color = Color.Gray.copy(alpha = 0.5f),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(
                                            Icons.Default.Block,
                                            contentDescription = "Blocked",
                                            tint = Color.Gray.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelItem(
    channel: JsChatChannel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val channelSubtitle = when (channel.id) {
        "wdw-community" -> "Everyone"
        "wdw-members" -> "Members & Leaders"
        "wdw-leaders" -> "Leaders Only"
        else -> null
    }

    val bgColor = if (isSelected) Color(0xFFFF7F33) else Color(0xFF2A2A2A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(30.dp)
                        .background(Color(0xFFFF7F33), RoundedCornerShape(1.5.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(11.dp))
            }

            Box(
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFFFF7F33)),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.image.isNotEmpty()) {
                        AsyncImage(
                            model = channel.image,
                            contentDescription = "${channel.name} avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = channel.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Online presence dot for DM channels
                if (channel.isDirectMessage && channel.otherUserOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF121212), CircleShape)
                            .padding(2.dp)
                            .background(Color(0xFF44b700), CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(channel.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                if (channelSubtitle != null) {
                    Text(
                        channelSubtitle,
                        color = Color(0xFFFF7F33),
                        fontSize = 10.sp
                    )
                }
                val blockedIds = LocalModeration.current.blockedUserIds
                val isLastMsgBlocked = channel.lastMessageUserId.isNotEmpty() &&
                        channel.lastMessageUserId in blockedIds
                if (channel.lastMessage.isNotEmpty() && !isLastMsgBlocked) {
                    Text(
                        channel.lastMessage,
                        color = Color.Gray,
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }
            }
            if (channel.unreadCount > 0) {
                Badge(containerColor = Color(0xFFFF7F33)) {
                    Text(channel.unreadCount.toString(), color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatArea(
    modifier: Modifier,
    messages: List<JsChatMessage>,
    channelName: String,
    selectedChannelId: String?,
    currentUserId: String?,
    isAdmin: Boolean,
    replyingTo: JsChatMessage?,
    editingMessage: JsChatMessage?,
    typingUsers: List<String>,
    onSendMessage: (String, org.w3c.files.File?) -> Unit,
    onReply: (JsChatMessage) -> Unit,
    onDelete: (String) -> Unit,
    onCancelReply: () -> Unit,
    onReaction: (String, String) -> Unit,
    onFlag: (String) -> Unit,
    onEdit: (JsChatMessage) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onCancelEditing: () -> Unit,
    onTyping: () -> Unit,
    onBack: (() -> Unit)?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenThread: (JsChatMessage) -> Unit,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
    onSendGif: (String, String) -> Unit,
    onForward: (String, String) -> Unit,
    channels: List<JsChatChannel>,
    onOpenSettings: () -> Unit = {}
) {
    var activeReactionMessageId by remember { mutableStateOf<String?>(null) }
    var isInputEmojiPickerOpen by remember { mutableStateOf(false) }
    var isGifPanelOpen by remember { mutableStateOf(false) }
    var forwardingMessage by remember { mutableStateOf<JsChatMessage?>(null) }
    var lightboxImageUrl by remember { mutableStateOf<String?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    val closePickers = {
        activeReactionMessageId = null
        isInputEmojiPickerOpen = false
        isGifPanelOpen = false
    }

    if (selectedChannelId == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Select a conversation to start chatting", color = Color.Gray)
        }
        return
    }

    // Setup drag & drop and poll for state changes
    LaunchedEffect(selectedChannelId) {
        setupDragDrop()
        while (true) {
            isDragging = isDraggingOver().toBoolean()
            val droppedFile = getDroppedFile()
            if (droppedFile != null) {
                onSendMessage("", droppedFile)
            }
            kotlinx.coroutines.delay(100)
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            var isSearchVisible by remember { mutableStateOf(false) }
            var isChannelInfoOpen by remember { mutableStateOf(false) }
            var channelMembers by
            remember { mutableStateOf<List<JsChannelMember>>(emptyList()) }
            // Settings dialog state is hoisted to MessagingScreen

            // Chat Header
            TopAppBar(
                title = { Text(channelName, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                navigationIcon = {
                    if (onBack != null) {
                        Text(
                            "< Back",
                            color = Color(0xFFFF7F33),
                            modifier = Modifier.clickable { onBack() }.padding(8.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible) onSearchQueryChange("")
                    }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search messages",
                            tint = if (isSearchVisible) Color(0xFFFF7F33) else Color.White
                        )
                    }
                    IconButton(onClick = {
                        isChannelInfoOpen = !isChannelInfoOpen
                        if (isChannelInfoOpen && selectedChannelId != null) {
                            StreamBridge.getChannelMembers(selectedChannelId).then { members ->
                                channelMembers = (0 until members.length)
                                    .mapNotNull { members[it] }
                                null
                            }
                        }
                    }) {
                        TextWithNotoImageEmoji(
                            "\u2139\uFE0F",
                            fontSize = 18.sp,
                            color = if (isChannelInfoOpen) Color(0xFFFF7F33) else Color.White
                        )
                    }
                    // Settings
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            )

            AnimatedVisibility(visible = isSearchVisible) {
                SearchBar(
                    label = "Search messages",
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange
                )
            }

            // Channel Info Panel
            AnimatedVisibility(visible = isChannelInfoOpen) {
                ChannelInfoPanel(
                    channelName = channelName,
                    members = channelMembers,
                    onDismiss = { isChannelInfoOpen = false }
                )
            }

            val filteredMessages = remember(messages, searchQuery) {
                if (searchQuery.isBlank()) messages
                else messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
            }

            // Messages List
            val listState = rememberLazyListState()

            // Only auto-scroll to bottom when user is already at (or near) the bottom
            val isAtBottom by remember {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisible >= listState.layoutInfo.totalItemsCount - 2
                }
            }

            var unreadCount by remember { mutableStateOf(0) }

            // Track new messages arriving while scrolled up
            LaunchedEffect(filteredMessages.size) {
                if (filteredMessages.isNotEmpty()) {
                    if (isAtBottom) {
                        listState.animateScrollToItem(filteredMessages.size - 1)
                        unreadCount = 0
                    } else {
                        unreadCount++
                    }
                }
            }

            // Reset unread counter when user scrolls to bottom manually
            LaunchedEffect(isAtBottom) {
                if (isAtBottom) unreadCount = 0
            }

            val coroutineScope = rememberCoroutineScope()

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(filteredMessages) { index, message ->
                        // Date separator: show when date changes from previous message
                        val currentDate = extractDatePart(message.createdAt)
                        val previousDate =
                            if (index > 0) extractDatePart(filteredMessages[index - 1].createdAt) else null
                        if (currentDate != previousDate) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatDateLabel(currentDate),
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .background(
                                            Color(0xFF1E1E1E),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        MessageBubble(
                            message = message,
                            isMe = message.userId == currentUserId,
                            isAdmin = isAdmin,
                            onReply = { onReply(message) },
                            onDelete = { onDelete(message.id) },
                            onReaction = { type -> onReaction(message.id, type) },
                            onFlag = { onFlag(message.id) },
                            onEdit = { onEdit(message) },
                            isEmojiPickerOpen = activeReactionMessageId == message.id,
                            onToggleEmojiPicker = {
                                if (activeReactionMessageId == message.id) {
                                    activeReactionMessageId = null
                                } else {
                                    closePickers()
                                    activeReactionMessageId = message.id
                                }
                            },
                            onClosePickers = closePickers,
                            onEmojiPickerOpen = {
                                coroutineScope.launch {
                                    // Scroll so the message + its picker are visible
                                    listState.animateScrollToItem(index)
                                }
                            },
                            onOpenThread = { onOpenThread(message) },
                            onPin = { onPin(message.id) },
                            onUnpin = { onUnpin(message.id) },
                            onForward = { forwardingMessage = message },
                            onImageClick = { lightboxImageUrl = it },
                            currentUserId = currentUserId,
                        )
                    }
                }

                // Scroll-to-bottom FAB with unread badge
                if (!isAtBottom) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    ) {
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    if (filteredMessages.isNotEmpty()) {
                                        listState.animateScrollToItem(filteredMessages.size - 1)
                                    }
                                    unreadCount = 0
                                }
                            },
                            color = Color(0xFF2C2C2C),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center, modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Scroll to bottom",
                                    tint = Color(0xFFFF7F33),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        // Unread badge
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = Color(0xFFFF7F33),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Typing Indicator
            AnimatedVisibility(
                visible = typingUsers.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val typingText = when (typingUsers.size) {
                    1 -> "${typingUsers.first()} is typing..."
                    2 -> "${typingUsers[0]} and ${typingUsers[1]} are typing..."
                    else -> "Several people are typing..."
                }
                Text(
                    text = typingText,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            // Reply Banner
            AnimatedVisibility(
                visible = replyingTo != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                if (replyingTo != null) {
                    ReplyBanner(
                        replyingTo = replyingTo,
                        onCancel = onCancelReply
                    )
                }
            }

            // GIF Search Panel
            AnimatedVisibility(visible = isGifPanelOpen) {
                GifSearchPanel(
                    onGifSelected = { url, title ->
                        onSendGif(url, title)
                        isGifPanelOpen = false
                    },
                    onDismiss = { isGifPanelOpen = false }
                )
            }

            // Input Area
            MessageInput(
                replyingToUser = replyingTo?.userName,
                editingMessage = editingMessage,
                onSendMessage = onSendMessage,
                onEditMessage = onEditMessage,
                onCancelEditing = onCancelEditing,
                onTyping = onTyping,
                isEmojiPickerOpen = isInputEmojiPickerOpen,
                onToggleEmojiPicker = {
                    if (isInputEmojiPickerOpen) {
                        isInputEmojiPickerOpen = false
                    } else {
                        closePickers()
                        isInputEmojiPickerOpen = true
                    }
                },
                onClosePickers = closePickers,
                isGifPanelOpen = isGifPanelOpen,
                onToggleGifPanel = {
                    isGifPanelOpen = !isGifPanelOpen
                }
            )
        }

        // Forward dialog
        if (forwardingMessage != null) {
            ForwardMessageDialog(
                message = forwardingMessage!!,
                channels = channels,
                onForward = { text, channelId ->
                    onForward(text, channelId)
                    forwardingMessage = null
                },
                onDismiss = { forwardingMessage = null }
            )
        }

        // Image lightbox
        if (lightboxImageUrl != null) {
            ImageLightboxDialog(
                imageUrl = lightboxImageUrl!!,
                onDismiss = { lightboxImageUrl = null }
            )
        }

        // Drag overlay
        if (isDragging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(
                        width = 3.dp,
                        color = Color(0xFFFF7F33),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextWithNotoImageEmoji(
                        "\uD83D\uDCCE",
                        fontSize = 40.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Drop file here",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } // end Box
}

@Composable
fun MessageBubble(
    message: JsChatMessage,
    isMe: Boolean,
    isAdmin: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onReaction: (String) -> Unit,
    onFlag: () -> Unit,
    onEdit: () -> Unit,
    isEmojiPickerOpen: Boolean,
    onToggleEmojiPicker: () -> Unit,
    onClosePickers: () -> Unit,
    onEmojiPickerOpen: () -> Unit = {},
    onOpenThread: () -> Unit = {},
    onPin: () -> Unit = {},
    onUnpin: () -> Unit = {},
    onForward: () -> Unit = {},
    onImageClick: (String) -> Unit = {},
    currentUserId: String?,
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 60f
    var showProfilePopover by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Reply arrow icon (hidden behind the bubble, revealed on drag)
        if (dragOffsetX > 0) {
            val progress = (dragOffsetX / swipeThreshold).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(if (isMe) Alignment.CenterStart else Alignment.CenterStart)
                    .offset { IntOffset(0, 0) }
                    .padding(start = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2C2C2C).copy(alpha = progress),
                    modifier = Modifier.size(32.dp * progress)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Reply",
                            tint = Color.White.copy(alpha = progress),
                            modifier = Modifier.size(16.dp * progress)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                .let {
                    if (!message.isDeleted) {
                        it.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    dragOffsetX =
                                        (dragOffsetX + dragAmount).coerceAtLeast(0f)
                                },
                                onDragEnd = {
                                    if (dragOffsetX >= swipeThreshold) {
                                        onReply()
                                    }
                                    dragOffsetX = 0f
                                },
                                onDragCancel = {
                                    dragOffsetX = 0f
                                }
                            )
                        }
                    } else it
                },
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Avatar for non-self messages
            if (!isMe) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { showProfilePopover = true }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF555555)),
                        contentAlignment = Alignment.Center
                    ) {
                        val userImg = message.userImage
                        if (userImg.isNotEmpty()) {
                            AsyncImage(
                                model = userImg,
                                contentDescription = message.userName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = message.userName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Online presence dot
                    if (message.userOnline) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color(0xFF121212), CircleShape)
                                .padding(1.5.dp)
                                .background(Color(0xFF44b700), CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Profile popover
                if (showProfilePopover) {
                    val moderation = LocalModeration.current
                    val messagingViewModel: MessagingViewModel = koinInject()
                    UserProfilePopover(
                        userName = message.userName,
                        userImage = message.userImage,
                        isOnline = message.userOnline,
                        userId = message.userId,
                        isSelf = message.userId == currentUserId,
                        isBlocked = message.userId in moderation.blockedUserIds,
                        onDismiss = { showProfilePopover = false },
                        onBlock = { moderation.onBlock(message.userId) },
                        onUnblock = { moderation.onUnblock(message.userId) },
                        onReportClick = {
                            showProfilePopover = false
                            messagingViewModel.openReportDialog(
                                message.userName,
                                message.userId
                            )
                        }
                    )
                }
            }

            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                if (!isMe) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            message.userName,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                        if (message.pinned) {
                            Spacer(Modifier.width(4.dp))
                            TextWithNotoImageEmoji(
                                "\uD83D\uDCCC",
                                fontSize = 10.sp
                            )
                            Text(
                                " Pinned",
                                color = Color(0xFFFF7F33),
                                fontSize = 9.sp
                            )
                        }
                    }
                } else if (message.pinned) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp, bottom = 2.dp)
                    ) {
                        TextWithNotoImageEmoji(
                            "\uD83D\uDCCC",
                            fontSize = 10.sp
                        )
                        Text(
                            " Pinned",
                            color = Color(0xFFFF7F33),
                            fontSize = 9.sp
                        )
                    }
                }

                Box {
                    val mentionBorder = if (message.isMentioned) {
                        Modifier.border(
                            2.dp, Color(0xFFFF7F33), RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            )
                        )
                    } else Modifier

                    // Detect media/file-only messages (no text, has image/giphy/file attachment)
                    val parsedAttachments = remember(message.attachments) {
                        parseAttachments(message.attachments)
                    }
                    val isMediaOnlyMessage = remember(
                        message.text,
                        message.attachments
                    )
                    {
                        val textTrimmed = message.text.trim()
                        val hasNoText = textTrimmed.isEmpty()
                        val hasMediaAttachment = parsedAttachments.any { att ->
                            att.type == "image" ||
                                    att.type == "giphy" ||
                                    att.type == "file" ||
                                    att.imageUrl.isNotEmpty() ||
                                    att.titleLink.isNotEmpty()
                        }
                        // Check if text is just a URL that generated a link preview:
                        // Link-only messages have no spaces, are short, and have a link preview
                        val hasLinkPreview = parsedAttachments.any { it.titleLink.isNotEmpty() }
                        val textIsJustUrl = hasLinkPreview &&
                                textTrimmed.length <= 200 &&
                                !textTrimmed.contains(' ') &&
                                (textTrimmed.contains('.') ||
                                        textTrimmed.startsWith("http"))
                        (hasNoText || textIsJustUrl) && hasMediaAttachment
                    }

                    Surface(
                        color = if (isMediaOnlyMessage) Color.Transparent
                        else if (isMe) Color(0xFFFF7F33) else Color(0xFF2C2C2C),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        ),
                        modifier = Modifier
                            .then(mentionBorder)
                            .then(
                                if (isMediaOnlyMessage) Modifier.widthIn(max = 220.dp)
                                else Modifier
                            )
                            .let {
                                if (!message.isDeleted) {
                                    it.pointerInput(isEmojiPickerOpen) {
                                        detectTapGestures(
                                            onTap = {
                                                if (isEmojiPickerOpen) {
                                                    onToggleEmojiPicker()
                                                } else {
                                                    onClosePickers()
                                                }
                                            },
                                            onLongPress = {
                                                if (!isEmojiPickerOpen) {
                                                    onToggleEmojiPicker()
                                                }
                                                onEmojiPickerOpen()
                                            }
                                        )
                                    }
                                } else it
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = if (isMediaOnlyMessage) 4.dp else 12.dp,
                                vertical = if (isMediaOnlyMessage) 4.dp else 8.dp
                            )
                        ) {
                            // Quoted message preview (for replies)
                            if (message.quotedText.isNotEmpty()) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Row(modifier = Modifier.padding(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(24.dp)
                                                .background(
                                                    if (isMe) Color.White.copy(alpha = 0.6f)
                                                    else Color(0xFFFF7F33),
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                message.quotedUserName,
                                                color = if (isMe) Color.White.copy(alpha = 0.8f)
                                                else Color(0xFFFF7F33),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                message.quotedText.take(50) +
                                                        if (message.quotedText.length > 50) "\u2026" else "",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                            Column {
                                if (parsedAttachments.isNotEmpty() && !message.isDeleted) {
                                    Column(
                                        modifier = Modifier.padding(
                                            bottom =
                                                if (isMediaOnlyMessage) 0.dp else 4.dp
                                        )
                                    ) {
                                        parsedAttachments.forEach { attachment ->
                                            if (attachment.type == "image" || attachment.type ==
                                                "giphy" || (attachment.imageUrl.isNotEmpty() &&
                                                        attachment.title.isEmpty())
                                            ) {
                                                val imageSource = attachment.imageUrl.ifEmpty {
                                                    attachment.thumbUrl
                                                }
                                                val isGif = imageSource
                                                    .contains("tenor", ignoreCase = true) ||
                                                        imageSource.endsWith(
                                                            ".gif",
                                                            ignoreCase = true
                                                        ) || attachment.type == "giphy"
                                                if (isGif) {
                                                    // Static GIF preview with "GIF" badge — click to view in lightbox
                                                    Box {
                                                        AsyncImage(
                                                            model = imageSource,
                                                            contentDescription = "GIF",
                                                            modifier = Modifier
                                                                .then(
                                                                    if (isMediaOnlyMessage) Modifier.widthIn(
                                                                        max = 200.dp
                                                                    ).heightIn(max = 160.dp)
                                                                    else Modifier.widthIn(max = 160.dp)
                                                                        .heightIn(max = 130.dp)
                                                                )
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .clickable {
                                                                    onImageClick(
                                                                        imageSource
                                                                    )
                                                                },
                                                            contentScale = ContentScale.Fit
                                                        )
                                                        // "GIF" badge in bottom-right corner
                                                        Surface(
                                                            color = Color.Black.copy(alpha = 0.7f),
                                                            shape = RoundedCornerShape(4.dp),
                                                            modifier = Modifier
                                                                .align(Alignment.BottomEnd)
                                                                .padding(6.dp)
                                                                .clickable {
                                                                    onImageClick(
                                                                        imageSource
                                                                    )
                                                                }
                                                        ) {
                                                            Text(
                                                                "GIF ▶",
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 6.dp,
                                                                    vertical = 2.dp
                                                                )
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    AsyncImage(
                                                        model = imageSource,
                                                        contentDescription = "Attached Image",
                                                        modifier = Modifier
                                                            .then(
                                                                if (isMediaOnlyMessage)
                                                                    Modifier.widthIn(
                                                                        max = 200.dp
                                                                    ).heightIn(max = 160.dp) else
                                                                        Modifier.fillMaxWidth()
                                                                            .heightIn(max = 240.dp)
                                                            )
                                                            .clip(
                                                                RoundedCornerShape(
                                                                    8.dp)
                                                            )
                                                            .clickable {
                                                                onImageClick(imageSource)
                                                            },
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (message.isDeleted) {
                                        Text(
                                            text = message.text,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontStyle = Italic,
                                            modifier = Modifier.weight(1f, fill = false),
                                            fontSize = 14.sp
                                        )
                                    } else if (message.text.isNotEmpty() && !isMediaOnlyMessage) {
                                        TextWithNotoImageEmoji(
                                            text = message.text,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f, fill = false))
                                    }

                                    // Reaction chips inside the bubble at the end of text
                                    val reactions = remember(
                                        message.id + message.reactions
                                    ) { parseReactions(message.reactions) }
                                    AnimatedVisibility(
                                        visible = reactions.isNotEmpty() &&
                                                !message.isDeleted
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.offset(y = 4.dp)
                                        ) {
                                            reactions.forEach { (type, data) ->
                                                val def = reactionDefs.find { it.type == type }
                                                val emoji = if (def != null) {
                                                    def.emoji
                                                } else if (type.startsWith("emoji_")) {
                                                    // Convert hex-encoded type back to emoji
                                                    try {
                                                        type.removePrefix("emoji_")
                                                            .split("_")
                                                            .map { it.toInt(16).toChar() }
                                                            .joinToString("")
                                                    } catch (_: Exception) {
                                                        type
                                                    }
                                                } else {
                                                    type
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .clickable {
                                                            hapticVibrate(HapticDuration.TICK)
                                                            onReaction(type)
                                                        }
                                                        .padding(
                                                            horizontal = 2.dp,
                                                            vertical = 2.dp
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement
                                                        .spacedBy(2.dp)
                                                ) {
                                                    TextWithNotoImageEmoji(
                                                        emoji,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        "${data.first}",
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (parsedAttachments.isNotEmpty() && !message.isDeleted) {
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        parsedAttachments.forEach { attachment ->
                                            if (attachment.type == "file" && attachment.fileUrl
                                                    .isNotEmpty()
                                            ) {
                                                FileAttachmentCard(attachment = attachment)
                                            } else if (attachment.type == "og_scrape" ||
                                                attachment.title.isNotEmpty()
                                            ) {
                                                LinkPreviewCard(attachment = attachment)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Timestamp + action row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)
                ) {
                    Text(
                        text = formatMessageTime(message.createdAt),
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        val receiptIcon = if (message.readStatus == "read" ||
                            message.readStatus == "delivered"
                        ) {
                            Icons.Default.DoneAll
                        } else {
                            Icons.Default.Done
                        }
                        val receiptColor = if (message
                                .readStatus == "read"
                        ) Color(0xFF4ea4e8) else Color.Gray

                        Icon(
                            receiptIcon,
                            contentDescription = "Read Receipt",
                            tint = receiptColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (!message.isDeleted) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reply",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clickable { onReply() }
                                .padding(horizontal = 4.dp)
                        )
                        TextWithNotoImageEmoji(
                            "\uD83D\uDE00",
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    val wasOpen = isEmojiPickerOpen
                                    onToggleEmojiPicker()
                                    if (!wasOpen) onEmojiPickerOpen()
                                }
                                .padding(horizontal = 4.dp)
                        )
                    }
                    if (isMe && !message.isDeleted) {
                        Spacer(Modifier.width(8.dp))
                        TextWithNotoImageEmoji(
                            "✏️",
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onEdit() }
                                .padding(horizontal = 2.dp)
                        )
                    }
                    if ((isMe || isAdmin) && !message.isDeleted) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onDelete() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Message",
                                tint = Color.Red.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (!isMe && !message.isDeleted) {
                        Spacer(Modifier.width(8.dp))
                        TextWithNotoImageEmoji(
                            "🚩",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onFlag() }
                                .padding(horizontal = 2.dp)
                        )
                    }
                    // Pin / Unpin button
                    if (!message.isDeleted) {
                        Spacer(Modifier.width(8.dp))
                        TextWithNotoImageEmoji(
                            "\uD83D\uDCCC",
                            color = if (message.pinned) Color(0xFFFF7F33) else Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable {
                                    if (message.pinned) onUnpin() else onPin()
                                }
                                .padding(horizontal = 2.dp)
                        )
                    }
                    // Forward button
                    if (!message.isDeleted) {
                        Spacer(Modifier.width(8.dp))
                        TextWithNotoImageEmoji(
                            "\u21AA\uFE0F",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onForward() }
                                .padding(horizontal = 2.dp)
                        )
                    }
                }

                // Clickable reply count to open thread
                if (message.replyCount > 0 && !message.isDeleted) {
                    Text(
                        text = "${message.replyCount} ${
                            if (message
                                    .replyCount == 1
                            ) "reply" else "replies"
                        }",
                        color = Color(0xFF4ea4e8),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { onOpenThread() }
                            .padding(start = 4.dp, top = 2.dp)
                    )
                }

                // Reaction picker — use full emoji suite
                if (isEmojiPickerOpen) {
                    EmojiPickerPanel(
                        onEmojiSelect = { emoji ->
                            // Map emoji to reaction type if it matches a known reaction,
                            // otherwise convert to a Stream-safe type (alphanumeric only)
                            val type = reactionDefs.find { it.emoji == emoji }?.type
                                ?: ("emoji_" + emoji.map { it.code.toString(16) }
                                    .joinToString("_"))
                            hapticVibrate(HapticDuration.TICK)
                            onReaction(type)
                            onToggleEmojiPicker()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Parses an ISO 8601 timestamp and returns a short time string (e.g., "2:30 PM").
 */
private fun formatMessageTime(isoString: String): String {
    return try {
        // ISO format: "2026-03-12T14:30:00.000Z"
        val timePart = isoString.substringAfter("T").substringBefore("Z")
            .substringBefore("+").substringBefore("-")
        val parts = timePart.split(":")
        if (parts.size >= 2) {
            val hour = parts[0].toIntOrNull() ?: return ""
            val minute = parts[1]
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            "$displayHour:$minute $amPm"
        } else ""
    } catch (e: Exception) {
        ""
    }
}

/**
 * Extracts the date portion (YYYY-MM-DD) from an ISO 8601 timestamp.
 */
private fun extractDatePart(isoString: String): String {
    return isoString.substringBefore("T")
}

// Top-level JS function to get today's date as YYYY-MM-DD
@JsFun("() => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0'); }")
private external fun jsTodayDate(): String

/**
 * Formats a date string (YYYY-MM-DD) as "Today", "Yesterday", or "Mon DD, YYYY".
 */
private fun formatDateLabel(dateStr: String): String {
    return try {
        val today = jsTodayDate()
        if (dateStr == today) return "Today"

        // Calculate yesterday
        val todayParts = today.split("-")
        if (todayParts.size == 3) {
            val y = todayParts[0].toInt()
            val m = todayParts[1].toInt()
            val d = todayParts[2].toInt()
            val yesterday = if (d > 1) {
                "${todayParts[0]}-${todayParts[1]}-${
                    (d - 1)
                        .toString().padStart(2, '0')
                }"
            } else {
                // Simplified: just compare the string from JS
                ""
            }
            if (dateStr == yesterday) return "Yesterday"
        }

        // Format as "Mon DD, YYYY"
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val month = parts[1].toIntOrNull() ?: return dateStr
            val day = parts[2].toIntOrNull() ?: return dateStr
            val monthNames = listOf(
                "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            "${monthNames.getOrElse(month) { "" }} $day, ${parts[0]}"
        } else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

/**
 * Parses the JSON reactions string into a map of type -> (count, isOwn).
 */
private fun parseReactions(json: String): Map<String, Pair<Int, Boolean>> {
    if (json.isEmpty() || json == "{}") return emptyMap()
    val result = mutableMapOf<String, Pair<Int, Boolean>>()
    val pattern = Regex(""""(\w+)":\{"count":(\d+),"own":(true|false)\}""")
    for (match in pattern.findAll(json)) {
        val type = match.groupValues[1]
        val count = match.groupValues[2].toInt()
        val own = match.groupValues[3].toBoolean()
        result[type] = Pair(count, own)
    }
    return result
}

private data class ReactionDef(val type: String, val emoji: String)

private val reactionDefs = listOf(
    ReactionDef("like", "\uD83D\uDC4D"),        // 👍
    ReactionDef("love", "\u2764\uFE0F"),          // ❤️
    ReactionDef("haha", "\uD83D\uDE02"),          // 😂
    ReactionDef("fire", "\uD83D\uDD25"),          // 🔥
    ReactionDef("tada", "\uD83C\uDF89"),          // 🎉
    ReactionDef("wow", "\uD83D\uDE2E"),           // 😮
    ReactionDef("sad", "\uD83D\uDE22"),           // 😢
    ReactionDef("clap", "\uD83D\uDC4F"),          // 👏
    ReactionDef("hundred", "\uD83D\uDCAF"),       // 💯
    ReactionDef("eyes", "\uD83D\uDC40"),          // 👀
    ReactionDef("mindblown", "\uD83E\uDD2F"),     // 🤯
    ReactionDef("cheers", "\uD83C\uDF7B"),        // 🍻
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReactionPicker(onSelect: (String) -> Unit) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        FlowRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            reactionDefs.forEach { def ->
                Surface(
                    onClick = {
                        hapticVibrate(HapticDuration.TICK)
                        onSelect(def.type)
                    },
                    color = Color(0xFF2C2C2C),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    TextWithNotoImageEmoji(
                        def.emoji,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReplyBanner(
    replyingTo: JsChatMessage,
    onCancel: () -> Unit
) {
    Surface(
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF222222), Color(0xFF1A1A1A))
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .background(Color(0xFFFF7F33), RoundedCornerShape(1.5.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = Color(0xFFFF7F33),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Replying to ${replyingTo.userName}",
                            color = Color(0xFFFF7F33),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        replyingTo.text.take(60) +
                                if (replyingTo.text.length > 60) "\u2026" else "",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel reply",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageInput(
    replyingToUser: String?,
    editingMessage: JsChatMessage?,
    onSendMessage: (String, org.w3c.files.File?) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onCancelEditing: () -> Unit,
    onTyping: () -> Unit,
    isEmojiPickerOpen: Boolean,
    onToggleEmojiPicker: () -> Unit,
    onClosePickers: () -> Unit,
    isGifPanelOpen: Boolean = false,
    onToggleGifPanel: () -> Unit = {}
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedFile by remember { mutableStateOf<org.w3c.files.File?>(null) }

    // When entering edit mode, pre-fill the text field
    LaunchedEffect(editingMessage?.id) {
        if (editingMessage != null) {
            textFieldValue = TextFieldValue(
                text = editingMessage.text,
                selection = TextRange(editingMessage.text.length)
            )
        }
    }

    val isEditing = editingMessage != null
    val placeholderText = when {
        isEditing -> "Edit message..."
        replyingToUser != null -> "Reply to $replyingToUser..."
        else -> "Write a message..."
    }

    Surface(
        color = Color(0xFF121212),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            // Editing banner
            if (isEditing && editingMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A2E))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .background(Color(0xFF4ea4e8), RoundedCornerShape(1.5.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextWithNotoImageEmoji(
                                "✏️",
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Editing message",
                                color = Color(0xFF4ea4e8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            editingMessage.text.take(60) +
                                    if (editingMessage.text.length > 60) "\u2026" else "",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = {
                            onCancelEditing()
                            textFieldValue = TextFieldValue("")
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel editing",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (selectedFile != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "Image",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedFile?.name ?: "Attached image",
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { selectedFile = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Emoji picker panel (above the input row)
            AnimatedVisibility(visible = isEmojiPickerOpen) {
                EmojiPickerPanel(
                    onEmojiSelect = { emoji ->
                        val cursorPos = textFieldValue.selection.start
                        val before = textFieldValue.text.substring(0, cursorPos)
                        val after = textFieldValue.text.substring(cursorPos)
                        val newText = before + emoji + after
                        val newCursorPos = cursorPos + emoji.length
                        textFieldValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursorPos)
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        openImagePicker { file ->
                            selectedFile = file
                        }
                    }
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "Attach file",
                        tint = if (selectedFile != null) Color(0xFFFF7F33) else Color.Gray
                    )
                }
                // GIF button
                Surface(
                    color = if (isGifPanelOpen) Color(0xFFFF7F33) else Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .clickable { onToggleGifPanel() }
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        "GIF",
                        color = if (isGifPanelOpen) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                TextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        if (newValue.text.isNotEmpty()) {
                            onTyping()
                        }
                    },
                    placeholder = { Text(placeholderText, color = Color.Gray) },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent { event ->
                            if (event.key.keyCode == Key.Enter.keyCode) {
                                if (event.isShiftPressed) {
                                    val cursorPos = textFieldValue.selection.start
                                    val before = textFieldValue.text.substring(0, cursorPos)
                                    val after = textFieldValue.text.substring(cursorPos)
                                    textFieldValue = TextFieldValue(
                                        text = before + "\n" + after,
                                        selection = TextRange(cursorPos + 1)
                                    )
                                    true
                                } else {
                                    if (textFieldValue.text.isNotBlank() || selectedFile != null) {
                                        if (isEditing && editingMessage != null) {
                                            onEditMessage(editingMessage.id, textFieldValue.text)
                                        } else {
                                            hapticVibrate(HapticDuration.LIGHT)
                                            onSendMessage(textFieldValue.text, selectedFile)
                                        }
                                        textFieldValue = TextFieldValue("")
                                        selectedFile = null
                                    }
                                    true
                                }
                            } else {
                                false
                            }
                        },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFFFF7F33),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        selectionColors = TextSelectionColors(
                            handleColor = Color(0xFFFF7F33),
                            backgroundColor = Color(0xFF5599DD).copy(alpha = 0.4f)
                        )
                    ),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                // Emoji picker toggle button
                IconButton(
                    onClick = { onToggleEmojiPicker() }
                ) {
                    TextWithNotoImageEmoji(
                        "😊",
                        fontSize = 20.sp
                    )
                }

                IconButton(
                    onClick = {
                        if (textFieldValue.text.isNotBlank() || selectedFile != null) {
                            if (isEditing && editingMessage != null) {
                                onEditMessage(editingMessage.id, textFieldValue.text)
                            } else {
                                hapticVibrate(HapticDuration.LIGHT)
                                onSendMessage(textFieldValue.text, selectedFile)
                            }
                            textFieldValue = TextFieldValue("")
                            selectedFile = null
                            onClosePickers()
                        }
                    },
                    enabled = textFieldValue.text.isNotBlank() || selectedFile != null
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Done else Icons.Default.Send,
                        contentDescription = if (isEditing) "Save" else "Send",
                        tint = if (textFieldValue.text.isNotBlank() || selectedFile != null) {
                            if (isEditing) Color(0xFF4ea4e8) else Color(0xFFFF7F33)
                        } else Color.Gray
                    )
                }
            }

            // Emoji preview: show rendered emojis when text contains emoji chars
            val hasEmoji = textFieldValue.text.any { char ->
                val cp = char.code
                cp > 0x1F00  // Rough heuristic: characters beyond Latin/ASCII are likely emoji or special chars
            }
            if (hasEmoji && textFieldValue.text.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Preview: ", color = Color.Gray, fontSize = 11.sp)
                    TextWithNotoImageEmoji(
                        textFieldValue.text,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiPickerPanel(onEmojiSelect: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    val emojiCategories = listOf(
        "Smileys & People" to listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "😊",
            "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😚",
            "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🫢", "🤫",
            "🤔", "🫡", "🤐", "🤨", "😐", "😑", "😶", "🫥", "😏", "😒",
            "🙄", "😬", "🤥", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕",
            "🤢", "🤮", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "🥸",
            "😎", "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳",
            "🥺", "🥹", "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣",
            "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠", "🤬", "😈",
            "👿", "💀", "💩", "🤡", "👹", "👺", "👻", "👽", "👾", "🤖"
        ),
        "Gestures & Body" to listOf(
            "👍", "👎", "👊", "✊", "🤛", "🤜", "👏", "🙌", "🫶", "🤝",
            "✌️", "🤞", "🤟", "🤘", "🤙", "👋", "🤚", "✋", "🖖", "👌",
            "🤌", "💪", "🦾", "🖕", "🫰", "🫵", "👈", "👉", "👆", "👇",
            "☝️", "🫳", "🫴", "🤏", "🤜", "🤛", "✍️", "🙏", "💅", "🤳"
        ),
        "Animals & Nature" to listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
            "🦁", "🐮", "🐷", "🐸", "🐵", "🙈", "🙉", "🙊", "🐔", "🐧",
            "🐦", "🦆", "🦅", "🦉", "🦇", "🐺", "🐴", "🦄", "🐝", "🦋",
            "🐌", "🐞", "🐜", "🌸", "💐", "🌹", "🌺", "🌻", "🌼", "🌷",
            "🌱", "🪴", "🌿", "🍀", "🍁", "🍂", "🍃", "🍄", "🌰"
        ),
        "Food & Drink" to listOf(
            "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈",
            "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🥑", "🥦", "🌽",
            "🌶️", "🥒", "🥕", "🍕", "🍔", "🍟", "🌭", "🍿", "🍳", "🥞",
            "🍞", "🧀", "🥗", "🌮", "🌯", "🍜", "🍝", "🍣", "🍱", "🍛",
            "☕", "🍵", "🧃", "🥤", "🍶", "🍺", "🍻", "🥂", "🍷", "🥃",
            "🍸", "🍹", "🍾", "🍰", "🎂", "🍩", "🍪", "🍫", "🍬", "🍭"
        ),
        "Activities & Travel" to listOf(
            "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🎱", "🏓", "🏸", "🥊",
            "🎮", "🕹️", "🎰", "🎲", "🧩", "🎭", "🎨", "🎤", "🎧", "🎵",
            "🚗", "🚕", "🚙", "🏎️", "🚑", "🚒", "✈️", "🚀", "🛸", "🚁",
            "🏠", "🏢", "🏥", "🏨", "🏪", "🗼", "🗽", "🏰", "⛪", "🗿"
        ),
        "Hearts & Love" to listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
            "❤️‍🔥", "❤️‍🩹", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟"
        ),
        "Symbols & Objects" to listOf(
            "✅", "❌", "⭕", "❗", "❓", "⚡", "💡", "🔥", "⭐", "✨",
            "🌟", "💫", "🎉", "🎊", "🎈", "🎁", "🏆", "🥇", "🥈", "🥉",
            "🎯", "💬", "👀", "💯", "💢", "💥", "💤", "🚀", "📌", "🔔",
            "🔒", "🔑", "🔎", "📎", "✏️", "📝", "📅", "📊", "⏰", "🗓️",
            "🏁", "🚩", "🎌", "🏳️‍🌈"
        )
    )

    val filteredCategories = if (searchQuery.isBlank()) {
        emojiCategories
    } else {
        emojiCategories.filter { (category, _) ->
            category.contains(searchQuery, ignoreCase = true)
        }
    }

    // Skin tone modifiers
    val skinTones = listOf(
        "" to Color(0xFFFFD93D),      // Default yellow
        "\uD83C\uDFFB" to Color(0xFFF5D0A9),  // Light
        "\uD83C\uDFFC" to Color(0xFFE4B88A),  // Medium-Light
        "\uD83C\uDFFD" to Color(0xFFC99A6B),  // Medium
        "\uD83C\uDFFE" to Color(0xFFA57145),  // Medium-Dark
        "\uD83C\uDFFF" to Color(0xFF6B4226)   // Dark
    )
    var selectedSkinTone by remember { mutableStateOf("") }

    // Emojis that support skin tones
    val skinToneEmojis = setOf(
        "👍", "👎", "👊", "✊", "🤛", "🤜", "👏", "🙌", "🤝",
        "✌️", "🤞", "🤟", "🤘", "🤙", "👋", "🤚", "✋", "🖖", "👌",
        "🤌", "💪", "🖕", "🫰", "🫵", "👈", "👉", "👆", "👇",
        "☝️", "🫳", "🫴", "🤏", "✍️", "🙏", "💅", "🤳"
    )

    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Skin tone selector row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Skin:", color = Color.Gray, fontSize = 10.sp)
                skinTones.forEach { (modifier, color) ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selectedSkinTone == modifier)
                                    Modifier.padding(1.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                else Modifier
                            )
                            .clickable { selectedSkinTone = modifier }
                            .then(
                                if (selectedSkinTone == modifier)
                                    Modifier.padding(2.dp)
                                else Modifier
                            )
                    ) {
                        if (selectedSkinTone == modifier) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .padding(1.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
            // Category search
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search category...", color = Color.Gray, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2C2C2C),
                    unfocusedContainerColor = Color(0xFF2C2C2C),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            Spacer(Modifier.height(4.dp))

            LazyColumn {
                filteredCategories.forEach { (category, emojis) ->
                    item {
                        Text(
                            category,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            emojis.forEach { emoji ->
                                val displayEmoji =
                                    if (selectedSkinTone.isNotEmpty() && emoji in skinToneEmojis) {
                                        emoji + selectedSkinTone
                                    } else emoji
                                Surface(
                                    onClick = { onEmojiSelect(displayEmoji) },
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        TextWithNotoImageEmoji(displayEmoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openImagePicker(onImagePicked: (org.w3c.files.File?) -> Unit) {
    val input = kotlinx.browser.document.createElement("input") as
            org.w3c.dom.HTMLInputElement
    input.apply {
        type = "file"
        accept = "*/*"
        onchange = {
            val file = files?.item(0)
            onImagePicked(file)
            null
        }
    }
    input.click()
}

@Composable
fun FileAttachmentCard(attachment: ChatAttachment, modifier: Modifier = Modifier) {
    val ext = attachment.title.substringAfterLast(
        '.', "").uppercase()
    val accentColor = when {
        ext == "PDF" -> Color(0xFFE53935)
        ext in listOf("DOC", "DOCX") -> Color(0xFF2196F3)
        ext in listOf("XLS", "XLSX", "CSV") -> Color(0xFF4CAF50)
        ext in listOf("PPT", "PPTX") -> Color(0xFFFF9800)
        ext in listOf("ZIP", "RAR", "7Z", "TAR", "GZ") -> Color(0xFF9C27B0)
        else -> Color(0xFF78909C)
    }

    Surface(
        color = Color(0xFF1E1E30),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .widthIn(max = 280.dp)
            .padding(vertical = 2.dp)
            .clickable {
                if (attachment.fileUrl.isNotEmpty()) {
                    kotlinx.browser.window.open(attachment.fileUrl, "_blank")
                }
            }
    ) {
        Row(
            modifier = Modifier
        ) {
            // Colored accent bar on the left
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(accentColor)
            )
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // File extension badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (ext.length <= 4) ext else ext.take(3),
                        color = accentColor,
                        fontSize = if (ext.length <= 3) 11.sp else 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.title.ifEmpty { "File" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (attachment.fileSize > 0) {
                            Text(
                                text = formatFileSize(attachment.fileSize),
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = if (ext.isNotEmpty()) "• $ext file" else "• File",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
                // Download icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    TextWithNotoImageEmoji(
                        "\u2B07\uFE0F",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun getFileIcon(mimeType: String, fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when {
        mimeType.contains("pdf") || ext == "pdf" -> "\uD83D\uDCC4"       // 📄
        mimeType.contains("word") || ext in listOf("doc", "docx") -> "\uD83D\uDCC3"  // 📃
        mimeType.contains("sheet") || mimeType.contains("excel") || ext in listOf(
            "xls",
            "xlsx",
            "csv"
        ) -> "\uD83D\uDCCA" // 📊
        mimeType.contains("presentation") || mimeType.contains("powerpoint") ||
                ext in listOf(
            "ppt",
            "pptx"
        ) -> "\uD83D\uDCCA"

        mimeType.contains("zip") || mimeType.contains("archive") || ext in listOf(
            "zip",
            "rar",
            "7z",
            "tar",
            "gz"
        ) -> "\uD83D\uDDC4\uFE0F"

        mimeType.contains("text") || ext == "txt" -> "\uD83D\uDCC4"
        mimeType.contains("audio") -> "\uD83C\uDFB5"                      // 🎵
        mimeType.contains("video") -> "\uD83C\uDFA5"                      // 🎥
        else -> "\uD83D\uDCCE"                                             // 📎
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> {
            val mb = (bytes * 10 / (1024 * 1024)).toDouble() / 10
            "$mb MB"
        }

        else -> {
            val gb = (bytes * 10 / (1024 * 1024 * 1024)).toDouble() / 10
            "$gb GB"
        }
    }
}

@Composable
fun LinkPreviewCard(attachment: ChatAttachment, modifier: Modifier = Modifier) {
    // Extract domain from URL
    val domain = remember(attachment.titleLink) {
        try {
            val url = attachment.titleLink.ifEmpty { attachment.thumbUrl }
            url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
                .substringBefore("/").substringBefore("?")
        } catch (_: Exception) {
            ""
        }
    }

    Surface(
        color = Color(0xFF1E1E30),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .widthIn(max = 300.dp)
            .padding(vertical = 4.dp)
            .clickable {
                if (attachment.titleLink.isNotEmpty()) {
                    kotlinx.browser.window.open(attachment.titleLink, "_blank")
                }
            }
    ) {
        Row {
            // Blue accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (attachment.thumbUrl.isNotEmpty() ||
                        attachment.imageUrl.isNotEmpty()) 180.dp else 70.dp)
                    .background(Color(0xFF4ea4e8))
            )
            Column {
                // OG Image
                val imageSource = attachment.thumbUrl.ifEmpty { attachment.imageUrl }
                if (imageSource.isNotEmpty()) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = attachment.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(topEnd = 10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    // Domain with link icon
                    if (domain.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextWithNotoImageEmoji(
                                "\uD83D\uDD17",
                                fontSize = 10.sp
                            )
                            Text(
                                text = domain,
                                color = Color(0xFF4ea4e8),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    // Title
                    if (attachment.title.isNotEmpty()) {
                        Text(
                            text = attachment.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Description
                    if (attachment.text.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = attachment.text,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun parseAttachments(jsonString: String): List<ChatAttachment> {
    if (jsonString.isBlank() || jsonString == "[]") return emptyList()

    val attachments = mutableListOf<ChatAttachment>()

    try {
        val objects = jsonString.split("},{", "}, {")
        for (obj in objects) {
            val type = Regex(""""type"\s*:\s*"([^"]+)"""")
                .find(obj)?.groupValues?.get(1) ?: ""
            val title = Regex(""""title"\s*:\s*"([^"]+)"""")
                .find(obj)?.groupValues?.get(1) ?: ""
            val text = Regex(""""text"\s*:\s*"([^"]+)"""")
                .find(obj)?.groupValues?.get(1) ?: ""
            val titleLink =
                Regex(""""title_link"\s*:\s*"([^"]+)"""")
                    .find(obj)?.groupValues?.get(1) ?: Regex(
                    """"og_scrape_url"\s*:\s*"([^"]+)""""
                ).find(obj)?.groupValues?.get(1) ?: ""
            val thumbUrl =
                Regex(""""thumb_url"\s*:\s*"([^"]+)"""")
                    .find(obj)?.groupValues?.get(1) ?: ""
            val imageUrl =
                Regex(""""image_url"\s*:\s*"([^"]+)"""")
                    .find(obj)?.groupValues?.get(1) ?: ""
            val assetUrl =
                Regex(""""asset_url"\s*:\s*"([^"]+)"""")
                    .find(obj)?.groupValues?.get(1) ?: ""
            val fileSize =
                Regex(""""file_size"\s*:\s*(\d+)""")
                    .find(obj)?.groupValues?.get(1)?.toLongOrNull()
                    ?: 0L
            val mimeType =
                Regex(""""mime_type"\s*:\s*"([^"]+)"""")
                    .find(obj)?.groupValues?.get(1) ?: ""

            if (type.isNotEmpty() || title.isNotEmpty()) {
                attachments.add(
                    ChatAttachment(
                        type = type,
                        title = title,
                        text = text,
                        titleLink = titleLink,
                        thumbUrl = thumbUrl,
                        imageUrl = imageUrl.ifEmpty { if (type != "file") assetUrl else "" },
                        fileUrl = if (type == "file") assetUrl else "",
                        fileSize = fileSize,
                        mimeType = mimeType
                    )
                )
            }
        }
    } catch (e: Exception) {
        println("Error parsing attachments JSON: ${e.message}")
    }

    return attachments
}

@Composable
fun NewChatDialog(
    searchResults: List<JsStreamUser>,
    onSearch: (String) -> Unit,
    onSelectUser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        onSearch("")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth().height(400.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "New Chat",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearch(it)
                    },
                    placeholder = { Text("Search users...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2C),
                        unfocusedContainerColor = Color(0xFF2C2C2C),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (query.isBlank()) {
                            CircularProgressIndicator(color = Color(0xFFFF7F33))
                        } else {
                            Text("No users found", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn {
                        items(searchResults) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUser(user.id) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar with profile picture or fallback initial
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF7F33)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (user.image.isNotEmpty()) {
                                        AsyncImage(
                                            model = user.image,
                                            contentDescription = "${user.name} avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    user.name,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadPanel(
    parentMessage: JsChatMessage,
    replies: List<JsChatMessage>,
    currentUserId: String?,
    onClose: () -> Unit,
    onSendReply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var replyText by remember { mutableStateOf("") }

    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            TopAppBar(
                title = { Text("Thread", color = Color.White, fontSize = 16.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close thread",
                            tint = Color.White
                        )
                    }
                }
            )

            // Parent message preview
            Surface(
                color = Color(0xFF1E1E1E),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        parentMessage.userName,
                        color = Color(0xFFFF7F33),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        parentMessage.text,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatMessageTime(parentMessage.createdAt),
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${replies.size} ${if (replies.size == 1) "reply" else "replies"}",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Replies list
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(replies) { reply ->
                    val isMe = reply.userId == currentUserId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isMe) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF555555)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (reply.userImage.isNotEmpty()) {
                                    AsyncImage(
                                        model = reply.userImage,
                                        contentDescription = reply.userName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        reply.userName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Column(
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            if (!isMe) {
                                Text(reply.userName, color = Color.Gray, fontSize = 10.sp)
                            }
                            Surface(
                                color = if (isMe) Color(0xFF1A3A5C) else Color(0xFF2C2C2C),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    reply.text,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Text(
                                formatMessageTime(reply.createdAt),
                                color = Color.Gray,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            }

            // Reply input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = {
                        Text(
                            "Reply in thread...",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 36.dp, max = 100.dp)
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter
                                && !event.isShiftPressed && replyText.isNotBlank())
                            {
                                hapticVibrate(HapticDuration.LIGHT)
                                onSendReply(replyText)
                                replyText = ""
                                true
                            } else false
                        },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2C),
                        unfocusedContainerColor = Color(0xFF2C2C2C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFF7F33),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(fontSize = 13.sp),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = false
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            hapticVibrate(HapticDuration.LIGHT)
                            onSendReply(replyText)
                            replyText = ""
                        }
                    },
                    enabled = replyText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send reply",
                        tint = if (replyText.isNotBlank()) Color(0xFFFF7F33) else Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * GIF Search Panel using Tenor API
 */
@Composable
fun GifSearchPanel(
    onGifSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var gifResults by remember {
        mutableStateOf<List<Triple<String, String, String>>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "GIF Search",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                        .clickable { onDismiss() }
                )
            }
            Spacer(Modifier.height(8.dp))
            TextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    if (query.length >= 2) {
                        isLoading = true
                        searchGiphy(query) { results ->
                            gifResults = results
                            isLoading = false
                        }
                    }
                },
                placeholder = { Text("Search GIFs...", color = Color.Gray) },
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2C2C2C),
                    unfocusedContainerColor = Color(0xFF2C2C2C),
                    cursorColor = Color(0xFFFF7F33),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF7F33),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (gifResults.isEmpty() && searchQuery.length >= 2) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No GIFs found", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                // 3-column compact grid
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val cols = 3
                    items((gifResults.size + cols - 1) / cols) { rowIndex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until cols) {
                                val idx = rowIndex * cols + col
                                if (idx < gifResults.size) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF2A2A3A))
                                            .clickable {
                                                val (_, fullUrl, title) = gifResults[idx]
                                                onGifSelected(fullUrl, title)
                                            }
                                    ) {
                                        AsyncImage(
                                            model = gifResults[idx].first,
                                            contentDescription = gifResults[idx].third,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@JsFun("(query, callback) => { fetch('https://tenor.googleapis.com/v2/search?q=' + encodeURIComponent(query) + '&key=REDACTED_TENOR_API_KEY&client_key=wdw_web&limit=20').then(r => r.json()).then(data => { const results = (data.results || []).map(g => [g.media_formats.tinygif.url, g.media_formats.gif.url, g.content_description || g.title || 'GIF']); callback(results); }).catch(e => { console.error('GIF search error:', e); callback([]); }); }")
private external fun searchGiphyJs(query: String, callback: (JsArray<JsArray<JsString>>) -> Unit)

private fun searchGiphy(query: String, onResults: (List<Triple<String, String, String>>) -> Unit) {
    searchGiphyJs(query) { jsResults ->
        val results = mutableListOf<Triple<String, String, String>>()
        for (i in 0 until jsResults.length) {
            val item = jsResults[i]
            if (item != null && item.length >= 3) {
                val previewUrl = item[0]?.toString() ?: ""
                val fullUrl = item[1]?.toString() ?: ""
                val title = item[2]?.toString() ?: ""
                if (previewUrl.isNotEmpty()) results.add(
                    Triple(
                        previewUrl,
                        fullUrl.ifEmpty { previewUrl },
                        title
                    )
                )
            }
        }
        onResults(results)
    }
}

/**
 * Forward Message Dialog — choose a channel to forward to
 */
@Composable
fun UserProfilePopover(
    userName: String,
    userImage: String,
    isOnline: Boolean,
    userId: String,
    isSelf: Boolean = false,
    isBlocked: Boolean = false,
    onDismiss: () -> Unit,
    onBlock: () -> Unit = {},
    onUnblock: () -> Unit = {},
    onReportClick: () -> Unit = {}
) {
    // Look up member profile from the members list
    val members = LocalMembers.current
    val member = remember(userName, members) {
        members.filterNotNull().firstOrNull {
            it.name.equals(userName, ignoreCase = true)
        }
    }
    val profileImage = member?.profilePictureUrl?.takeIf { it.isNotEmpty() } ?: userImage

    var showConfirmBlock by remember { mutableStateOf(false) }

    // Block confirmation dialog
    if (showConfirmBlock) {
        Dialog(onDismissRequest = { showConfirmBlock = false }) {
            Surface(
                color = Color(0xFF1E1E30),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        if (isBlocked) "Unblock $userName?" else "Block $userName?",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isBlocked)
                            "You will see their messages again and they will appear in the member" +
                                    "directory."
                        else
                            "Their messages will be hidden and they won't appear in your member" +
                                    "directory.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color(0xFF2C2C2C),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { showConfirmBlock = false }
                        ) {
                            Text(
                                "Cancel",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = if (isBlocked) Color(0xFF4ea4e8) else Color(0xFFe54545),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                showConfirmBlock = false
                                if (isBlocked) onUnblock() else onBlock()
                                onDismiss()
                            }
                        ) {
                            Text(
                                if (isBlocked) "Unblock" else "Block",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }



    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF1E1E30),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(modifier = Modifier.size(72.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF555555)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImage.isNotEmpty()) {
                            AsyncImage(
                                model = profileImage,
                                contentDescription = "$userName avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = userName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Online dot
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF1E1E30), CircleShape)
                            .padding(2.dp)
                            .background(
                                if (isOnline) Color(0xFF44b700) else Color.Gray,
                                CircleShape
                            )
                    )
                }
                Spacer(Modifier.height(12.dp))
                // Username
                Text(
                    text = member?.name ?: userName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                // Role badge
                if (member != null) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = when (member.memberType) {
                            net.winedownwednesday.web.data.MembershipType.LEADER -> Color(0xFFFF7F33)
                            net.winedownwednesday.web.data.MembershipType.MEMBER -> Color(0xFF4ea4e8)
                            net.winedownwednesday.web.data.MembershipType.GUEST -> Color(0xFF666666)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = member.memberType.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                // Online status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isOnline) Color(0xFF44b700) else Color.Gray,
                                CircleShape
                            )
                    )
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        color = if (isOnline) Color(0xFF44b700) else Color.Gray,
                        fontSize = 12.sp
                    )
                }
                // Member profile details
                if (member != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(Color(0xFF333333))
                    )
                    Spacer(Modifier.height(8.dp))
                    // Profession & Company
                    if (member.profession.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextWithNotoImageEmoji("\uD83D\uDCBC", fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (member.company.isNotEmpty())
                                    "${member.profession} at ${member.company}"
                                else member.profession,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    // Interests
                    if (member.interests.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextWithNotoImageEmoji("\u2B50", fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = member.interests.take(3).joinToString(", "),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    // Favorite wines
                    if (member.favoriteWines.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextWithNotoImageEmoji("\uD83C\uDF77", fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = member.favoriteWines.take(3).joinToString(", "),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ─── Moderation actions (not shown for self) ────────────────
                if (!isSelf) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(Color(0xFF333333))
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Block / Unblock
                        Surface(
                            color = if (isBlocked) Color(0xFF3a3a3a) else Color(0xFFe54545).copy(
                                alpha = 0.15f
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).clickable { showConfirmBlock = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = if (isBlocked) "Unblock" else "Block",
                                    tint = if (isBlocked) Color.White else Color(0xFFe54545),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isBlocked) "Unblock" else "Block",
                                    color = if (isBlocked) Color.White else Color(0xFFe54545),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFFFF7F33).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).clickable { onReportClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = "Report",
                                    tint = Color(0xFFFF7F33),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Report",
                                    color = Color(0xFFFF7F33),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                // Close button
                Surface(
                    color = Color(0xFF2C2C2C),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                ) {
                    Text(
                        "Close",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ReportUserDialog(
    userName: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String?, category: String) -> Unit
) {
    val categories = listOf("SPAM", "HARASSMENT", "INAPPROPRIATE", "OTHER")
    val categoryLabels = listOf("Spam", "Harassment", "Inappropriate Content", "Other")
    var selectedCategory by remember { mutableStateOf("OTHER") }
    var reason by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF1E1E30),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Report $userName",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Select a reason for reporting this user.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(16.dp))

                // Category chips
                categories.forEachIndexed { index, cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        color = if (isSelected) Color(0xFFFF7F33).copy(alpha = 0.2f) else
                            Color(0xFF2C2C2C),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { selectedCategory = cat }
                            .then(
                                if (isSelected) Modifier.border(
                                    1.dp,
                                    Color(0xFFFF7F33),
                                    RoundedCornerShape(
                                        8.dp
                                    )
                                ) else Modifier
                            )
                    ) {
                        Text(
                            text = categoryLabels[index],
                            color = if (isSelected) Color(0xFFFF7F33) else
                                Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Optional reason
                Text(
                    "Additional details (optional)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = {
                        Text(
                            "Describe the issue...", color = Color
                                .White.copy(alpha = 0.3f), fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF7F33),
                        unfocusedBorderColor = Color(0xFF444444),
                        cursorColor = Color(0xFFFF7F33)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    maxLines = 3
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color(0xFF2C2C2C),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(
                            "Cancel",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFFF7F33),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            onSubmit(
                                reason.takeIf { it.isNotBlank() },
                                selectedCategory
                            )
                        }
                    ) {
                        Text(
                            "Submit Report",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageLightboxDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full-size image",
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f)
                    .clickable { /* prevent dismiss when clicking the image */ },
                contentScale = ContentScale.Fit
            )
            // Close button
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clickable { onDismiss() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ForwardMessageDialog(
    message: JsChatMessage,
    channels: List<JsChatChannel>,
    onForward: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredChannels = channels.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF1E1E2E),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 360.dp).heightIn(max = 400.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Forward Message",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                // Preview of message being forwarded
                Surface(
                    color = Color(0xFF2A2A3A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.text
                            .take(100) + if (message.text.length > 100) "..." else "",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search channels...", color = Color.Gray) },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2C),
                        unfocusedContainerColor = Color(0xFF2C2C2C),
                        cursorColor = Color(0xFFFF7F33),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(filteredChannels) { channel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onForward(message.text, channel.id) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (channel.image.isNotEmpty()) {
                                AsyncImage(
                                    model = channel.image,
                                    contentDescription = channel.name,
                                    modifier = Modifier.size(32.dp).clip(
                                        CircleShape
                                    ),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                channel.name,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Channel Info Panel — shows members and channel details
 */
@Composable
fun ChannelInfoPanel(
    channelName: String,
    members: List<JsChannelMember>,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color(0xFF1A1A2E),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        channelName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "${members.size} members",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp).clickable { onDismiss() }
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(members.size) { index ->
                    val member = members[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            if (member.userImage.isNotEmpty()) {
                                AsyncImage(
                                    model = member.userImage,
                                    contentDescription = member.userName,
                                    modifier = Modifier.size(28.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3A3A5A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        member.userName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            // Online indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (member.online) Color(0xFF4CAF50) else
                                            Color.Gray
                                    )
                                    .align(Alignment.BottomEnd)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            member.userName,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (member.role == "owner" || member.role == "admin") {
                            Surface(
                                color = Color(0xFFFF7F33).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    member.role,
                                    color = Color(0xFFFF7F33),
                                    fontSize = 9.sp,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Chat Settings Dialog ────────────────────────────────────────────────────

@Composable
fun ChatSettingsDialog(
    blockedUsers: List<JsStreamUser>,
    onUnblock: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        modifier = Modifier.widthIn(min = 340.dp, max = 480.dp),
        title = {
            Text(
                "Blocked Users",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            BlockedUsersSection(
                blockedUsers = blockedUsers,
                onUnblock = onUnblock
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFFFF7F33))
            }
        }
    )
}

@Composable
private fun BlockedUsersSection(
    blockedUsers: List<JsStreamUser>,
    onUnblock: (String) -> Unit
) {
    if (blockedUsers.isEmpty()) {
        Text(
            "No blocked users",
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
                .verticalScroll(rememberScrollState())
        ) {
            blockedUsers.forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF7F33)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.image.isNotEmpty()) {
                            AsyncImage(
                                model = user.image,
                                contentDescription = "${user.name} avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = user.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        user.name,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { onUnblock(user.id) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF7F33)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF7F33)),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp, vertical = 4.dp
                        )
                    ) {
                        Text("Unblock", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
