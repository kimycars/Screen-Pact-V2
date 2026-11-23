package com.scrollban.app.data.crypto

import com.scrollban.app.data.db.Friend
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Claves de los grupos de 3 o más.
 *
 * Cada grupo tiene un secreto de 32 bytes que crea quien funda el grupo. De él salen:
 *  - la llave de cada miembro: HMAC(secreto, "scrollban-group-unlock:" + id de instalación);
 *  - el secreto para entrar en el grupo del servidor (el servidor solo guarda su hash);
 *  - la clave con la que cada miembro cifra su nombre para el resto (el servidor no lo lee).
 *
 * El secreto viaja de un miembro a un amigo nuevo cifrado con una clave que sale de su pacto
 * ([seal]/[open]): el servidor lo guarda sin poder leerlo y nadie que vea un enlace lo tiene.
 */
object GroupKeys {

    private const val SECRET_BYTES = 32
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun newSecret(): ByteArray = ByteArray(SECRET_BYTES).also { SecureRandom().nextBytes(it) }

    /** Llave TOTP del miembro con id de instalación [installId] en el grupo de [secret]. */
    fun unlockKey(secret: ByteArray, installId: String): ByteArray =
        hmac(secret, "scrollban-group-unlock:$installId".toByteArray()).copyOf(20)

    /** Secreto para entrar en el grupo del servidor (64 caracteres hexadecimales). */
    fun joinSecret(secret: ByteArray): String = hex(hmac(secret, "scrollban-join".toByteArray()))

    /** Cifra [text] para el resto del grupo de [secret]. */
    fun sealForGroup(secret: ByteArray, text: String): String = seal(hmac(secret, "scrollban-profile".toByteArray()), text)

    fun openFromGroup(secret: ByteArray, box: String): String? = open(hmac(secret, "scrollban-profile".toByteArray()), box)

    /** Cifra [text] para el amigo del pacto [friend] (los dos móviles sacan la misma clave). */
    fun sealForFriend(friend: Friend, text: String): String = seal(pactKey(friend), text)

    fun openFromFriend(friend: Friend, box: String): String? = open(pactKey(friend), box)

    /**
     * Foto de perfil (JPEG) cifrada para un grupo de 3 o más ([secret]) o para el amigo de un
     * pacto. Clave propia de la foto, distinta de la del nombre y de la de las invitaciones.
     */
    fun sealPhotoForGroup(secret: ByteArray, jpeg: ByteArray): String = sealBytes(hmac(secret, PHOTO), jpeg)

    fun openPhotoFromGroup(secret: ByteArray, box: String): ByteArray? = openBytes(hmac(secret, PHOTO), box)

    fun sealPhotoForFriend(friend: Friend, jpeg: ByteArray): String = sealBytes(hmac(pactKey(friend), PHOTO), jpeg)

    fun openPhotoFromFriend(friend: Friend, box: String): ByteArray? = openBytes(hmac(pactKey(friend), PHOTO), box)

    private val PHOTO = "scrollban-photo".toByteArray()

    private fun pactKey(friend: Friend): ByteArray {
        // Un móvil tiene (mía, suya) y el otro (suya, mía): ordenadas, son las mismas.
        val (a, b) = listOf(friend.verifyKey, friend.generateKey).sortedBy { Base64.getEncoder().encodeToString(it) }
        return hmac(a + b, "scrollban-box".toByteArray())
    }

    private fun seal(key: ByteArray, text: String): String = sealBytes(key, text.toByteArray(Charsets.UTF_8))

    private fun sealBytes(key: ByteArray, data: ByteArray): String {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        }
        return PactKeys.b64(iv + cipher.doFinal(data))
    }

    /** Null si la caja está dañada o no es para esta clave. */
    private fun open(key: ByteArray, box: String): String? = openBytes(key, box)?.let { String(it, Charsets.UTF_8) }

    private fun openBytes(key: ByteArray, box: String): ByteArray? = runCatching {
        val bytes = PactKeys.unb64(box)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        }
        cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES)
    }.getOrNull()

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(key, "HmacSHA256")); doFinal(data) }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
}
