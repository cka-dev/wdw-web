package net.winedownwednesday.web.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Accent / brand colours (unchanged across themes) ──────────────────────
val WdwOrange       = Color(0xFFFF7F33)
val WdwBurgundy     = Color(0xFF800020)

// ── Dark palette ───────────────────────────────────────────────────────────
private val DarkBackground      = Color(0xFF000000)
private val DarkSurface         = Color(0xFF1A1A1A)
private val DarkSurfaceVariant  = Color(0xFF333333)
private val DarkPrimary         = Color(0xFF1E1E1E)
private val DarkOnBackground    = Color(0xFFFFFFFF)
private val DarkOnSurface       = Color(0xFFFFFFFF)
private val DarkOnPrimary       = Color(0xFFFFFFFF)

// ── Light palette (champagne / warm cream) ─────────────────────────────────
val LightBackground             = Color(0xFFFEFDF9)  // barely-warm editorial white
val LightSurface                = Color(0xFFEFEFEF)  // clearly distinct from background
val LightSurfaceVariant         = Color(0xFFD9D9D9)  // more visible card surface
val LightPrimary                = Color(0xFFF5EDD8)  // champagne
val LightOnBackground           = Color(0xFF1A1A1A)
val LightOnSurface              = Color(0xFF1A1A1A)
val LightOnPrimary              = Color(0xFF1A1A1A)
val LightPrimaryContainer       = Color(0xFFFFD580)  // warm amber — brand-aligned
val LightOnPrimaryContainer     = Color(0xFF3D2B00)  // dark brown on amber

// Champagne gradient stops (used on Login page in light mode)
val ChampagneLight              = Color(0xFFFFE08A)
val ChampagneDark               = Color(0xFFD4AF37)

private fun wdwDarkColorScheme() = darkColorScheme(
    primary              = DarkPrimary,
    secondary            = DarkSurfaceVariant,
    surface              = DarkSurface,
    surfaceVariant       = DarkSurfaceVariant,
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    onSurface            = DarkOnSurface,
    onPrimary            = DarkOnPrimary,
    onSecondary          = DarkOnSurface,
    onSecondaryContainer = DarkOnSurface,
)

private fun wdwLightColorScheme() = lightColorScheme(
    primary              = LightPrimary,
    secondary            = LightSurface,
    surface              = LightSurface,
    surfaceVariant       = LightSurfaceVariant,
    background           = LightBackground,
    onBackground         = LightOnBackground,
    onSurface            = LightOnSurface,
    onPrimary            = LightOnPrimary,
    onSecondary          = LightOnSurface,
    onSecondaryContainer = LightOnSurface,
    primaryContainer     = LightPrimaryContainer,
    onPrimaryContainer   = LightOnPrimaryContainer,
)

// ── CompositionLocal so any composable can read the current mode ───────────
val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun WdwTheme(
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDark) wdwDarkColorScheme() else wdwLightColorScheme()
    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            content     = content
        )
    }
}
