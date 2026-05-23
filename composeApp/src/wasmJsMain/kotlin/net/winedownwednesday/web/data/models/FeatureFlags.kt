package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

/**
 * Per-platform feature flags resolved by the server.
 *
 * ## Default Values & Endpoint Failure
 *
 * Defaults are chosen so that if the flag endpoint is
 * unreachable, the app degrades gracefully:
 *
 * - **Stable features** → default `true`
 *   Already shipped and proven. If the server is down,
 *   these stay ON so users aren't surprised.
 *
 * - **New/experimental features** → default `false`
 *   Not yet validated. If the server is down, these
 *   stay OFF so untested code never leaks.
 *
 * ## Flag Lifecycle
 *
 * 1. **New feature** → add flag with `default = false`
 * 2. **Feature enabled on some platforms** → server returns
 *    `true` for those platforms, `false` for others
 * 3. **Feature stable on all platforms** → flip default
 *    to `true`, then REMOVE the flag check from code
 *    and delete the flag from Firestore
 *
 * Flags are temporary — don't let them litter the codebase.
 * Once stable, remove them.
 *
 * Usage:
 * ```
 * val flags = LocalFeatureFlags.current
 * if (flags.deleteDmConversations) { ... }
 * ```
 */
@Serializable
data class FeatureFlags(
    // ── Stable features (default ON — survive outages) ──
    val deleteDmConversations: Boolean = true,
    val mentionsAutocomplete: Boolean = true,
    val memberInfoGating: Boolean = true,

    // ── New / experimental (default OFF — hidden until ready) ──
    val vinoWelcomePosts: Boolean = false,
    val onboardingEnforcement: Boolean = false,
    val notificationPreferences: Boolean = false,
    val rsvpCancellation: Boolean = false,
    val whatsNew: Boolean = false,
)
