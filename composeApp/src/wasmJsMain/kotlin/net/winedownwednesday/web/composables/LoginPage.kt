package net.winedownwednesday.web.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.LoginUIState
import org.koin.compose.koinInject

@Composable
fun LoginScreen(
    isCompactScreen: Boolean,
    onLoginSuccess: () -> Unit
) {
    val viewModel: AuthPageViewModel = koinInject()
    var email by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFD4AF37)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Feature under construction",
                color = Color.White,
            )
            Card(
                modifier = if (isCompactScreen) Modifier.fillMaxWidth() else Modifier.width(300.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegistering) "Register with Passkey" else "Login with Passkey",
//                        color = Color(0xFF800020),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF800020),
                            unfocusedBorderColor = Color(0xFF800020),
                            focusedLabelColor = Color(0xFF800020)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (isRegistering) {
                                    viewModel.checkSecureContext()
                                    viewModel.registerPasskey(email)
                                } else {
                                    viewModel.authenticateWithPasskey(email)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF800020),
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (isRegistering) "Register" else "Login")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { isRegistering = !isRegistering },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF800020))
                    ) {
                        Text(
                            text = if (isRegistering) "Already have a passkey? Login"
                            else "No passkey yet? Register",
                            color = Color.White
                        )
                    }

                    when (uiState) {
                        is LoginUIState.Error -> {
                            Text(
                                (uiState as LoginUIState.Error).message,
                                color = Color.Red,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        is LoginUIState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(top = 8.dp),
                                color = Color(0xFF800020)
                            )
                        }
                        is LoginUIState.Authenticated -> {
                            onLoginSuccess()
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}