package net.winedownwednesday.web.composables

import androidx.compose.runtime.staticCompositionLocalOf

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
