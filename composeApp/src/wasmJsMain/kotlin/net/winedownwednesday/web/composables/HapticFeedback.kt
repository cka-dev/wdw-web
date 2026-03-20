package net.winedownwednesday.web.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import net.winedownwednesday.web.getHapticPreference
import net.winedownwednesday.web.setHapticPreference
import net.winedownwednesday.web.vibrate
import net.winedownwednesday.web.vibratePatternStr

/**
 * Predefined vibration durations (milliseconds) for consistent
 * haptic feedback across the compact mobile layout.
 *
 * These are the *base* values before intensity scaling is applied.
 */
object HapticDuration {
    /** Ultra-light tap — hamburger menu, minor toggles. */
    const val TICK = 10

    /** Standard tap — nav bar / drawer item selection. */
    const val LIGHT = 25

    /** Confirm action — RSVP, send message, form submit. */
    const val MEDIUM = 50

    /** Strong feedback — error, warning, destructive action. */
    const val HEAVY = 100
}

/**
 * Predefined vibration patterns (alternating vibrate/pause in ms).
 */
object HapticPattern {
    /** Error: buzz–pause–buzz (50–100–50). */
    val ERROR = intArrayOf(50, 100, 50)

    /** Success: single smooth pulse (handled by HapticDuration.MEDIUM). */
    val SUCCESS = intArrayOf(40)

    /** Warning: short–short (30–60–30). */
    val WARNING = intArrayOf(30, 60, 30)
}

/**
 * User-configurable haptic intensity levels.
 *
 * Each level has a [multiplier] that scales the base [HapticDuration] values.
 * "OFF" disables all haptic feedback.
 */
enum class HapticIntensity(val label: String, val multiplier: Float) {
    OFF("Off", 0f),
    LIGHT("Light", 0.5f),
    NORMAL("Normal", 1.0f),
    STRONG("Strong", 1.5f);

    companion object {
        fun fromString(value: String): HapticIntensity =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: NORMAL
    }
}

/**
 * Read the user's haptic intensity preference from localStorage.
 */
fun currentHapticIntensity(): HapticIntensity =
    HapticIntensity.fromString(getHapticPreference())

/**
 * Intensity-aware vibration. Scales [baseDurationMs] by the user's preference.
 * No-ops if intensity is OFF.
 */
fun hapticVibrate(baseDurationMs: Int) {
    val intensity = currentHapticIntensity()
    if (intensity == HapticIntensity.OFF) return
    val scaled = (baseDurationMs * intensity.multiplier).toInt().coerceAtLeast(1)
    vibrate(scaled)
}

/**
 * Intensity-aware pattern vibration. Scales every vibration segment
 * (odd indices are pauses and stay unchanged).
 */
fun hapticVibratePattern(basePattern: IntArray) {
    val intensity = currentHapticIntensity()
    if (intensity == HapticIntensity.OFF) return
    val scaled = basePattern.mapIndexed { index, ms ->
        if (index % 2 == 0) (ms * intensity.multiplier).toInt().coerceAtLeast(1) else ms
    }
    vibratePatternStr(scaled.joinToString(","))
}

/**
 * Wraps a click callback with haptic feedback.
 *
 * Usage: `.clickable(onClick = hapticClick { doSomething() })`
 */
fun hapticClick(
    durationMs: Int = HapticDuration.LIGHT,
    onClick: () -> Unit
): () -> Unit = {
    hapticVibrate(durationMs)
    onClick()
}

/**
 * Composable helper to remember the current haptic intensity and provide
 * a setter that persists to localStorage.
 */
@Composable
fun rememberHapticIntensity(): Pair<HapticIntensity, (HapticIntensity) -> Unit> {
    val state = remember {
        mutableStateOf(HapticIntensity.fromString(getHapticPreference()))
    }
    return state.value to { newIntensity: HapticIntensity ->
        setHapticPreference(newIntensity.name.lowercase())
        state.value = newIntensity
    }
}
