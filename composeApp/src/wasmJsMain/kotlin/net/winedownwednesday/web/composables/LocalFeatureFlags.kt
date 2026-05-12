package net.winedownwednesday.web.composables

import androidx.compose.runtime.staticCompositionLocalOf
import net.winedownwednesday.web.data.models.FeatureFlags

/**
 * CompositionLocal providing the current [FeatureFlags] to all composables.
 *
 * Provided at the app root (WdwTheme) and updated whenever
 * [AppRepository.featureFlags] changes.
 *
 * Usage:
 * ```
 * val flags = LocalFeatureFlags.current
 * if (flags.deleteDmConversations) { /* show feature */ }
 * ```
 */
val LocalFeatureFlags = staticCompositionLocalOf { FeatureFlags() }
