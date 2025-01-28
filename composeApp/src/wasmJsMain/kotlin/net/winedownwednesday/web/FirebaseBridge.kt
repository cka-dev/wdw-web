package net.winedownwednesday.web

import kotlin.js.Promise

@JsName("wdwFirebaseBridge")
external object FirebaseBridge {
    fun initFirebase()


    fun requestNotificationPermission(): Promise<JsAny?>


    fun getFcmToken(): Promise<JsAny?>
}