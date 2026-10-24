package com.screenpact.app.data.crypto

import android.content.Context
import android.os.SystemClock
import java.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.UUID
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Claves del pacto con Diffie-Hellman (ECDH P-256).
 *
 * Por qué: antes el QR llevaba dentro, legible, la clave que desbloquea a quien lo enseña. Con
 * cualquier lector de QR y una app de códigos (Google Authenticator) uno podía desbloquearse
 * solo. Ahora el QR o el enlace solo llevan una clave PÚBLICA, que no sirve para nada por sí
 * sola. Cada móvil combina su clave privada (que nunca sale de él) con la pública del otro y
 * los dos llegan al mismo secreto, del que salen las dos llaves direccionales:
 *   llave que desbloquea a X = HKDF(secreto, "screenpact-unlock:" + id de X)
 * Así el enlace se puede mandar por WhatsApp sin que nadie que lo vea pueda usarlo.
 *
 * Cada clave privada es de un solo pacto: se guarda como invitación pendiente hasta que llega
 * la respuesta (o caduca a los [INVITE_TTL_MS]).
 */
object PactKeys {

    private const val PREFS = "screenpact_invites"
    const val INVITE_TTL_MS = 7L * 24 * 60 * 60_000L

    /** Margen para decidir que dos invitaciones vienen del mismo móvil encendido a la vez. */
    private const val SAME_BOOT_MS = 3_000L

    data class Invite(val id: String, val publicKey: ByteArray, val privateKey: PrivateKey)

    /** Crea una invitación nueva (par de claves) y la guarda como pendiente. */
    fun newInvite(context: Context): Invite {
        val kp = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val id = UUID.randomUUID().toString().replace("-", "").take(16)
        prefs(context).edit()
            .putString(id, b64(kp.private.encoded) + "|" + b64(kp.public.encoded) + "|" + System.currentTimeMillis())
            .apply()
        cleanExpired(context)
        return Invite(id, kp.public.encoded, kp.private)
    }

    /** La invitación pendiente [id], si existe y no ha caducado. */
    fun pending(context: Context, id: String): Invite? {
        val raw = prefs(context).getString(id, null) ?: return null
        val parts = raw.split("|")
        if (parts.size != 3 || System.currentTimeMillis() - parts[2].toLong() > INVITE_TTL_MS) { consume(context, id); return null }
        val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(unb64(parts[0])))
        return Invite(id, unb64(parts[1]), key)
    }

    /** Borra una invitación ya usada: cada respuesta solo vale una vez. */
    fun consume(context: Context, id: String) {
        prefs(context).edit().remove(id).apply()
    }

    private fun cleanExpired(context: Context) {
        val now = System.currentTimeMillis()
        val p = prefs(context)
        p.all.forEach { (k, v) ->
            val created = (v as? String)?.substringAfterLast("|")?.toLongOrNull() ?: 0L
            if (now - created > INVITE_TTL_MS) p.edit().remove(k).apply()
        }
    }

    /**
     * Las dos llaves del pacto entre yo ([myId], [myPrivate], [myPublic]) y el otro
     * ([otherId], [otherPublic]). Devuelve (verifyKey, generateKey): la que me desbloquea a mí y
     * la que desbloquea al otro. Lanza excepción si la clave pública del otro no es válida.
     */
    fun derive(myId: String, myPrivate: PrivateKey, myPublic: ByteArray, otherId: String, otherPublic: ByteArray): Pair<ByteArray, ByteArray> {
        val other: PublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(otherPublic))
        val shared = KeyAgreement.getInstance("ECDH").run { init(myPrivate); doPhase(other, true); generateSecret() }
        // La sal no depende de quién la calcula: las dos claves públicas en orden fijo.
        val (a, b) = listOf(myPublic, otherPublic).sortedBy { b64(it) }
        val salt = MessageDigest.getInstance("SHA-256").digest(a + b)
        val prk = hmac(salt, shared)
        fun key(id: String) = hmac(prk, "screenpact-unlock:$id".toByteArray() + byteArrayOf(1)).copyOf(20)
        return key(myId) to key(otherId)
    }

    /**
     * Momento en que se encendió este móvil. Es igual para todas las copias de una app en el
     * mismo móvil (apps duplicadas, segundo espacio), así que delata a quien intenta hacerse
     * amigo de sí mismo con una app clonada.
     */
    fun bootTime(): Long = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    fun sameDevice(otherBoot: Long): Boolean = otherBoot > 0 && kotlin.math.abs(otherBoot - bootTime()) <= SAME_BOOT_MS

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(key, "HmacSHA256")); doFinal(data) }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun b64(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    fun unb64(text: String): ByteArray = Base64.getUrlDecoder().decode(text)
}
