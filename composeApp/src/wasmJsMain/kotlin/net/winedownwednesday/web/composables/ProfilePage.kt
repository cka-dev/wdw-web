package net.winedownwednesday.web.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.winedownwednesday.web.NanpVisualTransformation
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.formatPhoneNumber
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import wdw_web.composeapp.generated.resources.Res
import wdw_web.composeapp.generated.resources.placeholder
import androidx.compose.ui.unit.sp

@Composable
fun ProfilePage(
    isCompactScreen: Boolean,
    onLogout: () -> Unit,
    isNewUser: Boolean,
    userEmail: String,
    viewModel: AuthPageViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.profileData.collectAsStateWithLifecycle()
    val isProfileLoading by viewModel.isFetchingProfile.collectAsStateWithLifecycle()
    val isProfileSaving by viewModel.isSavingProfile.collectAsStateWithLifecycle()
    var editMode by remember { mutableStateOf(isNewUser) }
    var showSuccessToast by remember { mutableStateOf(false) }
    var showFailureToast by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showVerificationEmailToast by remember { mutableStateOf(false) }
    var showVerificationEmailFailureToast by remember { mutableStateOf(false) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE08A),
            Color(0xFFD4AF37)
        )
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (editMode)
                        "Update your personal information"
                    else
                        "View and edit your personal details",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF343434)
                    ),
                    modifier = if (isCompactScreen) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.width(450.dp)
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item(key = "profile_content_${userProfile?.hashCode()}") {
                            if (editMode) {
                                ProfileEditSection(
                                    profile = userProfile,
                                    editMode = editMode,
                                    onSave = { updatedProfile ->
                                        viewModel.saveProfile(updatedProfile)
                                        { success ->
                                            if (success) {
                                                editMode = false
                                                showSuccessToast = true
                                                showFailureToast = false
                                                viewModel.fetchProfile(userEmail)
                                            } else {
                                                showFailureToast = true
                                                showSuccessToast = false
                                            }
                                            coroutineScope.launch {
                                                delay(3500)
                                                showSuccessToast = false
                                                showFailureToast = false
                                            }
                                        }
                                    },

                                    onCancel = {
                                        editMode = false
                                        viewModel.fetchProfile(userEmail)
                                    },
                                    isNewUser = isNewUser,
                                    userEmail = userEmail,
                                    onEmailVerification = {
                                        viewModel.sendVerificationEmail(userEmail) { success ->
                                            if (success) {
                                                showVerificationEmailToast = true
                                                coroutineScope.launch {
                                                    delay(3500)
                                                    showVerificationEmailToast = false
                                                }
                                            } else {
                                                showVerificationEmailFailureToast = true
                                                coroutineScope.launch {
                                                    delay(3500)
                                                    showVerificationEmailFailureToast = false
                                                }
                                            }
                                        }
                                    }
                                )
                                AnimatedVisibility(visible = isProfileSaving) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    LinearProgressBar()
                                }
                            } else {
                                userProfile?.let {
                                    ProfileReadSection(
                                        profile = it,
                                        editMode = editMode,
                                        onEdit = { editMode = true },
                                        viewModel = viewModel
                                    )
                                }
                                AnimatedVisibility(visible = isProfileLoading) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    LinearProgressBar()
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (!editMode) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Button(
                                        onClick = { editMode = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF7F33),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Edit Profile")
                                    }

                                    Button(
                                        onClick = { onLogout() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.DarkGray,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Sign Out")
                                    }
                                }
                            }

                            // ── Danger Zone: Delete Account ──
                            item {
                                DeleteAccountSection(
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

    if (showSuccessToast) {
        Toast(message = "Profile saved!")
    }
    if (showFailureToast) {
        Toast(message = "Error saving profile.")
    }

    if (showVerificationEmailToast) {
        Toast(message = "Verification email sent. Please check your inbox.")
    }

    if (showVerificationEmailFailureToast) {
        Toast(message = "Error sending verification email. Please try again.")
    }
}


@Composable
fun ProfileReadSection(
    profile: UserProfileData,
    editMode: Boolean,
    onEdit: () -> Unit,
    viewModel: AuthPageViewModel
) {
    val formattedPhone = formatPhoneNumber(profile.phone ?: "")
    var showLinkPasswordDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfilePictureSection(
                profileImageBitmap = profile.profileImageBitmap,
                editMode = editMode,
                onImageUploaded = {},
                profileImageUrl = profile.profileImageUrl
            )
            
            Text(
                text = profile.name ?: "",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Text(
                text = profile.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )

            Text(
                text = formattedPhone,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )

            if (!profile.birthDate.isNullOrEmpty()) {
                Text(
                    text = "Birth Date: ${profile.birthDate}",
                    color = Color.LightGray
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Verified:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp)
                )

                if (profile.isVerified == true) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Verified",
                        tint = Color(0xFF00FF00),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Not Verified",
                        tint = Color(0xFFFF0000)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Member:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp)
                )
                if (profile.isMember == true) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Member",
                        tint = Color(0xFF00FF00),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Not Member",
                        tint = Color(0xFFFF0000)
                    )
                }
            }

            Text(
                text = "About Me",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = profile.aboutMe ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Haptic Feedback Settings
            Text(
                text = "Haptic Feedback",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            val (hapticIntensity, setHapticIntensity) = rememberHapticIntensity()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HapticIntensity.entries.forEach { level ->
                    val isSelected = hapticIntensity == level
                    Button(
                        onClick = {
                            setHapticIntensity(level)
                            if (level != HapticIntensity.OFF) {
                                hapticVibrate(HapticDuration.MEDIUM)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected)
                                Color(0xFFFF7F33) else Color(0xFF444444),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = level.label,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Settings Section
            Text(
                text = "Security Settings",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            if (!profile.hasPasskey) {
                Button(
                    onClick = { viewModel.registerPasskeyV2(profile.email ?: "") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800020))
                ) {
                    Text("Add Passkey")
                }
            }

            if (profile.hasPassword) {
                Button(
                    onClick = { showChangePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Change Password")
                }
            } else {
                Button(
                    onClick = { showLinkPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Link Password")
                }
            }

            if (editMode) {
                Button(onClick = onEdit) {
                    Text("Edit")
                }
            }
        }
    }

    if (showLinkPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLinkPasswordDialog = false },
            title = { Text("Link Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set a password for your account to use as an alternative login method.")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (password == confirmPassword && password.isNotEmpty()) {
                            viewModel.linkPasswordToAccount(password) { success ->
                                if (success) showLinkPasswordDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800020))
                ) {
                    Text("Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmNewPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword == confirmNewPassword && newPassword.isNotEmpty()) {
                            viewModel.changePassword(currentPassword, newPassword) { success ->
                                if (success) showChangePasswordDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800020))
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun ProfileEditSection(
    profile: UserProfileData?,
    editMode: Boolean,
    onSave: (UserProfileData) -> Unit,
    onCancel: () -> Unit,
    onEmailVerification: () -> Unit,
    isNewUser: Boolean,
    userEmail: String
) {
    val numericRegex = Regex("[^0-9]")
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var email by remember(profile) { mutableStateOf(profile?.email ?: userEmail) }
    var phone by remember(profile) { mutableStateOf(profile?.phone?.replace(
        numericRegex, "")?.take(10) ?: "") }
    var aboutMe by remember(profile) { mutableStateOf(profile?.aboutMe ?: "") }
    var profileImageBitmap by remember(profile) {
        mutableStateOf(profile?.profileImageBitmap)
    }
    var birthDate by remember(profile) { mutableStateOf(profile?.birthDate) }
    val isVerified by remember(profile) {
        mutableStateOf(profile?.isVerified ?: false)
    }
    val isMember by remember(profile) {
        mutableStateOf(profile?.isMember ?: false)
    }
    val hasPassword by remember(profile) {
        mutableStateOf(profile?.hasPassword ?: false)
    }
    val hasPasskey by remember(profile) {
        mutableStateOf(profile?.hasPasskey ?: false)
    }
    val showDatePicker = remember { mutableStateOf(false) }

    val updatedProfile = UserProfileData(
        name = name,
        email = email,
        phone = phone,
        aboutMe = aboutMe,
        profileImageBitmap = profileImageBitmap,
        birthDate = birthDate,
        isVerified = isVerified,
        isMember = isMember,
        hasPassword = hasPassword,
        hasPasskey = hasPasskey
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ProfilePictureSection(
                profileImageBitmap = profileImageBitmap,
                editMode = editMode,
                onImageUploaded = {
                    profileImageBitmap = it
                },
                profileImageUrl = profile?.profileImageUrl
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Color.White) },
                colors = TextFieldDefaults.colors(cursorColor = Color(0xFFFF7F33))
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.White) },
                colors = TextFieldDefaults.colors(cursorColor = Color(0xFFFF7F33))
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    val stripped = numericRegex.replace(it, "")
                    phone = stripped.take(10)
                },
                label = { Text("Phone", color = Color.White) },
                visualTransformation = NanpVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    cursorColor = Color(0xFFFF7F33),
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                )
            )


            Spacer(modifier = Modifier.height(8.dp))

            BirthDateField(
                birthDate = birthDate,
                onBirthDateChange = {
                    birthDate = it
                    showDatePicker.value = false
                },
                showDatePicker = showDatePicker
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = aboutMe,
                onValueChange = { aboutMe = it },
                label = { Text("About Me", color = Color.White) },
                colors = TextFieldDefaults.colors(cursorColor = Color(0xFFFF7F33)),
                modifier = Modifier.heightIn(min = 80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Verified:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp)
                )

                if (isVerified) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Verified",
                        tint = Color(0xFF00FF00),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Not Verified",
                        tint = Color(0xFFFF0000)
                    )
                    Button(
                        onClick = {
                            onEmailVerification()
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Verify your email")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Member:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp)
                )
                if (isMember) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Member",
                        tint = Color(0xFF00FF00),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Not Member",
                        tint = Color(0xFFFF0000)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onSave(updatedProfile) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7F33),
                        contentColor = Color.White
                    )
                ) {
                    Text("Save")
                }

                Button(
                    onClick = { onCancel() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF333333),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isNewUser) "Skip for now" else "Discard Changes"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDateField(
    birthDate: String?,
    onBirthDateChange: (String?) -> Unit,
    showDatePicker: MutableState<Boolean>
) {
    val datePickerState = rememberDatePickerState()
    val selectedDate = remember { mutableStateOf(birthDate ?: "") }
    val source = remember { MutableInteractionSource() }

    LaunchedEffect(birthDate) {
        selectedDate.value = birthDate ?: ""
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable {
                    showDatePicker.value = true
                }
        ) {
            OutlinedTextField(
                value = selectedDate.value,
                onValueChange = {
                    selectedDate.value = it
                    onBirthDateChange(it)
                },
                label = { Text("Date of birth", color = Color.White) },
                placeholder = { Text("MM/DD/YYYY", color = Color.White) },
                readOnly = true,
                interactionSource = source,
                colors = TextFieldDefaults.colors(cursorColor = Color(0xFFFF7F33)),
            )
        }

        Button(
            onClick = {
                showDatePicker.value = true
            },
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
            )
        ) {
            Text("")
        }

        if (showDatePicker.value) {
            AlertDialog(
                onDismissRequest = { showDatePicker.value = false },
                title = { Text("Select Date of Birth") },
                text = {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = true,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val formattedDate = convertMillisToDate(millis)
                                selectedDate.value = formattedDate
                                onBirthDateChange(formattedDate)
                            }
                            showDatePicker.value = false
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDatePicker.value = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun convertMillisToDate(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(localDateTime.monthNumber.toString().padStart(2, '0'))
        append("/")
        append(localDateTime.dayOfMonth.toString().padStart(2, '0'))
        append("/")
        append(localDateTime.year)
    }
}

@Composable
fun ProfilePictureSection(
    profileImageUrl: String?,
    profileImageBitmap: Bitmap?,
    editMode: Boolean,
    onImageUploaded: (Bitmap) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
    ) {
        val launcher = rememberFilePickerLauncher(
            mode = PickerMode.Single,
            type = PickerType.Image,
        ) { file ->
            if (file != null) {
                coroutineScope.launch {
                    val bytes = file.readBytes()
                    val skiaImage = Image.makeFromEncoded(bytes)
                    val bitmap = Bitmap().apply {
                        allocPixels(
                            ImageInfo.makeN32Premul(
                                skiaImage.width,
                                skiaImage.height
                            )
                        )
                    }
                    skiaImage.readPixels(bitmap, 0, 0)
                    onImageUploaded(bitmap)
                }
            }
        }

        if (!profileImageUrl.isNullOrEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(profileImageUrl),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            if (profileImageBitmap != null) {
                Image(
                    bitmap = profileImageBitmap.asComposeImageBitmap(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.placeholder),
                    contentDescription = "Profile Picture Placeholder",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (editMode) {
            Button(
                onClick = { launcher.launch() },
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Text("Change Image")
            }
        }
    }
}

// ─── Delete Account Section ─────────────────────────────────────────────────

@Composable
fun DeleteAccountSection(
    viewModel: AuthPageViewModel,
    onLogout: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    val requiredPhrase = "DELETE MY ACCOUNT"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1A1A)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Danger Zone",
                color = Color(0xFFFF6B6B),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Permanently delete your account and all associated data. " +
                    "This action cannot be undone.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))

            if (!showConfirm) {
                Button(
                    onClick = { showConfirm = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A2020),
                        contentColor = Color(0xFFFF6B6B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete My Account")
                }
            } else {
                Text(
                    "Type \"$requiredPhrase\" to confirm:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFF6B6B),
                        focusedIndicatorColor = Color(0xFFFF6B6B),
                        unfocusedIndicatorColor = Color(0xFF444444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            showConfirm = false
                            confirmText = ""
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            isDeleting = true
                            viewModel.deleteAccount(confirmText) { success ->
                                isDeleting = false
                                if (success) {
                                    onLogout()
                                }
                            }
                        },
                        enabled = confirmText == requiredPhrase && !isDeleting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B6B),
                            disabledContainerColor = Color(0xFF333333)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (isDeleting) "Deleting..."
                            else "Delete Forever",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}