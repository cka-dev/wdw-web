package net.winedownwednesday.web.composables

import kotlinx.browser.window

// ---------------------------------------------------------------------------
// JS Interop — clipboard + navigator.share
// ---------------------------------------------------------------------------

@JsFun("(text) => navigator.clipboard.writeText(text)")
external fun copyToClipboard(text: JsString)

@JsFun("""
() => typeof navigator.share === 'function'
    && ('ontouchstart' in window || navigator.maxTouchPoints > 0)
    && window.innerWidth < 1024
"""
)
external fun navigatorCanShare(): Boolean

@JsFun("""
(url, title, text) => {
    navigator.share({ url: url, title: title, text: text })
        .catch(() => {});
}
""")
external fun navigatorShare(url: JsString, title: JsString, text: JsString)

/**
 * Share a URL using the native share sheet on mobile browsers,
 * falling back to clipboard copy on desktop.
 *
 * @return true if native share was used, false if clipboard copy was used.
 */
fun shareOrCopy(url: String, title: String, text: String): Boolean {
    return if (navigatorCanShare()) {
        navigatorShare(
            url.toJsString(),
            title.toJsString(),
            text.toJsString()
        )
        true
    } else {
        copyToClipboard(url.toJsString())
        false
    }
}

/**
 * Build a shareable URL for a given content type and ID.
 * Uses path-based format (/share/event/123) so the
 * serveOgPreview Cloud Function can serve dynamic OG
 * meta tags to social media crawlers. Real browsers
 * are 302-redirected to the SPA hash deep link.
 */
fun buildShareUrl(contentType: String, id: String): String {
    val origin = window.location.origin
    return "$origin/share/$contentType/$id"
}

/**
 * Build the SPA deep-link hash URL (used internally, not for sharing).
 */
fun buildDeepLinkHash(contentType: String, id: String): String {
    val paramName = when (contentType) {
        "event" -> "eventId"
        "wine" -> "wineId"
        "member" -> "memberId"
        "blog" -> "postId"
        else -> "id"
    }
    return "#${contentType}s?$paramName=$id"
}

/**
 * Trigger a browser download of an .ics calendar file.
 */
@JsFun("""
(content, filename) => {
    const blob = new Blob([content], { type: 'text/calendar' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}
""")
external fun downloadIcsFile(
    content: JsString,
    filename: JsString
)
