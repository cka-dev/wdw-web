/**
 * AI Bridge for Kotlin/Wasm — Vino Bot & On-Device AI
 *
 * Tier 1: On-device inference via Chrome Prompt API (Gemini Nano)
 * Tier 2: Server-side inference via aiInfer Cloud Function
 * Tier 3: Vino bot Q&A via chatWithBot Cloud Function
 */
window.wdwAiBridge = {
    session: null,
    onDeviceAvailable: null, // cached capability check

    // ─── On-Device Capability Detection ────────────────────────────────────

    /**
     * Checks if the Chrome Prompt API (Gemini Nano) is available.
     * Caches the result for the session.
     * @returns {boolean}
     */
    isOnDeviceAvailable: function() {
        if (this.onDeviceAvailable !== null) return this.onDeviceAvailable;
        try {
            this.onDeviceAvailable = !!(
                window.ai && window.ai.languageModel
            );
        } catch (e) {
            this.onDeviceAvailable = false;
        }
        return this.onDeviceAvailable;
    },

    // ─── Tier 1: On-Device Prompt API ──────────────────────────────────────

    /**
     * Creates or reuses an on-device AI session.
     * @returns {Promise<object>} The session object.
     */
    _getSession: async function() {
        if (this.session) return this.session;
        if (!this.isOnDeviceAvailable()) return null;
        try {
            this.session = await window.ai.languageModel.create();
            return this.session;
        } catch (e) {
            console.warn("AiBridge: Failed to create on-device session:", e);
            this.onDeviceAvailable = false;
            return null;
        }
    },

    /**
     * Prompts the on-device model.
     * @param {string} prompt The prompt text.
     * @returns {Promise<string|null>} The response, or null if unavailable.
     */
    _promptOnDevice: async function(prompt) {
        const session = await this._getSession();
        if (!session) return null;
        try {
            return await session.prompt(prompt);
        } catch (e) {
            console.warn("AiBridge: On-device prompt failed:", e);
            return null;
        }
    },

    // ─── Tier 2: Server-Side Lightweight Inference ─────────────────────────

    /**
     * Calls the aiInfer Cloud Function for server-side inference.
     * @param {string} task Task type: "smart_replies" | "rewrite" | "translate"
     * @param {object} context Task-specific context.
     * @param {string} idToken Firebase auth ID token.
     * @param {string} functionUrl The Cloud Function URL.
     * @returns {Promise<string>} The inference result.
     */
    _inferOnServer: async function(task, context, idToken, functionUrl) {
        const response = await fetch(functionUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + idToken,
            },
            body: JSON.stringify({ task, context }),
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || "Server inference failed");
        }

        const data = await response.json();
        return data.result;
    },

    // ─── Public API: Smart Replies ─────────────────────────────────────────

    /**
     * Generates 3 smart reply suggestions.
     * Tries on-device first, falls back to server.
     * @param {string} contextJson JSON string of { messages: [{name, text}] }
     * @param {string} idToken Firebase auth ID token.
     * @param {string} functionUrl The aiInfer Cloud Function URL.
     * @returns {Promise<string>} JSON array of 3 reply suggestions.
     */
    generateSmartReplies: async function(contextJson, idToken, functionUrl) {
        const context = JSON.parse(contextJson);
        const messages = context.messages || [];
        const msgContext = messages
            .map(m => m.name + ": " + m.text)
            .join("\n");

        // Tier 1: Try on-device
        if (this.isOnDeviceAvailable()) {
            const prompt =
                "Given this chat conversation context, generate exactly 3 " +
                "brief, natural reply suggestions (max 6 words each). " +
                "Match the tone of the conversation. " +
                "Return ONLY a JSON array of 3 strings, nothing else.\n\n" +
                "Conversation:\n" + msgContext;

            const result = await this._promptOnDevice(prompt);
            if (result) return result;
        }

        // Tier 2: Fall back to server
        return await this._inferOnServer(
            "smart_replies", context, idToken, functionUrl
        );
    },

    // ─── Public API: Message Rewriting ─────────────────────────────────────

    /**
     * Rewrites a message with a given instruction.
     * @param {string} text The original message text.
     * @param {string} instruction Instruction type or custom instruction.
     * @param {string} idToken Firebase auth ID token.
     * @param {string} functionUrl The aiInfer Cloud Function URL.
     * @returns {Promise<string>} The rewritten message.
     */
    rewriteMessage: async function(text, instruction, idToken, functionUrl) {
        const instructions = {
            improve: "Improve grammar, clarity, and flow. Keep the same meaning and length.",
            casual: "Rewrite in a casual, conversational tone. Keep the same meaning.",
            formal: "Rewrite in a professional, formal tone. Keep the same meaning.",
            expand: "Expand this into a fuller, more detailed message. Add relevant context.",
            shorten: "Condense this into a shorter message. Keep the key points.",
            wine_flair: "Add wine-themed vocabulary and flair to this message. Make it sound like a sommelier wrote it. Keep the core meaning.",
        };

        const instr = instructions[instruction] || instruction;

        // Tier 1: Try on-device
        if (this.isOnDeviceAvailable()) {
            const prompt = instr + '\n\nOriginal message:\n"' + text +
                '"\n\nReturn ONLY the rewritten message, nothing else.';
            const result = await this._promptOnDevice(prompt);
            if (result) return result;
        }

        // Tier 2: Fall back to server
        return await this._inferOnServer(
            "rewrite", { text, instruction }, idToken, functionUrl
        );
    },

    // ─── Public API: Translation ───────────────────────────────────────────

    /**
     * Detects the language of text.
     * Uses Chrome LanguageDetector API if available.
     * @param {string} text Text to detect language of.
     * @returns {Promise<string>} ISO language code (e.g., "es", "fr").
     */
    detectLanguage: async function(text) {
        // Try Chrome LanguageDetector API
        if (window.translation && window.translation.createLanguageDetector) {
            try {
                const detector = await window.translation.createLanguageDetector();
                const results = await detector.detect(text);
                if (results && results.length > 0) {
                    return results[0].detectedLanguage;
                }
            } catch (e) {
                console.warn("AiBridge: Language detection API failed:", e);
            }
        }
        return "unknown";
    },

    /**
     * Translates text to the target language.
     * Tries Chrome Translator API first, falls back to server.
     * @param {string} text Text to translate.
     * @param {string} sourceLang Source language code (or "auto").
     * @param {string} targetLang Target language code (e.g., "en").
     * @param {string} idToken Firebase auth ID token.
     * @param {string} functionUrl The aiInfer Cloud Function URL.
     * @returns {Promise<string>} Translated text.
     */
    translateText: async function(text, sourceLang, targetLang, idToken, functionUrl) {
        // Tier 1: Try Chrome Translator API
        if (window.translation && window.translation.createTranslator) {
            try {
                const translator = await window.translation.createTranslator({
                    sourceLanguage: sourceLang === "auto" ? undefined : sourceLang,
                    targetLanguage: targetLang,
                });
                const result = await translator.translate(text);
                if (result) return result;
            } catch (e) {
                console.warn("AiBridge: Chrome translation API failed:", e);
            }
        }

        // Tier 2: Fall back to server
        return await this._inferOnServer(
            "translate",
            { text, sourceLang, targetLang },
            idToken,
            functionUrl
        );
    },

    /**
     * Sends a message to the Vino bot and gets a response.
     * Always server-side (Tier 3).
     * @param {string} channelId The Stream channel ID.
     * @param {string} messageText The user's message with @Vino mention.
     * @param {string|null} parentMessageId Optional parent message ID for threading.
     * @param {string} idToken Firebase auth ID token.
     * @param {string} functionUrl The chatWithBot Cloud Function URL.
     * @param {string} recentHistory JSON array of {name, text} recent messages.
     * @returns {Promise<string>} JSON response from the bot.
     */
    chatWithBot: async function(channelId, messageText, parentMessageId, idToken, functionUrl, recentHistory) {
        const now = new Date();
        const localTimeOfDay = now.toLocaleTimeString(
            [], { hour: 'numeric', minute: '2-digit', hour12: true }
        ) + ' ' + Intl.DateTimeFormat().resolvedOptions().timeZone;

        // Parse recentHistory if it's a JSON string
        let parsedHistory = null;
        try {
            if (recentHistory) parsedHistory = JSON.parse(recentHistory);
        } catch (_) {}

        const response = await fetch(functionUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + idToken,
            },
            body: JSON.stringify({
                channelId,
                messageText,
                parentMessageId: parentMessageId || null,
                localTimeOfDay,
                recentHistory: parsedHistory,
            }),
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || "Bot request failed");
        }

        const data = await response.json();
        return JSON.stringify(data);
    },


    // ─── Public API: Thread Summarization ──────────────────────────────────

    /**
     * Summarizes unread messages in a channel.
     * Always server-side (Tier 3).
     * @param {string} channelId The Stream channel ID.
     * @param {string|null} since ISO timestamp to summarize from.
     * @param {string} idToken Firebase auth ID token.
     * @param {string} functionUrl The summarizeThread Cloud Function URL.
     * @returns {Promise<string>} JSON summary object.
     */
    summarizeThread: async function(channelId, since, idToken, functionUrl) {
        const response = await fetch(functionUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + idToken,
            },
            body: JSON.stringify({
                channelId,
                since: since || null,
            }),
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || "Summary request failed");
        }

        const data = await response.json();
        return JSON.stringify(data);
    },
    // ─── Public API: Generic Authenticated POST ─────────────────────────────

    /**
     * Generic authenticated POST to any Cloud Function URL.
     * Used by recommendWines, recommendEvents, and aiInfer(summarize).
     * @param {string} functionUrl Full Cloud Function URL.
     * @param {string} bodyJson JSON body string.
     * @param {string} idToken Firebase auth ID token.
     * @returns {Promise<string>} Raw JSON response string.
     */
    callAuthenticatedApi: async function(functionUrl, bodyJson, idToken) {
        const response = await fetch(functionUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + idToken,
            },
            body: bodyJson,
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || "API request failed");
        }

        const data = await response.json();
        return JSON.stringify(data);
    },
};
