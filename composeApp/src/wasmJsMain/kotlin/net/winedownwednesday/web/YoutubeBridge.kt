package net.winedownwednesday.web

/**
 * Kotlin/Wasm interop wrappers for window.wdwYoutubeBridge (youtube-bridge.js).
 *
 * Coordinates are CSS pixels (i.e. Compose px / LocalDensity.current.density).
 */

/** Creates/replaces the YouTube iframe at the given viewport coordinates. */
fun showYouTubePlayer(
    videoId: String,
    left: Float,
    top: Float,
    width: Float,
    height: Float
): Unit = js("""
    {
        if (window.wdwYoutubeBridge) {
            window.wdwYoutubeBridge.showPlayer(videoId, left, top, width, height);
        } else {
            console.error("YoutubeBridge: wdwYoutubeBridge not found on window");
        }
    }
""")

/** Removes the YouTube iframe from the DOM. */
fun hideYouTubePlayer(): Unit = js("""
    {
        if (window.wdwYoutubeBridge) {
            window.wdwYoutubeBridge.hidePlayer();
        }
    }
""")

/**
 * Repositions an already-visible player without recreating the iframe.
 * Called on every layout change (e.g., window resize).
 */
fun updateYouTubePlayerPosition(
    left: Float,
    top: Float,
    width: Float,
    height: Float
): Unit = js("""
    {
        if (window.wdwYoutubeBridge) {
            window.wdwYoutubeBridge.updatePosition(left, top, width, height);
        }
    }
""")
