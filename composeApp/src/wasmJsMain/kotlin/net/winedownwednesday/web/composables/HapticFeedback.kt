package net.winedownwednesday.web.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import net.winedownwednesday.web.getHapticCategoryPref
import net.winedownwednesday.web.getHapticPreference
import net.winedownwednesday.web.setHapticCategoryPref
import net.winedownwednesday.web.setHapticPreference
import net.winedownwednesday.web.iosHapticPattern
import net.winedownwednesday.web.iosHapticTap
import net.winedownwednesday.web.isIOS
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
 * Per-category haptic feedback toggles.
 * Each category can be individually enabled/disabled via localStorage.
 */
enum class HapticCategory(val label: String, val storageKey: String) {
    NAVIGATION("Navigation", "wdw_haptic_nav"),
    INTERACTIONS("Interactions", "wdw_haptic_interact"),
    REACTIONS("Reactions", "wdw_haptic_react"),
    DIALOGS("Dialogs", "wdw_haptic_dialog"),
    ALERTS("Alerts", "wdw_haptic_alert");

    fun isEnabled(): Boolean = getHapticCategoryPref(storageKey) == "true"

    fun setEnabled(enabled: Boolean) {
        setHapticCategoryPref(storageKey, if (enabled) "true" else "false")
    }
}

// ── Core functions ───────────────────────────────────────────────────────

fun currentHapticIntensity(): HapticIntensity =
    HapticIntensity.fromString(getHapticPreference())

/**
 * Intensity-aware vibration with optional category check.
 * On Android: scales duration by intensity. On iOS 18+: triggers Taptic Engine via hidden switch.
 * No-ops if intensity is OFF or the category is disabled.
 */
fun hapticVibrate(baseDurationMs: Int, category: HapticCategory? = null) {
    val intensity = currentHapticIntensity()
    if (intensity == HapticIntensity.OFF) return
    if (category != null && !category.isEnabled()) return
    if (isIOS()) {
        // iOS: single Taptic Engine tap (no intensity/duration control)
        iosHapticTap()
    } else {
        val scaled = (baseDurationMs * intensity.multiplier).toInt().coerceAtLeast(1)
        vibrate(scaled)
    }
}

/**
 * Intensity-aware pattern vibration with optional category check.
 * On iOS: triggers timed Taptic Engine taps via hidden switch.
 */
fun hapticVibratePattern(basePattern: IntArray, category: HapticCategory? = null) {
    val intensity = currentHapticIntensity()
    if (intensity == HapticIntensity.OFF) return
    if (category != null && !category.isEnabled()) return
    if (isIOS()) {
        // iOS: timed switch clicks for pattern
        iosHapticPattern(basePattern.joinToString(","))
    } else {
        val scaled = basePattern.mapIndexed { index, ms ->
            if (index % 2 == 0) (ms * intensity.multiplier).toInt().coerceAtLeast(1) else ms
        }
        vibratePatternStr(scaled.joinToString(","))
    }
}

/**
 * Wraps a click callback with haptic feedback.
 */
fun hapticClick(
    durationMs: Int = HapticDuration.LIGHT,
    category: HapticCategory? = null,
    onClick: () -> Unit
): () -> Unit = {
    hapticVibrate(durationMs, category)
    onClick()
}

// ── Composable helpers ───────────────────────────────────────────────────

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

@Composable
fun rememberCategoryEnabled(category: HapticCategory): Pair<Boolean, (Boolean) -> Unit> {
    val state = remember { mutableStateOf(category.isEnabled()) }
    return state.value to { enabled: Boolean ->
        category.setEnabled(enabled)
        state.value = enabled
    }
}
