package com.scrollban.app.data

import java.security.SecureRandom

/**
 * Pacto mixto con iPhone: el amigo con iPhone usa el Tiempo de uso de Apple, bloqueado con un
 * código de 4 cifras que pone su amigo de Android sin que él lo vea. ScrollBan lo guarda para
 * que se lo pueda dar cuando le pida más tiempo. Si se lo dicta, hay que cambiarlo después.
 */
object IosCodes {

    private val random = SecureRandom()

    /** Código nuevo, sin los fáciles de adivinar (cuatro iguales o seguidos). */
    fun newCode(): String {
        while (true) {
            val code = random.nextInt(10_000).toString().padStart(4, '0')
            val digits = code.map { it - '0' }
            val same = digits.all { it == digits[0] }
            val up = digits.zipWithNext().all { (a, b) -> b == a + 1 }
            val down = digits.zipWithNext().all { (a, b) -> b == a - 1 }
            if (!same && !up && !down) return code
        }
    }
}
