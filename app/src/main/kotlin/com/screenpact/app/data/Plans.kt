package com.screenpact.app.data

/**
 * Planes de Pro: única fuente de nombres y precios dentro de la app (la landing y
 * docs/PRODUCTO.md llevan los mismos). Si cambia un precio, se cambia aquí y allí.
 */
data class Plan(val id: String, val title: String, val subtitle: String, val price: String)

object Plans {
    val all = listOf(
        Plan("monthly", "Mensual", "Cancela cuando quieras", "2,99 €/mes"),
        Plan("yearly", "Anual", "Sale a 1,67 € al mes", "19,99 €/año"),
        Plan("lifetime", "De por vida", "Pagas una vez", "49,99 €"),
    )

    /** El que sale marcado al abrir la pantalla. */
    const val DEFAULT = "yearly"

    /** Pack Pacto: el plan anual para ti y tu amigo. */
    const val PACT_PACK = "Pack Pacto: el anual para ti y tu amigo por 29,99 €."
}
