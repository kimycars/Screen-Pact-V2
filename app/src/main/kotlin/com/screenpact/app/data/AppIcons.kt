package com.screenpact.app.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Iconos de la app (elegir uno es de Pro). Cada icono es un activity-alias del manifest; solo uno
 * está activo a la vez. El cambio puede tardar unos segundos en verse en el escritorio.
 */
object AppIcons {
    data class Icon(val alias: String, val name: String)

    val all = listOf(
        Icon("LauncherAmarillo", "Amarillo"),
        Icon("LauncherNoche", "Noche"),
        Icon("LauncherMenta", "Menta"),
    )

    private fun component(context: Context, alias: String) = ComponentName(context, "com.screenpact.app.$alias")

    fun current(context: Context): String {
        val pm = context.packageManager
        return all.firstOrNull { icon ->
            val state = pm.getComponentEnabledSetting(component(context, icon.alias))
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon.alias == all.first().alias)
        }?.alias ?: all.first().alias
    }

    fun set(context: Context, alias: String) {
        val pm = context.packageManager
        // Primero se activa el nuevo y luego se apagan los demás, para no quedarse nunca sin icono.
        pm.setComponentEnabledSetting(component(context, alias), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        all.filter { it.alias != alias }.forEach {
            pm.setComponentEnabledSetting(component(context, it.alias), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
}
