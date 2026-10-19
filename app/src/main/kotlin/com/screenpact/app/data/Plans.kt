package com.screenpact.app.data

/**
 * Planes de Pro: única fuente de nombres, precios y enlaces de pago dentro de la app (la landing
 * y docs/PRODUCTO.md llevan los mismos). Si cambia un precio, se cambia aquí y allí.
 *
 * [link] es el Payment Link de Stripe de cada plan (https://buy.stripe.com/…). Mientras esté
 * vacío, el botón de comprar sale como "Muy pronto". Al pagar, Stripe redirige a
 * kimycars.com/screenpact/gracias, que entrega la licencia (ver Pro.kt).
 */
data class Plan(val id: String, val title: String, val subtitle: String, val price: String, val link: String = "")

object Plans {
    val all = listOf(
        Plan("monthly", "Mensual", "Cancela cuando quieras", "2,99 €/mes"),
        Plan("yearly", "Anual", "Sale a 1,67 € al mes", "19,99 €/año"),
        Plan("lifetime", "De por vida", "Pagas una vez", "49,99 €"),
        Plan("pack", "Pack Pacto", "El anual para ti y tu amigo", "29,99 €/año"),
    )

    /** El que sale marcado al abrir la pantalla. */
    const val DEFAULT = "yearly"

    /** Apps con límite en el plan gratis. Para añadir la siguiente hace falta Pro. */
    const val FREE_APP_LIMIT = 3

    fun byId(id: String): Plan? = all.firstOrNull { it.id == id }
}
