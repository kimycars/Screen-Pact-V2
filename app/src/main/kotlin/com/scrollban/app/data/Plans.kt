package com.scrollban.app.data

/**
 * Planes de Pro: única fuente de nombres, precios y enlaces de pago dentro de la app (la landing
 * y docs/PRODUCTO.md llevan los mismos). Si cambia un precio, se cambia aquí y allí.
 *
 * [link] es el Payment Link de Stripe de cada plan (https://buy.stripe.com/…). Mientras esté
 * vacío, el botón de comprar sale como "Muy pronto". Al pagar, Stripe redirige a
 * kimycars.com/scrollban/gracias, que entrega la licencia (ver Pro.kt).
 */
data class Plan(val id: String, val title: String, val subtitle: String, val price: String, val link: String = "")

object Plans {
    val all = listOf(
        Plan("monthly", "Mensual", "Cancela cuando quieras", "2,99 €/mes", "https://buy.stripe.com/test_8x2cMZ1w5afL92a0km7bW03"),
        Plan("yearly", "Anual", "1,67 € al mes · ahorras un 44 %", "19,99 €/año", "https://buy.stripe.com/test_4gM00ddeN3Rndiq1oq7bW02"),
        Plan("lifetime", "De por vida", "Pagas una vez", "49,99 €", "https://buy.stripe.com/test_7sYcMZb6F4VrguCaZ07bW01"),
        Plan("pack", "Pack Pacto", "El anual para ti y tu amigo", "29,99 €/año", "https://buy.stripe.com/test_bJeaER6Qp5Zva6e4AC7bW00"),
    )

    /** El recomendado: sale destacado arriba y marcado al abrir la pantalla. */
    const val DEFAULT = "yearly"

    /** Las otras dos formas de pagar Pro para una persona, en pequeño debajo del recomendado. */
    val ALTERNATIVES = listOf("monthly", "lifetime")

    /** Pro para dos: va aparte, porque es otra compra (para ti y tu amigo). */
    const val PACK = "pack"

    /** Apps con límite en el plan gratis. Para añadir la siguiente hace falta Pro. */
    const val FREE_APP_LIMIT = 3

    fun byId(id: String): Plan? = all.firstOrNull { it.id == id }
}
