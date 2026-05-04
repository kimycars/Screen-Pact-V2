package com.screenpact.app.data.crypto

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 6238 TOTP. SHA1, 6 dígitos, paso de 30s.
 * El secreto compartido se intercambia por QR al emparejar dos amigos.
 */
object TOTPManager {

    private const val DIGITS = 6
    private const val PERIOD_SECONDS = 30L
    private const val ALGORITHM = "HmacSHA1"
    private const val SECRET_BYTES = 20 // 160 bits, igual que Google Authenticator

    fun generateSecret(): ByteArray {
        val bytes = ByteArray(SECRET_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    fun currentCode(secret: ByteArray, timeMillis: Long = System.currentTimeMillis()): String {
        val counter = timeMillis / 1000L / PERIOD_SECONDS
        return codeForCounter(secret, counter)
    }

    /** Verifica con ventana ±1 (90 segundos de tolerancia total) por desfase de relojes. */
    fun verifyCode(secret: ByteArray, code: String, timeMillis: Long = System.currentTimeMillis()): Boolean {
        if (code.length != DIGITS || !code.all { it.isDigit() }) return false
        val counter = timeMillis / 1000L / PERIOD_SECONDS
        for (offset in -1..1) {
            if (codeForCounter(secret, counter + offset) == code) return true
        }
        return false
    }

    fun secondsRemaining(timeMillis: Long = System.currentTimeMillis()): Int {
        val sec = timeMillis / 1000L
        return (PERIOD_SECONDS - (sec % PERIOD_SECONDS)).toInt()
    }

    private fun codeForCounter(secret: ByteArray, counter: Long): String {
        val mac = Mac.getInstance(ALGORITHM).apply {
            init(SecretKeySpec(secret, "RAW"))
        }
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
        val hash = mac.doFinal(counterBytes)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val mod = binary % POW10[DIGITS]
        return mod.toString().padStart(DIGITS, '0')
    }

    private val POW10 = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)
}
