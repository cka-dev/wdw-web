package net.winedownwednesday.web.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.BlockedUserInfo

enum class SettingsCategory(val label: String, val icon: ImageVector) {
    SECURITY("Security", Icons.Default.Lock),
    PRIVACY("Privacy & Moderation", Icons.Default.Shield),
    DANGER_ZONE("Danger Zone", Icons.Default.DeleteForever)
}

@Composable
fun SettingsPage(
    isCompactScreen: Boolean,
    viewModel: AuthPageViewModel,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.profileData.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val isUnblocking by viewModel.isUnblocking.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf(SettingsCategory.SECURITY) }

    val isDark = LocalIsDarkTheme.current
    val backgroundBrush = if (!isDark) Brush.verticalGradient(
        colors = listOf(Color(0xFFFFE08A), Color(0xFFD4AF37))
    ) else null

    LaunchedEffect(Unit) {
        viewModel.fetchBlockedUsers()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { m ->
                    if (backgroundBrush != null) m.background(brush = backgroundBrush)
                    else m.background(MaterialTheme.colorScheme.background)
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else LightOnPrimaryContainer
                        )
                    }
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isDark) MaterialTheme.colorScheme.onBackground
                            else LightOnPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (isCompactScreen) {
                    // Compact: stacked cards
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            SecuritySection(
                                hasPassword = userProfile?.hasPassword ?: false,
                                hasPasskey = userProfile?.hasPasskey ?: false,
                                userEmail = userProfile?.email ?: "",
                                onRegisterPasskey = { viewModel.registerPasskeyV2(it) },
                                onLinkPassword = { pw, cb -> viewModel.linkPasswordToAccount(pw, cb) },
                                onChangePassword = { cur, new, cb -> viewModel.changePassword(cur, new, cb) }
                            )
                        }
                        item {
                            PrivacySection(
                                blockedUsers = blockedUsers,
                                isUnblocking = isUnblocking,
                                onUnblock = { userId ->
                                    viewModel.unblockUser(userId) { }
                                }
                            )
                        }
                        item {
                            DeleteAccountSection(
                                viewModel = viewModel,
                                onLogout = onLogout
                            )
                        }
                    }
                } else {
                    // Wide: sidebar + detail pane
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .width(800.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Sidebar
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF343434)
                            ),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                SettingsCategory.entries.forEach { category ->
                                    val isSelected = category == selectedCategory
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) Color(0xFF4A4A4A)
                                                else Color.Transparent
                                            )
                                            .clickable { selectedCategory = category }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            category.icon,
                                            contentDescription = category.label,
                                            tint = if (category == SettingsCategory.DANGER_ZONE)
                                                Color(0xFFFF6B6B)
                                            else if (isSelected) Color(0xFFFF7F33)
                                            else Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            category.label,
                                            color = if (category == SettingsCategory.DANGER_ZONE)
                                                Color(0xFFFF6B6B)
                                            else if (isSelected) Color.White
                                            else Color.LightGray,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold
                                                else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Detail pane
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF343434)
                            ),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(
                                    modifier = Modifier.widthIn(max = 480.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    when (selectedCategory) {
                                        SettingsCategory.SECURITY -> SecuritySection(
                                            hasPassword = userProfile?.hasPassword ?: false,
                                            hasPasskey = userProfile?.hasPasskey ?: false,
                                            userEmail = userProfile?.email ?: "",
                                            onRegisterPasskey = { viewModel.registerPasskeyV2(it) },
                                            onLinkPassword = { pw, cb -> viewModel.linkPasswordToAccount(pw, cb) },
                                            onChangePassword = { cur, new, cb -> viewModel.changePassword(cur, new, cb) }
                                        )
                                        SettingsCategory.PRIVACY -> PrivacySection(
                                            blockedUsers = blockedUsers,
                                            isUnblocking = isUnblocking,
                                            onUnblock = { email ->
                                                viewModel.unblockUser(email) { }
                                            }
                                        )
                                        SettingsCategory.DANGER_ZONE -> DeleteAccountSection(
                                            viewModel = viewModel,
                                            onLogout = onLogout
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

// ─── Security Section ────────────────────────────────────────────────────────

@Composable
private fun SecuritySection(
    hasPassword: Boolean,
    hasPasskey: Boolean,
    userEmail: String,
    onRegisterPasskey: (String) -> Unit,
    onLinkPassword: (String, (Boolean) -> Unit) -> Unit,
    onChangePassword: (String, String, (Boolean) -> Unit) -> Unit
) {
    var showLinkPasswordDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Security",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(color = Color(0xFF444444))

            // Password status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Password", color = Color.White, modifier = Modifier.width(80.dp))
                    Icon(
                        if (hasPassword) Icons.Default.Check else Icons.Default.Cancel,
                        contentDescription = if (hasPassword) "Linked" else "Not linked",
                        tint = if (hasPassword) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (hasPassword) {
                    TextButton(onClick = { showChangePasswordDialog = true }) {
                        Text("Change", color = Color(0xFFFF7F33))
                    }
                } else {
                    TextButton(onClick = { showLinkPasswordDialog = true }) {
                        Text("Link Password", color = Color(0xFFFF7F33))
                    }
                }
            }

            // Passkey status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Passkey", color = Color.White, modifier = Modifier.width(80.dp))
                    Icon(
                        if (hasPasskey) Icons.Default.Check else Icons.Default.Cancel,
                        contentDescription = if (hasPasskey) "Registered" else "Not registered",
                        tint = if (hasPasskey) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (!hasPasskey) {
                    TextButton(
                        onClick = { onRegisterPasskey(userEmail) }
                    ) {
                        Text("Add Passkey", color = Color(0xFFFF7F33))
                    }
                }
            }
        }
    }

    // Link Password Dialog
    if (showLinkPasswordDialog) {
        LinkPasswordDialog(
            onLinkPassword = onLinkPassword,
            onDismiss = { showLinkPasswordDialog = false }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onChangePassword = onChangePassword,
            onDismiss = { showChangePasswordDialog = false }
        )
    }
}

@Composable
private fun LinkPasswordDialog(
    onLinkPassword: (String, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLinking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLinking) onDismiss() },
        title = { Text("Link Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set a password for your account to use as an alternative login method.")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isLinking,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isLinking,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password == confirmPassword && password.isNotEmpty()) {
                        isLinking = true
                        onLinkPassword(password) { success ->
                            isLinking = false
                            if (success) onDismiss()
                        }
                    }
                },
                enabled = !isLinking && password.isNotEmpty() && password == confirmPassword,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800020))
            ) {
                if (isLinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Link")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLinking) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onChangePassword: (String, String, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isChanging by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isChanging) onDismiss() },
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isChanging,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isChanging,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isChanging,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword == confirmNewPassword && newPassword.isNotEmpty()) {
                        isChanging = true
                        onChangePassword(currentPassword, newPassword) { success ->
                            isChanging = false
                            if (success) onDismiss()
                        }
                    }
                },
                enabled = !isChanging && newPassword.isNotEmpty()
                        && newPassword == confirmNewPassword,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800020))
            ) {
                if (isChanging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isChanging) {
                Text("Cancel")
            }
        }
    )
}

// ─── Privacy Section ─────────────────────────────────────────────────────────

@Composable
private fun PrivacySection(
    blockedUsers: List<BlockedUserInfo>,
    isUnblocking: Boolean,
    onUnblock: (String) -> Unit
) {
    var confirmUnblockTarget by remember { mutableStateOf<BlockedUserInfo?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Blocked Users",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (blockedUsers.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF5252), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${blockedUsers.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF444444))

            if (blockedUsers.isEmpty()) {
                Text(
                    "You haven't blocked anyone.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                blockedUsers.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF333333))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar fallback (initial letter)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF555555), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    user.name.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                user.name,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(
                            onClick = { confirmUnblockTarget = user },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF444444),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isUnblocking
                        ) {
                            if (isUnblocking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("Unblock", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog
    confirmUnblockTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!isUnblocking) confirmUnblockTarget = null },
            title = { Text("Unblock User") },
            text = { Text("Are you sure you want to unblock ${target.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onUnblock(target.id)
                        confirmUnblockTarget = null
                    },
                    enabled = !isUnblocking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7F33)
                    )
                ) {
                    Text("Unblock")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmUnblockTarget = null },
                    enabled = !isUnblocking
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
