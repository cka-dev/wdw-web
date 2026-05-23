package net.winedownwednesday.web.composables

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.browser.localStorage
import net.winedownwednesday.web.data.models.WhatsNew

private val AccentOrange = Color(0xFFFF7F33)
private const val SEEN_KEY = "wdw_whats_new_seen_version"

/**
 * Returns the last "What's New" version the user has seen
 * (stored in localStorage), or null if never seen.
 */
fun getSeenWhatsNewVersion(): String? =
    localStorage.getItem(SEEN_KEY)

/**
 * Marks the given version as "seen" in localStorage.
 */
fun markWhatsNewSeen(version: String) {
    localStorage.setItem(SEEN_KEY, version)
}

/**
 * Dialog showing release highlights for the current version.
 * Shown once per version — the caller gates display via
 * [getSeenWhatsNewVersion].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewDialog(
    whatsNew: WhatsNew,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = {
        markWhatsNewSeen(whatsNew.version)
        onDismiss()
    }) {
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp)
                )
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Sparkle emoji header ────────────────────
                Text(
                    text = "✨",
                    fontSize = 36.sp,
                )
                Spacer(Modifier.height(8.dp))

                // ── Title ───────────────────────────────────
                Text(
                    text = whatsNew.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))

                // ── Items list ──────────────────────────────
                whatsNew.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = item.emoji,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 8.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme
                                    .colorScheme.onSurface,
                            )
                            Text(
                                text = item.description,
                                fontSize = 13.sp,
                                color = MaterialTheme
                                    .colorScheme.onSurface
                                    .copy(alpha = 0.65f),
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Dismiss button ──────────────────────────
                Button(
                    onClick = {
                        markWhatsNewSeen(whatsNew.version)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentOrange
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Got it!",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
