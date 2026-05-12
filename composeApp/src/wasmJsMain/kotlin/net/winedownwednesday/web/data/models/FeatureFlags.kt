package net.winedownwednesday.web.data.models

import kotlinx.serialization.Serializable

/**
 * Per-platform feature flags resolved by the server.
 *
 * All flags default to `false` so that:
 * 1. If the server hasn't deployed yet, features stay hidden.
 * 2. Unknown flags from future server versions are silently ignored.
 * 3. If the batch endpoint fails, the fallback is "all disabled."
 *
 * Usage in composables:
 * ```
 * val flags = LocalFeatureFlags.current
 * if (flags.deleteDmConversations) { ... }
 * ```
 */
@Serializable
data class FeatureFlags(
    val deleteDmConversations: Boolean = false,
    val mentionsAutocomplete: Boolean = false,
    val memberInfoGating: Boolean = false,
    val vinoWelcomePosts: Boolean = false,
    val onboardingEnforcement: Boolean = false,
)
