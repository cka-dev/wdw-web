package net.winedownwednesday.web.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import wdw_web.composeapp.generated.resources.Download_on_the_App_Store_Badge_US_UK_RGB_wht_092917
import wdw_web.composeapp.generated.resources.Google_Play_App
import wdw_web.composeapp.generated.resources.Res
import wdw_web.composeapp.generated.resources.ig_logo_96
import wdw_web.composeapp.generated.resources.yt_logo_96

// ── Brand colours ─────────────────────────────────────────
private val FooterBg      = Color(0xFF141414)
private val AccentOrange  = Color(0xFFFF7F33)
private val TextSecondary = Color(0xFFAAAAAA)
private val TextMuted     = Color(0xFF666666)

// ── External links ────────────────────────────────────────
private const val INSTAGRAM_URL    = "https://www.instagram.com/uncorked.conversations/"
private const val YOUTUBE_URL      = "https://www.youtube.com/@uncorked.conversations"
private const val PLAY_STORE_URL   =
    "https://play.google.com/store/apps/details?id=net.winedownwednesday.android"
private const val APP_STORE_URL    = "https://apps.apple.com/us/app/wine-down-wednesday/id6760641629"
private const val PRIVACY_POLICY_URL =
    "https://www.freeprivacypolicy.com/live/6a72afcb-3f5c-4093-aee9-98a35c3b637c"

// ──────────────────────────────────────────────────────────
//  Slim desktop / tablet footer  — single-row layout
// ──────────────────────────────────────────────────────────
@Composable
fun Footer(modifier: Modifier = Modifier) {
    var showContactForm by remember { mutableStateOf(false) }
    if (showContactForm) {
        ContactFormDialog(onDismiss = { showContactForm = false })
    }

    var assetsReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        assetsReady = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FooterBg)
    ) {
        // Orange accent divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(AccentOrange)
        )

        // ── Main content row (4 columns) ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // ── Col 1 · Connect (social icons) ───────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FooterLabel("Connect")
                if (assetsReady) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            painter = painterResource(Res.drawable.ig_logo_96),
                            contentDescription = "Instagram",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { window.open(INSTAGRAM_URL) }
                        )
                        Icon(
                            painter = painterResource(Res.drawable.yt_logo_96),
                            contentDescription = "YouTube",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { window.open(YOUTUBE_URL) }
                        )
                    }
                }
            }

            // ── Col 2 · Contact ───────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FooterLabel("Contact")
                Text(
                    text = "info@winedownwednesday.net",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.clickable { showContactForm = true }
                )
                Text(
                    text = "+1 (404) 939-3370",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.clickable { window.open("tel:+14049393370") }
                )
            }

            // ── Col 3 · Get our Apps ──────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FooterLabel("Get our Apps")
                if (assetsReady) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.Google_Play_App),
                            contentDescription = "Get it on Google Play",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .height(34.dp)
                                .widthIn(max = 105.dp)
                                .clickable { window.open(PLAY_STORE_URL) }
                        )
                        Image(
                            painter = painterResource(Res.drawable
                                .Download_on_the_App_Store_Badge_US_UK_RGB_wht_092917),
                            contentDescription = "Download on the App Store",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .height(34.dp)
                                .widthIn(max = 105.dp)
                                .clickable { window.open(APP_STORE_URL) }
                        )
                    }
                }
            }

            // ── Col 4 · Legal ─────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FooterLabel("Legal")
                Text(
                    text = "© 2025 Wine Down Wednesday",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = "Privacy Policy",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.clickable { window.open(PRIVACY_POLICY_URL) }
                )
            }
        }
    }
}


// ── Internal helpers ──────────────────────────────────────

@Composable
private fun FooterLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = AccentOrange,
        letterSpacing = 1.sp
    )
}

// ──────────────────────────────────────────────────────────
//  Slim compact footer — above bottom nav bar on mobile
// ──────────────────────────────────────────────────────────
@Composable
fun CompactFooter(modifier: Modifier = Modifier) {
    var badgesReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        badgesReady = true
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AccentOrange.copy(alpha = 0.5f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Social icons (white tinted)
            if (badgesReady) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        painter = painterResource(Res.drawable.ig_logo_96),
                        contentDescription = "Instagram",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { window.open(INSTAGRAM_URL) }
                    )
                    Icon(
                        painter = painterResource(Res.drawable.yt_logo_96),
                        contentDescription = "YouTube",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { window.open(YOUTUBE_URL) }
                    )
                }
            }

            // Store badges
            if (badgesReady) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(
                        painter = painterResource(Res.drawable.Google_Play_App),
                        contentDescription = "Google Play",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(24.dp)
                            .widthIn(max = 80.dp)
                            .clickable { window.open(PLAY_STORE_URL) }
                    )
                    Image(
                        painter = painterResource(Res.drawable
                            .Download_on_the_App_Store_Badge_US_UK_RGB_wht_092917),
                        contentDescription = "App Store",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(24.dp)
                            .widthIn(max = 80.dp)
                            .clickable { window.open(APP_STORE_URL) }
                    )
                }
            }
        }
    }
}