package net.winedownwednesday.web.composables

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.winedownwednesday.web.vibrate
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.LoginUIState

@Composable
fun LoginScreen(
    isCompactScreen: Boolean,
    onLoginSuccess: () -> Unit,
    viewModel: AuthPageViewModel
) {
    val email by viewModel.email.collectAsState()
    var isRegistering by remember { mutableStateOf(false) }
    var isShowingOtherOptions by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE08A),
            Color(0xFFD4AF37)
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = if (isCompactScreen) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 400.dp)
                    } else {
                        Modifier
                            .width(400.dp)
                            .heightIn(min = 450.dp)
                    },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF343434)
                    )
                ) {
                    Crossfade(targetState = isShowingOtherOptions) { showingOther ->
                        if (showingOther) {
                            OtherSignInOptionsScreen(
                                onBack = {
                                    isShowingOtherOptions = false
                                    viewModel.resetToIdle()
                                },
                                viewModel = viewModel,
                                onLoginSuccess = onLoginSuccess
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isRegistering)
                                        "Register with Passkey" else
                                        "Login with Passkey",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineSmall
                                )

                                Text(
                                    text = if (isRegistering)
                                        "Create a passkey-enabled account"
                                    else
                                        "Secure login with passkey",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { viewModel.setEmail(it) },
                                    label = { Text("Email") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onKeyEvent { event ->
                                            return@onKeyEvent if (event.key.keyCode == Key.Enter.keyCode) {
                                                if (isRegistering) {
                                                    viewModel.registerPasskeyV2(email = email)
                                                } else {
                                                    viewModel.authenticateWithPasskeyV2(email = email)
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF800020),
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedLabelColor = Color(0xFF800020),
                                        cursorColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (isRegistering) {
                                                viewModel.registerPasskeyV2(email = email)
                                            } else {
                                                viewModel.authenticateWithPasskeyV2(email = email)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF800020),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = if (isRegistering) "Register" else "Login",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                TextButton(
                                    onClick = {
                                        isRegistering = !isRegistering
                                        viewModel.checkIsNewUser(isRegistering)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color(0xFF800020)
                                    )
                                ) {
                                    Text(
                                        text = if (isRegistering)
                                            "Already have a passkey? Login"
                                        else
                                            "No passkey yet? Register",
                                        color = Color.White
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        isShowingOtherOptions = true
                                        viewModel.resetToIdle()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Other sign in options", color = Color.White)
                                }

                                when (uiState) {
                                    is LoginUIState.Error -> {
                                        hapticVibratePattern(HapticPattern.ERROR)
                                        Text(
                                            (uiState as LoginUIState.Error).message,
                                            color = Color.Red,
                                            modifier = Modifier.padding(top = 8.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    is LoginUIState.Loading -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.padding(top = 8.dp),
                                            color = Color(0xFF800020)
                                        )
                                    }
                                    is LoginUIState.Authenticated -> {
                                        hapticVibrate(HapticDuration.MEDIUM)
                                        onLoginSuccess()
                                    }
                                    else -> { /* Nothing to do */ }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}