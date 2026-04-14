package net.winedownwednesday.web

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.Promise


external object myWebAuthnBridge {
    fun startRegistration(
        challenge: String,
        rpId: String,
        rpName: String,
        userId: String,
        userName: String,
        userDisplayName: String,
        timeout: Int,
        attestationType: String?,
        authenticatorAttachment: String?,
        residentKey: String?,
        requireResidentKey: Boolean?,
        userVerification: String?,
    ): Promise<JsAny>

    fun startAuthentication(
        challenge: String,
        rpId: String,
        timeout: Int,
        userVerification: String?,
        allowCredentialIds: String,
    ): Promise<JsAny>

    fun isSecureContext(): Boolean


    fun encodeBase64(bytes: Uint8Array): String
    fun decodeBase64(encoded: String): Uint8Array
    fun arrayBufferToBase64Url(buffer: ArrayBuffer): String
}

external interface AuthenticatorResponse: JsAny {
    val clientDataJSON: ArrayBuffer
    val attestationObject: ArrayBuffer?
    val authenticatorData: ArrayBuffer?
    val signature: ArrayBuffer?
    val userHandle: ArrayBuffer?
}

external interface PublicKeyCredential: JsAny {
    val id: String
    val rawId: ArrayBuffer
    val response: AuthenticatorResponse
    val type: String
}


fun ArrayBuffer.toBase64Url(): String {
    try {
        return myWebAuthnBridge.arrayBufferToBase64Url(this)
    } catch (e: Throwable) {
        throw e
    }
}



fun ByteArray.toBase64Url(): String {
    return myWebAuthnBridge.encodeBase64(this.toUint8Array())
        .replace('+', '-')
        .replace('/', '_')
        .replace("=", "")
}

fun Uint8Array.toByteArray(): ByteArray {
    val int8Array = Int8Array(this.buffer, this.byteOffset, this.length)
    return ByteArray(int8Array.length) { index -> int8Array[index] }
}

fun ByteArray.toUint8Array(): Uint8Array {
    val int8Array = Int8Array(this.size)
    for (i in this.indices) {
        int8Array[i] = this[i]
    }
    return Uint8Array(int8Array.buffer)
}