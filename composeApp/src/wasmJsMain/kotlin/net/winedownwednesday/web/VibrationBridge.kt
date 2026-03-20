package net.winedownwednesday.web

/**
 * Kotlin/Wasm interop for haptic feedback via browser APIs.
 *
 * ■ Android — uses the standard Vibration API (navigator.vibrate).
 * ■ iOS 18+ — falls back to programmatically clicking a hidden
 *   <input type="checkbox" switch> element, which triggers WebKit's
 *   native Taptic Engine feedback.
 * ■ Other platforms — all calls silently no-op.
 *
 * Requires a user gesture (click / tap) and a secure context (HTTPS).
 */

// ── Platform detection ───────────────────────────────────────────────────

/** Returns true if the browser supports navigator.vibrate(). */
fun isVibrationSupported(): Boolean = js("""
    { return ('vibrate' in navigator); }
""")

/** Returns true if we're running on iOS (iPhone/iPad/iPod). */
fun isIOS(): Boolean = js("""
    {
        return /iPhone|iPad|iPod/.test(navigator.userAgent) ||
               (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    }
""")

// ── Vibration ────────────────────────────────────────────────────────────

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

// ── iOS haptic fallback (hidden switch click) ────────────────────────────

/**
 * Trigger a single haptic tap on iOS 17.4+ by clicking the hidden
 * <label> wrapping <input type="checkbox" switch> in index.html.
 * Clicking the label triggers the native switch toggle via WebKit.
 */
fun iosHapticTap(): Unit = js("""
    {
        var lbl = document.getElementById('wdw-haptic-label');
        if (lbl) lbl.click();
    }
""")

/**
 * Trigger a pattern of haptic taps on iOS by clicking the hidden label
 * at timed intervals. Accepts a CSV string of alternating
 * tap/pause durations in ms.
 */
fun iosHapticPattern(patternCsv: String): Unit = js("""
    {
        var lbl = document.getElementById('wdw-haptic-label');
        if (!lbl) return;
        var parts = patternCsv.split(',').map(Number);
        var delay = 0;
        for (var i = 0; i < parts.length; i++) {
            if (i % 2 === 0) {
                (function(d) { setTimeout(function() { lbl.click(); }, d); })(delay);
            }
            delay += parts[i];
        }
    }
""")

// ── localStorage-backed haptic preference ────────────────────────────────

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
