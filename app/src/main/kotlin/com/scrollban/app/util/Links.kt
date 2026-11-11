package com.scrollban.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Abre una dirección web en el navegador. Única forma de hacerlo en la app (salvo el pago, que va en Custom Tab). */
fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
