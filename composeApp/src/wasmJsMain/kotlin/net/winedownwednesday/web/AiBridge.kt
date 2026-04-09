package net.winedownwednesday.web

import kotlin.js.Promise

/**
 * Kotlin/Wasm external declarations for the AI bridge (ai-bridge.js).
 *
 * Three-tier hybrid AI architecture:
 * - Tier 1: On-device inference (Chrome Prompt API / Gemini Nano)
 * - Tier 2: Server-side lightweight inference (aiInfer Cloud Function)
 * - Tier 3: Server-side heavy inference (chatWithBot, summarizeThread)
 */
@JsName("wdwAiBridge")
external object AiBridge {

    /**
     * Checks if on-device AI (Chrome Prompt API) is available.
     */
    fun isOnDeviceAvailable(): Boolean

    /**
     * Generates 3 smart reply suggestions.
     * Tries on-device first, falls back to server.
     * @param contextJson JSON string: { messages: [{ name, text }] }
     * @param idToken Firebase auth ID token
     * @param functionUrl aiInfer Cloud Function URL
     * @return JSON array of 3 suggestion strings
     */
    fun generateSmartReplies(
        contextJson: String,
        idToken: String,
        functionUrl: String
    ): Promise<JsString>

    /**
     * Rewrites a message with a given instruction.
     * Tries on-device first, falls back to server.
     * @param text Original message text
     * @param instruction "improve"|"casual"|"formal"|"expand"|"shorten"|"wine_flair"
     * @param idToken Firebase auth ID token
     * @param functionUrl aiInfer Cloud Function URL
     * @return Rewritten message text
     */
    fun rewriteMessage(
        text: String,
        instruction: String,
        idToken: String,
        functionUrl: String
    ): Promise<JsString>

    /**
     * Detects the language of text using Chrome LanguageDetector API.
     * @param text Text to detect
     * @return ISO language code (e.g., "es", "fr") or "unknown"
     */
    fun detectLanguage(text: String): Promise<JsString>

    /**
     * Translates text to the target language.
     * Tries Chrome Translator API first, falls back to server.
     * @param text Text to translate
     * @param sourceLang Source language code or "auto"
     * @param targetLang Target language code (e.g., "en")
     * @param idToken Firebase auth ID token
     * @param functionUrl aiInfer Cloud Function URL
     * @return Translated text
     */
    fun translateText(
        text: String,
        sourceLang: String,
        targetLang: String,
        idToken: String,
        functionUrl: String
    ): Promise<JsString>

    /**
     * Sends a message to the Vino bot. Always server-side (Tier 3).
     * @param channelId Stream channel ID
     * @param messageText User's message with @Vino mention
     * @param parentMessageId Optional parent message ID for threading
     * @param idToken Firebase auth ID token
     * @param functionUrl chatWithBot Cloud Function URL
     * @param recentHistory JSON array of {name, text} objects from recent messages
     * @return JSON response string
     */
    fun chatWithBot(
        channelId: String,
        messageText: String,
        parentMessageId: String?,
        idToken: String,
        functionUrl: String,
        recentHistory: String = definedExternally
    ): Promise<JsString>

    /**
     * Summarizes unread messages in a channel. Always server-side (Tier 3).
     * @param channelId Stream channel ID
     * @param since ISO timestamp to summarize from (or null)
     * @param idToken Firebase auth ID token
     * @param functionUrl summarizeThread Cloud Function URL
     * @return JSON summary object string
     */
    fun summarizeThread(
        channelId: String,
        since: String?,
        idToken: String,
        functionUrl: String
    ): Promise<JsString>
}
