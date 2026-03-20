package net.winedownwednesday.web

/**
 * Kotlin/Wasm interop for the browser Vibration API (navigator.vibrate).
 *
 * Progressive enhancement — all calls silently no-op when the API
 * is unavailable (iOS Safari, desktop browsers without vibration hardware).
 *
 * Requires a user gesture (click / tap) and a secure context (HTTPS).
 */

/** Returns true if the browser supports navigator.vibrate(). */
fun isVibrationSupported(): Boolean = js("""
    { return ('vibrate' in navigator); }
""")

/** Single vibration pulse of [durationMs] milliseconds. */
fun vibrate(durationMs: Int): Unit = js("""
    { if ('vibrate' in navigator) navigator.vibrate(durationMs); }
""")

/**
 * Pattern vibration: accepts a comma-separated string of alternating
 * vibrate/pause durations.
 * E.g. vibratePatternStr("50,100,50") → 50ms buzz, 100ms pause, 50ms buzz.
 */
fun vibratePatternStr(patternCsv: String): Unit = js("""
    { if ('vibrate' in navigator) navigator.vibrate(patternCsv.split(',').map(Number)); }
""")

/** Cancel any ongoing vibration. */
fun cancelVibration(): Unit = js("""
    { if ('vibrate' in navigator) navigator.vibrate(0); }
""")

// ── localStorage-backed haptic preference ────────────────────────────────

private const val HAPTIC_KEY = "wdw_haptic_intensity"

/** Read the stored intensity preference. Returns "off"|"light"|"normal"|"strong". */
fun getHapticPreference(): String = js("""
    { return localStorage.getItem('wdw_haptic_intensity') || 'normal'; }
""")

/** Persist the intensity preference. Accepts "off"|"light"|"normal"|"strong". */
fun setHapticPreference(value: String): Unit = js("""
    { localStorage.setItem('wdw_haptic_intensity', value); }
""")

/** Read a per-category enabled flag. Returns "true" if not set (enabled by default). */
fun getHapticCategoryPref(key: String): String = js("""
    { return localStorage.getItem(key) || 'true'; }
""")

/** Write a per-category enabled flag. */
fun setHapticCategoryPref(key: String, enabled: String): Unit = js("""
    { localStorage.setItem(key, enabled); }
""")
