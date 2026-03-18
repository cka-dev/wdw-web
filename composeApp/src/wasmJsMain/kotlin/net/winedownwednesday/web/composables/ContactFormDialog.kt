package net.winedownwednesday.web.composables

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val CONTACT_ENDPOINT =
    "https://sendcontactemail-iktff5ztia-uc.a.run.app"

private val DialogBg     = Color(0xFF1E1E1E)
private val AccentOrange = Color(0xFFFF7F33)
private val TextGray     = Color(0xFFAAAAAA)
private val ErrorRed     = Color(0xFFFF5252)
private val FieldBorder  = Color(0xFF444444)

// ── JS interop ────────────────────────────────────────────────────────────────
// In Kotlin/WASM 2.1, Promise<T> cannot be a @JsFun return type.
// We use a callback-based external function instead and bridge it to
// coroutines via suspendCancellableCoroutine.
@JsFun("""
function doContactPost(url, body, onSuccess, onError) {
    fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: body
    }).then(function(r) {
        if (!r.ok) { onError('HTTP ' + r.status); return; }
        onSuccess();
    }).catch(function(e) {
        onError(e.message || 'Network error');
    });
}
""")
external fun doContactPost(
    url: String,
    body: String,
    onSuccess: () -> Unit,
    onError: (JsString) -> Unit,
)

private suspend fun postContactForm(url: String, body: String) =
    suspendCancellableCoroutine<Unit> { cont ->
        doContactPost(
            url, body,
            onSuccess = { if (cont.isActive) cont.resume(Unit) },
            onError   = { err ->
                if (cont.isActive) cont.resumeWithException(Exception(err.toString()))
            }
        )
    }

// ── Dialog ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFormDialog(onDismiss: () -> Unit) {
    var name      by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var message   by remember { mutableStateOf("") }
    var nameErr   by remember { mutableStateOf("") }
    var emailErr  by remember { mutableStateOf("") }
    var msgErr    by remember { mutableStateOf("") }
    var loading   by remember { mutableStateOf(false) }
    var success   by remember { mutableStateOf(false) }
    var submitErr by remember { mutableStateOf("") }
    val scope     = rememberCoroutineScope()

    fun validate(): Boolean {
        var ok = true
        nameErr  = if (name.isBlank())  { ok = false; "Name is required"    } else ""
        emailErr = if (email.isBlank()) { ok = false; "Email is required"   }
                   else if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+\$").matches(email))
                             { ok = false; "Invalid email" }
                   else ""
        msgErr   = if (message.isBlank()) { ok = false; "Message is required" } else ""
        return ok
    }

    fun submit() {
        if (!validate()) return
        loading   = true
        submitErr = ""
        scope.launch {
            try {
                val nameEsc = name
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                val emailEsc = email
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                val msgEsc = message
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                val json = """{"name":"$nameEsc","email":"$emailEsc","message":"$msgEsc"}"""
                postContactForm(CONTACT_ENDPOINT, json)
                success = true
            } catch (e: Exception) {
                submitErr = "Failed to send. Please try again."
            } finally {
                loading = false
            }
        }
    }

    BasicAlertDialog(onDismissRequest = { if (!loading) onDismiss() }) {
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .background(DialogBg, RoundedCornerShape(12.dp))
                .padding(28.dp)
        ) {
            if (success) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = AccentOrange,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Message sent!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Thank you for reaching out. We'll get back to you soon.",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                    ) { Text("Close") }
                }
            } else {
                Column {
                    Text(
                        text = "Get in Touch",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "We'd love to hear from you.",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                    Spacer(Modifier.height(20.dp))
                    ContactField(
                        label = "Name",
                        value = name,
                        error = nameErr,
                        onValueChange = { name = it; nameErr = "" }
                    )
                    Spacer(Modifier.height(12.dp))
                    ContactField(
                        label = "Your email",
                        value = email,
                        error = emailErr,
                        onValueChange = { email = it; emailErr = "" }
                    )
                    Spacer(Modifier.height(12.dp))
                    ContactField(
                        label = "Message",
                        value = message,
                        error = msgErr,
                        onValueChange = { message = it; msgErr = "" },
                        minLines = 4
                    )
                    AnimatedVisibility(submitErr.isNotEmpty()) {
                        Text(
                            text = submitErr,
                            fontSize = 12.sp,
                            color = ErrorRed,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !loading
                        ) { Text("Cancel", color = TextGray) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { submit() },
                            enabled = !loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentOrange
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Send Message", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactField(
    label: String,
    value: String,
    error: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = TextGray, fontSize = 12.sp) },
            isError = error.isNotEmpty(),
            minLines = minLines,
            maxLines = if (minLines > 1) 8 else 1,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = AccentOrange,
                unfocusedBorderColor = FieldBorder,
                errorBorderColor     = ErrorRed,
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = AccentOrange,
            )
        )
        if (error.isNotEmpty()) {
            Text(
                text = error,
                fontSize = 11.sp,
                color = ErrorRed,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
