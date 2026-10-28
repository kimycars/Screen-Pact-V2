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
        val text = "¿Hacemos un pacto? Con ${Brand.NAME} tú guardas mi llave y yo la tuya: " +
            "si me paso con el móvil, solo tú puedes darme más tiempo. Descárgala aquí: $URL"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "Invitar a un amigo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Manda una invitación al pacto (o la respuesta a una) por la app que elija la persona. */
    fun sharePact(context: Context, payload: PairingPayload) {
        val text = if (payload.replyTo == null)
            "Sellemos un pacto en ${Brand.NAME}: tú guardas mi llave y yo la tuya. Abre este enlace en tu móvil: ${payload.link()}"
        else
            "Aquí va mi respuesta para cerrar nuestro pacto en ${Brand.NAME}. Ábrela en tu móvil: ${payload.link()}"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val title = if (payload.replyTo == null) "Mandar invitación" else "Mandar respuesta"
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
