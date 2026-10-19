package com.screenpact.app.ui.common

import android.content.Context
import android.content.Intent

/**
 * Invitar a un amigo: ScreenPact no sirve sin otra persona, así que esto es el motor de
 * crecimiento. Abre el menú de compartir de Android con un mensaje y el enlace a la landing.
 */
object Invite {
    private const val URL = "https://www.kimycars.com/screenpact"

    fun share(context: Context) {
        val text = "¿Hacemos un pacto? Con ScreenPact tú guardas mi llave y yo la tuya: " +
            "si me paso con el móvil, solo tú puedes darme más tiempo. Descárgala aquí: $URL"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "Invitar a un amigo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
