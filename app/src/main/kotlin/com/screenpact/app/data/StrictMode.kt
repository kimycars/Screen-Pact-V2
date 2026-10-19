package com.screenpact.app.data

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.screenpact.app.services.StrictAdminReceiver

/**
 * Modo estricto (Pro): ScreenPact se hace "administrador del dispositivo" sin ningún permiso
 * real, solo para que Android no deje desinstalarla mientras esté activo. Quitarlo desde la app
 * pide la llave de un amigo; si se quita desde los Ajustes de Android, cuenta como trampa.
 */
object StrictMode {
    private const val PREFS = "screenpact_strict"
    private const val KEY_WANTED = "wanted"

    private fun admin(context: Context) = ComponentName(context, StrictAdminReceiver::class.java)
    private fun dpm(context: Context) = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isActive(context: Context): Boolean = dpm(context).isAdminActive(admin(context))

    /** True si la persona lo activó (para saber si un "desactivado" desde Ajustes es trampa). */
    fun isWanted(context: Context): Boolean = prefs(context).getBoolean(KEY_WANTED, false)

    /** Pantalla de Android para activarlo. */
    fun enableIntent(context: Context): Intent {
        prefs(context).edit().putBoolean(KEY_WANTED, true).apply()
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin(context))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Con el modo estricto, para desinstalar ScreenPact primero hay que quitarlo, y quitarlo pide la llave de un amigo. No da ningún otro permiso sobre tu móvil."
            )
    }

    /** Quitarlo desde la app (la llave ya se pidió antes de llamar aquí). */
    fun disable(context: Context) {
        prefs(context).edit().putBoolean(KEY_WANTED, false).apply()
        runCatching { dpm(context).removeActiveAdmin(admin(context)) }
    }
}
