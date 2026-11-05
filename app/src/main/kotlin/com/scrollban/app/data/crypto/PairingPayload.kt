package com.scrollban.app.data.crypto

import org.json.JSONObject

/**
 * Lo que va en el QR o en el enlace de invitación (versión 2). Nada de esto es secreto:
 * se puede mandar por WhatsApp o enseñar en una foto sin riesgo (ver PactKeys).
 *
 *   { "v": 2, "n": nombre, "i": id de instalación, "k": clave pública,
 *     "b": momento de encendido del móvil, "x": id de esta invitación, "r": id al que responde,
 *     "o": "ios" si responde la página web del pacto con iPhone }
 *
 * Una invitación nueva no lleva "r". La respuesta a una invitación a distancia lleva en "r" el
 * id de la invitación original, para que quien invitó sepa con qué clave privada combinarla.
 */
data class PairingPayload(
    val name: String,
    val installId: String,
    val publicKey: ByteArray,
    val bootTime: Long,
    val inviteId: String,
    val replyTo: String? = null,
    /** "ios" si la respuesta viene de la página web del pacto con iPhone (sin app). */
    val platform: String? = null
) {
    /** Enlace que se puede mandar por chat. Lo que va tras "#" no llega nunca al servidor. */
    fun link(): String = WEB + "#" + data()

    /** La misma invitación, para un amigo con iPhone: abre la página web que sella el pacto sin app. */
    fun iphoneLink(): String = WEB_IPHONE + "#" + data()

    private fun data() = PactKeys.b64(json().toByteArray(Charsets.UTF_8))

    private fun json(): String = JSONObject().apply {
        put("v", 2)
        put("n", name.take(MAX_NAME))
        put("i", installId)
        put("k", PactKeys.b64(publicKey))
        put("b", bootTime)
        put("x", inviteId)
        replyTo?.let { put("r", it) }
        platform?.let { put("o", it) }
    }.toString()

    override fun equals(other: Any?): Boolean =
        other is PairingPayload && inviteId == other.inviteId && installId == other.installId && publicKey.contentEquals(other.publicKey)

    override fun hashCode(): Int = inviteId.hashCode()

    companion object {
        const val WEB = "https://www.kimycars.com/scrollban/pacto"
        const val WEB_IPHONE = "https://www.kimycars.com/scrollban/iphone"
        private const val MAX_NAME = 40

        /**
         * Lee una invitación desde un QR, un enlace (https o scrollban://pair) o un mensaje de
         * chat que lo contenga. Null si no es de ScrollBan o es de una versión antigua.
         */
        fun decode(text: String): PairingPayload? = runCatching {
            val data = Regex("""(?:pacto#|pair\?d=)([A-Za-z0-9_-]+)""").find(text)?.groupValues?.get(1)
                ?: text.trim().takeIf { Regex("^[A-Za-z0-9_-]+$").matches(it) }
                ?: return null
            val json = JSONObject(String(PactKeys.unb64(data), Charsets.UTF_8))
            if (json.optInt("v") != 2) return null
            PairingPayload(
                name = json.getString("n").trim().take(MAX_NAME).ifBlank { "Tu amigo" },
                installId = json.getString("i"),
                publicKey = PactKeys.unb64(json.getString("k")),
                bootTime = json.optLong("b", 0),
                inviteId = json.getString("x"),
                replyTo = json.optString("r").takeIf { it.isNotBlank() },
                platform = json.optString("o").takeIf { it == "ios" }
            )
        }.getOrNull()

        /** True si el texto parece una invitación de una versión antigua (sin clave pública). */
        fun isLegacy(text: String): Boolean = text.contains("pair?d=") && decode(text) == null
    }
}
