package net.winedownwednesday.web

import kotlin.js.Promise

@JsName("wdwFirebaseBridge")
external object FirebaseBridge {
    fun initFirebase()

    fun requestNotificationPermission(): Promise<JsAny?>

    fun getFcmToken(): Promise<JsAny?>

    fun signInWithCustomToken(token: String): Promise<JsAny>
    fun signOut(): Promise<JsAny>
    fun getCurrentUser(): JsAny?
    fun persistSession()
    fun observeAuthState(callback: (JsAny?) -> Unit)
    fun waitUntilInitialized(): Promise<JsAny>

}

external interface FbResponse: JsAny {
    val idToken: String
    val verified: Boolean
}

external interface FirebaseUser: JsAny {
    val uid: String
    val email: String?
}