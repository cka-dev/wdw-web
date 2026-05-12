package net.winedownwednesday.web.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.winedownwednesday.web.NanpVisualTransformation
import net.winedownwednesday.web.data.models.UserProfileData
import net.winedownwednesday.web.viewmodels.AuthPageViewModel

enum class OnboardingStep {
    WELCOME,
    NAME,
    EMAIL_VERIFY,
    PHONE,
    ABOUT_YOU,
    INTERESTS,
    PHOTO,
    COMPLETE
}

@Composable
fun OnboardingWizard(
    profile: UserProfileData?,
    userEmail: String,
    onComplete: (UserProfileData) -> Unit,
    onEmailVerification: () -> Unit,
    viewModel: AuthPageViewModel
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    
    val numericRegex = Regex("[^0-9]")
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var email by remember(profile) { mutableStateOf(profile?.email ?: userEmail) }
    var phone by remember(profile) { mutableStateOf(profile?.phone?.replace(numericRegex, "")?.take(10) ?: "") }
    var profession by remember(profile) { mutableStateOf(profile?.profession ?: "") }
    var company by remember(profile) { mutableStateOf(profile?.company ?: "") }
    var interestsText by remember(profile) { mutableStateOf(profile?.interests?.joinToString(", ") ?: "") }
    var favoriteWinesText by remember(profile) { mutableStateOf(profile?.favoriteWines?.joinToString(", ") ?: "") }
    var profileImageBitmap by remember(profile) { mutableStateOf(profile?.profileImageBitmap) }
    var profileImageUrl by remember(profile) { mutableStateOf(profile?.profileImageUrl) }
    
    var isVerified by remember(profile) { mutableStateOf(profile?.isVerified == true) }
    var isPollingVerification by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    val steps = OnboardingStep.entries.toTypedArray()
    val currentIndex = steps.indexOf(currentStep)
    val totalSteps = steps.size - 2 // Don't count WELCOME and COMPLETE in progress
    
    val currentProgress = when (currentStep) {
        OnboardingStep.WELCOME -> 0
        OnboardingStep.COMPLETE -> totalSteps
        else -> currentIndex
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Progress Bar (hidden on Welcome and Complete)
            if (currentStep != OnboardingStep.WELCOME && currentStep != OnboardingStep.COMPLETE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..totalSteps) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (i == currentProgress) 10.dp else 8.dp)
                                .background(
                                    color = if (i <= currentProgress) Color(0xFFFF7F33) else Color(0xFF555555),
                                    shape = CircleShape
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Step $currentProgress of $totalSteps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { fullWidth -> fullWidth }
                    ) + fadeIn() togetherWith slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { fullWidth -> -fullWidth }
                    ) + fadeOut()
                },
                label = "WizardSteps"
            ) { step ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (step) {
                        OnboardingStep.WELCOME -> {
                            Text(
                                text = "Welcome to Wine Down Wednesday! 🍷",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "We're excited to have you join our community. Let's get your profile set up so other members can get to know you.",
                                color = Color(0xFFCCCCCC),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        OnboardingStep.NAME -> {
                            Text(
                                text = "What's your name?",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This is how other members will know you.",
                                color = Color(0xFFCCCCCC),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name *", color = Color.White) },
                                colors = TextFieldDefaults.colors(cursorColor = Color(0xFFFF7F33)),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        
                        OnboardingStep.EMAIL_VERIFY -> {
                            Text(
                                text = "Verify your email",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "We need to verify your email address to secure your account.",
                                color = Color(0xFFCCCCCC),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Email Address *", color = Color.White) },
                                colors = TextFieldDefaults.colors(
                                    cursorColor = Color(0xFFFF7F33),
                                    unfocusedContainerColor = Color(0xFF333333),
                                    focusedContainerColor = Color(0xFF333333)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (isVerified) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅ Email verified!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { 
                                        onEmailVerification() 
                                        isPollingVerification = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555))
                                ) {
                                    Text("Send Verification Link")
                                }
                                
                                if (isPollingVerification) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = Color(0xFFFF7F33), modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Waiting for verification...", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                            Text("Check your inbox and click the link.", color = Color(0xFFCCCCCC), style = MaterialTheme.typography.bodySmall)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(onClick = {
                                                viewModel.refreshVerificationStatus(email) { verified ->
                                                    isVerified = verified
                                                }
                                            }) {
                                                Text("I clicked it, check now", color = Color(0xFFFF7F33))
                                            }
                                        }
                                    }
                                    
                                    // Auto-polling effect
                                    LaunchedEffect(Unit) {
                                        while (!isVerified) {
                                            delay(8000) // Poll every 8 seconds
                                            viewModel.refreshVerificationStatus(email) { verified ->
                                                isVerified = verified
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        OnboardingStep.PHONE -> {
                            Text(
                                text = "What's your phone number?",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "We need this to keep you in the loop about community events.",
                                color = Color(0xFFCCCCCC),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { 
                                    val stripped = numericRegex.replace(it, "")
                                    phone = stripped.take(10)
                                },
                                label = { Text("Phone Number *", color = Color.White) },
                                visualTransformation = NanpVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    cursorColor = Color(0xFFFF7F33),
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        
                        OnboardingStep.ABOUT_YOU -> {
                            Text(
                                text = "Tell us about yourself",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Optional — skip anytime.",
                                color = Color(0xFFCCCCCC),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = profession,
                                onValueChange = { profession = it },
                                label = { Text("Profession", color = Color.White) },
                                colors = TextFieldDefaults.colors(cursorColor = Color(0xFFFF7F33)),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = company,
                                onValueChange = { company = it },
                                label = { Text("Company", color = Color.White) },
                                colors = TextFieldDefaults.colors(cursorColor = Color(0xFFFF7F33)),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        
                        OnboardingStep.INTERESTS -> {
                            Text(
                                text = "What are your interests?",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Optional — skip anytime.",
                                color = Color(0xFFCCCCCC),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = interestsText,
                                onValueChange = { interestsText = it },
                                label = { Text("Interests (comma-separated)", color = Color.White) },
                                colors = TextFieldDefaults.colors(cursorColor = Color(0xFFFF7F33)),
                                placeholder = { Text("Wine, Travel, Cooking", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = favoriteWinesText,
                                onValueChange = { favoriteWinesText = it },
                                label = { Text("Favorite Wines (comma-separated)", color = Color.White) },
                                colors = TextFieldDefaults.colors(cursorColor = Color(0xFFFF7F33)),
                                placeholder = { Text("Cabernet, Merlot, Pinot Noir", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        OnboardingStep.PHOTO -> {
                            Text(
                                text = "Add a profile photo",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Optional — skip anytime.",
                                color = Color(0xFFCCCCCC),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            ProfilePictureSection(
                                profileImageBitmap = profileImageBitmap,
                                editMode = true,
                                onImageUploaded = {
                                    profileImageBitmap = it
                                    profileImageUrl = null
                                },
                                profileImageUrl = profileImageUrl,
                                onImageRemoved = {
                                    profileImageBitmap = null
                                    profileImageUrl = null
                                }
                            )
                        }
                        
                        OnboardingStep.COMPLETE -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎉 You're all set!",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Your profile is complete. Welcome to the Wine Down Wednesday community!",
                                    color = Color(0xFFCCCCCC),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep != OnboardingStep.WELCOME && currentStep != OnboardingStep.COMPLETE) {
                    TextButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentStep = steps[currentIndex - 1]
                            }
                        }
                    ) {
                        Text("← Back", color = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f)) // pushes Next button to right
                }
                
                val isNextEnabled = when (currentStep) {
                    OnboardingStep.NAME -> name.isNotBlank()
                    OnboardingStep.EMAIL_VERIFY -> isVerified
                    OnboardingStep.PHONE -> phone.length == 10
                    else -> true
                }
                
                if (currentStep != OnboardingStep.COMPLETE) {
                    Button(
                        onClick = {
                            if (currentStep == OnboardingStep.WELCOME) {
                                currentStep = OnboardingStep.NAME
                            } else if (currentIndex < steps.size - 2) {
                                currentStep = steps[currentIndex + 1]
                            } else if (currentStep == OnboardingStep.PHOTO) {
                                // Last step, save profile
                                val updatedProfile = UserProfileData(
                                    name = name,
                                    email = email,
                                    phone = phone.ifBlank { null },
                                    aboutMe = profile?.aboutMe, // carry over
                                    profileImageBitmap = profileImageBitmap,
                                    profileImageUrl = profileImageUrl,
                                    birthDate = profile?.birthDate, // carry over
                                    isVerified = isVerified,
                                    isMember = profile?.isMember ?: false,
                                    hasPassword = profile?.hasPassword ?: false,
                                    hasPasskey = profile?.hasPasskey ?: false,
                                    eventRsvps = profile?.eventRsvps ?: emptyMap(),
                                    blockedEmails = profile?.blockedEmails ?: emptyList(),
                                    profession = profession.ifBlank { null },
                                    company = company.ifBlank { null },
                                    interests = interestsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { null },
                                    favoriteWines = favoriteWinesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { null },
                                )
                                onComplete(updatedProfile)
                                currentStep = OnboardingStep.COMPLETE
                            }
                        },
                        enabled = isNextEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7F33))
                    ) {
                        Text(
                            text = if (currentStep == OnboardingStep.WELCOME) "Let's get started" 
                                   else if (currentStep == OnboardingStep.PHOTO) "Finish" 
                                   else "Next →"
                        )
                    }
                } else {
                    // COMPLETE step doesn't have a next button here, it's handled automatically or with a "Go to Profile" button
                }
            }
        }
    }
}
