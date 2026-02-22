package net.winedownwednesday.web.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.LoginUIState

@Composable
fun OtherSignInOptionsScreen(
    onBack: () -> Unit,
    viewModel: AuthPageViewModel,
    onLoginSuccess: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetEmailSent by remember { mutableStateOf(false) }

    val email by viewModel.email.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isSignUp) "Create Account" else "Sign In",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.setEmail(it) },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF800020),
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = Color(0xFF800020),
                cursorColor = Color.White
            )
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF800020),
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = Color(0xFF800020),
                cursorColor = Color.White
            )
        )

        if (isSignUp) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF800020),
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color(0xFF800020),
                    cursorColor = Color.White
                )
            )
        }

        if (!isSignUp) {
            TextButton(
                onClick = {
                    resetEmail = email
                    showForgotPasswordDialog = true
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot Password?", color = Color.White)
            }
        }

        Button(
            onClick = {
                if (isSignUp) {
                    if (password == confirmPassword) {
                        viewModel.registerWithEmailPassword(email, password)
                    } else {
                        // Handle password mismatch
                    }
                } else {
                    viewModel.signInWithEmailPassword(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF800020),
                contentColor = Color.White
            )
        ) {
            Text(if (isSignUp) "Sign Up" else "Sign In")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSignUp) "Already have an account?" else "Don't have an account?",
                color = Color.LightGray
            )
            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(if (isSignUp) "Sign In" else "Sign Up", color = Color(0xFF800020))
            }
        }

        TextButton(onClick = onBack) {
            Text("Back to Passkey Login", color = Color.White)
        }

        when (uiState) {
            is LoginUIState.Error -> {
                Text(
                    (uiState as LoginUIState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
            is LoginUIState.Loading -> {
                CircularProgressIndicator(color = Color(0xFF800020))
            }
            is LoginUIState.Authenticated -> {
                onLoginSuccess()
            }
            else -> {}
        }
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    if (!resetEmailSent) {
                        Text("Enter your email address to receive a password reset link.")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("A password reset email has been sent to $resetEmail.")
                    }
                }
            },
            confirmButton = {
                if (!resetEmailSent) {
                    Button(
                        onClick = {
                            viewModel.sendPasswordResetEmail(resetEmail) { success ->
                                if (success) {
                                    resetEmailSent = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800020))
                    ) {
                        Text("Send Email")
                    }
                } else {
                    Button(
                        onClick = {
                            showForgotPasswordDialog = false
                            resetEmailSent = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800020))
                    ) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (!resetEmailSent) {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
