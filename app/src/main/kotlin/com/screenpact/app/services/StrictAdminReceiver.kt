package com.screenpact.app.services

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.screenpact.app.data.StrictMode
import com.screenpact.app.util.TamperGuard

/** Receptor del modo estricto (ver data/StrictMode.kt). */
class StrictAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Si quitas el modo estricto desde aquí, tus apps con límite se bloquearán hasta que un amigo te dé su llave."

    override fun onDisabled(context: Context, intent: Intent) {
        // Si la persona lo quería activo y se ha quitado desde Ajustes (no desde la app), es trampa.
        if (StrictMode.isWanted(context)) TamperGuard.flag(context, TamperGuard.REASON_STRICT_OFF)
    }
}
