package com.screenpact.app.data.crypto

import android.util.Base64
import org.json.JSONObject

/**
 * Payload incrustado en el QR de emparejamiento. Texto JSON base64-url:
 *   { "v": 1, "name": "Carla", "secret": "<base64 url-safe>" }
 *
 * No hay servidor: el secreto se intercambia físicamente al escanear.
 */
data class PairingPayload(
    val name: String,
    val secret: ByteArray
) {
    fun encode(): String {
        val json = JSONObject().apply {
            put("v", 1)
            put("name", name)
            put("secret", Base64.encodeToString(secret, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        }
        return "screenpact://pair?d=" + Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    companion object {
        fun decode(text: String): PairingPayload? = runCatching {
            val payload = if (text.startsWith("screenpact://pair?d=")) {
                text.removePrefix("screenpact://pair?d=")
            } else text
            val jsonBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val json = JSONObject(String(jsonBytes, Charsets.UTF_8))
            val name = json.getString("name")
            val secret = Base64.decode(
                json.getString("secret"),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            PairingPayload(name, secret)
        }.getOrNull()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PairingPayload) return false
        return name == other.name && secret.contentEquals(other.secret)
    }

    override fun hashCode(): Int = 31 * name.hashCode() + secret.contentHashCode()
}
