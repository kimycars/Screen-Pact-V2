package com.screenpact.app.ui.common

import com.screenpact.app.Brand
import android.content.Context
import android.content.Intent
import com.screenpact.app.data.crypto.PairingPayload

/**
 * Invitar a un amigo: ScreenPact no sirve sin otra persona, así que esto es el motor de
 * crecimiento. Abre el menú de compartir de Android con un mensaje y el enlace a la landing.
 */
object Invite {
    private const val URL = "https://www.kimycars.com/screenpact"

    fun share(context: Context) {
        shareText(
            context,
            "¿Hacemos un pacto? Con ${Brand.NAME} tú guardas mi llave y yo la tuya: " +
                "si me paso con el móvil, solo tú puedes darme más tiempo. Descárgala aquí: $URL",
            "Invitar a un amigo"
        )
    }

    /** Comparte un logro (un reto, el tiempo ganado) con el enlace para descargar la app. */
    fun shareAchievement(context: Context, text: String) =
        shareText(context, "$text Con ${Brand.NAME}: $URL", "Compartir")

    /** Abre el menú de compartir de Android con [text]. Única forma de compartir texto en la app. */
    fun shareText(context: Context, text: String, title: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Manda una invitación al pacto (o la respuesta a una) por la app que elija la persona.
     * [iphone]: la invitación es para un amigo con iPhone y abre la página web del pacto.
     */
    fun sharePact(context: Context, payload: PairingPayload, iphone: Boolean = false) {
        val text = if (iphone)
            "Sellemos un pacto en ${Brand.NAME}: tú guardas mi llave y yo pongo el código de Tiempo de uso de tu iPhone. No tienes que instalar nada, abre este enlace en tu iPhone: ${payload.iphoneLink()}"
        else if (payload.replyTo == null)
            "Sellemos un pacto en ${Brand.NAME}: tú guardas mi llave y yo la tuya. Abre este enlace en tu móvil: ${payload.link()}"
        else
            "Aquí va mi respuesta para cerrar nuestro pacto en ${Brand.NAME}. Ábrela en tu móvil: ${payload.link()}"
        shareText(context, text, if (payload.replyTo == null) "Mandar invitación" else "Mandar respuesta")
    }
}
