package net.winedownwednesday.web.data.network

/**
 * Centralized Cloud Function endpoint URLs.
 *
 * REST endpoints used by [RemoteDataSource] share the same base via
 * [SERVER_URL]. AI/bot endpoints that bypass [RemoteDataSource] and
 * call Cloud Functions directly are listed individually below.
 */
object CloudFunctionUrls {
    const val SERVER_URL =
        "https://us-central1-wdw-app-52a3c.cloudfunctions.net"

    // ─── AI / Bot endpoints (called directly from ViewModels) ───────────

    const val AI_INFER = "$SERVER_URL/aiInfer"
    const val CHAT_WITH_BOT = "$SERVER_URL/chatWithBot"
    const val SUMMARIZE_THREAD = "$SERVER_URL/summarizeThread"
    const val RECOMMEND_WINES = "$SERVER_URL/recommendWines"
    const val RECOMMEND_EVENTS = "$SERVER_URL/recommendEvents"

    // ─── Cloud Run endpoints ────────────────────────────────────────────

    const val SEND_CONTACT_EMAIL =
        "https://sendcontactemail-iktff5ztia-uc.a.run.app"
    const val GENERATE_PASSKEY_REGISTRATION =
        "https://generatepasskeyregistrationoptions-iktff5ztia-uc.a.run.app"
    const val VERIFY_PASSKEY_REGISTRATION =
        "https://verifypasskeyregistration-iktff5ztia-uc.a.run.app"
    const val GENERATE_PASSKEY_AUTH =
        "https://generatepasskeyauthenticationoptions-iktff5ztia-uc.a.run.app"
    const val VERIFY_PASSKEY_AUTH =
        "https://verifypasskeyauthentication-iktff5ztia-uc.a.run.app"
}
