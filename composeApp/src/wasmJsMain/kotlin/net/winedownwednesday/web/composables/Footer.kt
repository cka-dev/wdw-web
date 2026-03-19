package net.winedownwednesday.web.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
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
import wdw_web.composeapp.generated.resources.wdw_new_logo
import wdw_web.composeapp.generated.resources.yt_logo_96

// ── Brand colours ─────────────────────────────────────────
private val FooterBg      = Color(0xFF141414)
private val FooterBarBg   = Color(0xFF0D0D0D)
private val AccentOrange  = Color(0xFFFF7F33)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAAAAA)
private val TextMuted     = Color(0xFF666666)

// ── External links ────────────────────────────────────────
private const val INSTAGRAM_URL    = "https://www.instagram.com/uncorked.conversations/"
private const val YOUTUBE_URL      = "https://www.youtube.com/@WineDownWednesdayAtl"
private const val PLAY_STORE_URL   =
    "https://play.google.com/store/apps/details?id=net.winedownwednesday.android"
private const val APP_STORE_URL    = "https://winedownwednesday.net/"   // placeholder until live
private const val PRIVACY_POLICY_URL =
    "https://www.freeprivacypolicy.com/live/6a72afcb-3f5c-4093-aee9-98a35c3b637c"

// ──────────────────────────────────────────────────────────
//  Full desktop / tablet footer
// ──────────────────────────────────────────────────────────
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun Footer(
    onNavClick: (Route) -> Unit,
    modifier: Modifier = Modifier
) {
    var showContactForm by remember { mutableStateOf(false) }

    if (showContactForm) {
        ContactFormDialog(onDismiss = { showContactForm = false })
    }

    var imagesReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        imagesReady = true
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

        // Main columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // ── Column 1 · Brand ────────────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.widthIn(max = 200.dp)
            ) {
                if (imagesReady) {
                    Image(
                        painter = painterResource(Res.drawable.wdw_new_logo),
                        contentDescription = "Wine Down Wednesday logo",
                        modifier = Modifier.size(48.dp)
                    )
                }
                Text(
                    text = "Wine Down Wednesday",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Connecting wine enthusiasts from around Atlanta and the world.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            // ── Column 2 · Navigate (2 sub-columns) ─────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FooterSectionHeader("Navigate")
                val navLinks = listOf(
                    "Home"                   to Route.Home,
                    "About"                  to Route.About,
                    "Blog"                   to Route.Blog,
                    "Members"                to Route.Members,
                    "Events"                 to Route.Events,
                    "Our Wine"               to Route.Wines,
                    "Uncorked Conversations" to Route.Podcasts,
                )
                val third = (navLinks.size + 2) / 3
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        navLinks.take(third).forEach { (label, state) ->
                            FooterLink(text = label, onClick = { onNavClick(state) })
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        navLinks.drop(third).take(third).forEach { (label, state) ->
                            FooterLink(text = label, onClick = { onNavClick(state) })
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        navLinks.drop(third * 2).forEach { (label, state) ->
                            FooterLink(text = label, onClick = { onNavClick(state) })
                        }
                    }
                }
            }

            // ── Column 3 · Connect ───────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FooterSectionHeader("Connect")

                // White-tinted social icons
                if (imagesReady) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            painter = painterResource(Res.drawable.ig_logo_96),
                            contentDescription = "Instagram",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { window.open(INSTAGRAM_URL) }
                        )
                        Icon(
                            painter = painterResource(Res.drawable.yt_logo_96),
                            contentDescription = "YouTube",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { window.open(YOUTUBE_URL) }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
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
                    modifier = Modifier.clickable {
                        window.open("tel:+14049393370")
                    }
                )
            }

            // ── Column 4 · Get our Apps ──────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(148.dp)
            ) {
                FooterSectionHeader("Get our Apps")

                if (imagesReady) {
                    Image(
                        painter = painterResource(Res.drawable.Google_Play_App),
                        contentDescription = "Get it on Google Play",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { window.open(PLAY_STORE_URL) }
                    )
                    Image(
                        painter = painterResource(Res.drawable
                            .Download_on_the_App_Store_Badge_US_UK_RGB_wht_092917),
                        contentDescription = "Download on the App Store",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { window.open(APP_STORE_URL) }
                    )
                }
            }
        }   // end Row
    }
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

// ── Internal helpers ──────────────────────────────────────

@Composable
private fun FooterSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = AccentOrange,
        letterSpacing = 1.2.sp
    )
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    val color by animateColorAsState(
        targetValue = if (hovered) AccentOrange else TextSecondary,
        animationSpec = tween(150)
    )
    Text(
        text = text,
        fontSize = 13.sp,
        color = color,
        modifier = Modifier
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .clickable { onClick() }
    )
}
