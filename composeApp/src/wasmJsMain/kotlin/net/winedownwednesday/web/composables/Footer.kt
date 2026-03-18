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
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.input.pointer.onPointerEvent
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
private const val PLAY_STORE_URL   = "https://play.google.com/store/apps/details?id=net.winedownwednesday.android"
private const val APP_STORE_URL    = "https://winedownwednesday.net/"   // placeholder until live
private const val PRIVACY_POLICY_URL = "https://www.freeprivacypolicy.com/live/6a72afcb-3f5c-4093-aee9-98a35c3b637c"

// ──────────────────────────────────────────────────────────
//  Full desktop / tablet footer
// ──────────────────────────────────────────────────────────
@Composable
fun Footer(
    onNavClick: (AppBarState) -> Unit,
    modifier: Modifier = Modifier
) {
    var showContactForm by remember { mutableStateOf(false) }

    if (showContactForm) {
        ContactFormDialog(onDismiss = { showContactForm = false })
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
                Image(
                    painter = painterResource(Res.drawable.wdw_new_logo),
                    contentDescription = "Wine Down Wednesday logo",
                    modifier = Modifier.size(48.dp)
                )
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
                    "Home"                   to AppBarState.HOME,
                    "About"                  to AppBarState.ABOUT,
                    "Blog"                   to AppBarState.BLOG,
                    "Members"                to AppBarState.MEMBERS,
                    "Events"                 to AppBarState.EVENTS,
                    "Our Wine"               to AppBarState.WINES,
                    "Uncorked Conversations" to AppBarState.PODCASTS,
                )
                val half = (navLinks.size + 1) / 2
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    // First half
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        navLinks.take(half).forEach { (label, state) ->
                            FooterLink(text = label, onClick = { onNavClick(state) })
                        }
                    }
                    // Second half
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        navLinks.drop(half).forEach { (label, state) ->
                            FooterLink(text = label, onClick = { onNavClick(state) })
                        }
                    }
                }
            }

            // ── Column 3 · Connect ───────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FooterSectionHeader("Connect")

                // White-tinted social icons
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

                // Google Play badge — fillMaxWidth so both badges are equal width
                Image(
                    painter = painterResource(Res.drawable.Google_Play_App),
                    contentDescription = "Get it on Google Play",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { window.open(PLAY_STORE_URL) }
                )

                // App Store badge — same fixed width
                Image(
                    painter = painterResource(Res.drawable.Download_on_the_App_Store_Badge_US_UK_RGB_wht_092917),
                    contentDescription = "Download on the App Store",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { window.open(APP_STORE_URL) }
                )
            }
        } // end Row

        // Copyright + Privacy Policy bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FooterBarBg)
                .padding(vertical = 12.dp, horizontal = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "© 2026 Wine Down Wednesday · All rights reserved",
                fontSize = 11.sp,
                color = TextMuted
            )
            Text(
                text = "  ·  ",
                fontSize = 11.sp,
                color = TextMuted
            )
            var privHovered by remember { mutableStateOf(false) }
            val privColor by animateColorAsState(
                targetValue = if (privHovered) AccentOrange else TextMuted,
                animationSpec = tween(150)
            )
            Text(
                text = "Privacy Policy",
                fontSize = 11.sp,
                color = privColor,
                modifier = Modifier.clickable {
                    privHovered = true
                    window.open(PRIVACY_POLICY_URL)
                }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────
//  Slim compact footer — above bottom nav bar on mobile
// ──────────────────────────────────────────────────────────
@Composable
fun CompactFooter(modifier: Modifier = Modifier) {
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

            // Copyright
            Text(
                text = "© 2026 WDW",
                fontSize = 10.sp,
                color = TextMuted
            )

            // Store badges
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
                    painter = painterResource(Res.drawable.Download_on_the_App_Store_Badge_US_UK_RGB_wht_092917),
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
            .onPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Enter) { hovered = true }
            .onPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Exit) { hovered = false }
            .clickable { onClick() }
    )
}
