package com.screenpact.app.data.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec

/** Las dos partes de un pacto llegan a las mismas llaves, y cada llave solo abre a su dueño. */
class PactKeysTest {

    private fun keyPair() = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    @Test
    fun bothSidesDeriveMatchingDirectionalKeys() {
        val a = keyPair(); val b = keyPair()
        val (aVerify, aGenerate) = PactKeys.derive("id-a", a.private, a.public.encoded, "id-b", b.public.encoded)
        val (bVerify, bGenerate) = PactKeys.derive("id-b", b.private, b.public.encoded, "id-a", a.public.encoded)

        // Lo que B genera es lo que A verifica, y al revés.
        assertArrayEquals(aVerify, bGenerate)
        assertArrayEquals(bVerify, aGenerate)
        // Cada sentido tiene su propia llave: los códigos que A da a B no abren a A.
        assertFalse(aVerify.contentEquals(aGenerate))

        val codeFromB = TOTPManager.currentCode(bGenerate)
        assertTrue(TOTPManager.verifyCode(aVerify, codeFromB))
        val codeFromA = TOTPManager.currentCode(aGenerate)
        assertFalse(TOTPManager.verifyCode(aVerify, codeFromA))
    }

    @Test
    fun aThirdPersonWithTheLinksCannotDeriveTheKeys() {
        val a = keyPair(); val b = keyPair(); val eve = keyPair()
        val (aVerify, _) = PactKeys.derive("id-a", a.private, a.public.encoded, "id-b", b.public.encoded)
        // Quien ve los dos enlaces solo tiene las dos claves públicas; con su propia privada no llega a nada.
        val (_, eveGuess) = PactKeys.derive("id-b", eve.private, b.public.encoded, "id-a", a.public.encoded)
        assertFalse(aVerify.contentEquals(eveGuess))
    }
}
