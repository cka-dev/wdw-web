package net.winedownwednesday.web.composables

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// ── JS interop — browser touch-capability detection ───────────────────────

/** Returns true when the primary pointer is coarse (finger / stylus). */
@JsFun("() => window.matchMedia('(pointer: coarse)').matches")
external fun isPrimaryPointerCoarse(): Boolean

/** Returns true when the device advertises touch points. */
@JsFun("() => navigator.maxTouchPoints > 0")
external fun hasTouchPoints(): Boolean

/**
 * CompositionLocal that is `true` on touch-capable devices
 * (phones, tablets, touchscreen laptops) and `false` on
 * traditional mouse-driven desktops.
 *
 * Provided once at the app root via [CompositionLocalProvider].
 */
val LocalIsTouchDevice = staticCompositionLocalOf {
    isPrimaryPointerCoarse() || hasTouchPoints()
}

// ── Brand-coloured PullToRefreshBox ───────────────────────────────────────

/**
 * A [PullToRefreshBox] that uses WDW Orange for the refresh spinner.
 */
@Composable
fun WdwPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                color = WdwOrange,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        content = content
    )
}
