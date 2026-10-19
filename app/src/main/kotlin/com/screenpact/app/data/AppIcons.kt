package com.screenpact.app.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.screenpact.app.R

/**
 * Iconos de la app (elegir uno es de Pro). Cada icono es un activity-alias del manifest; solo uno
 * está activo a la vez.
 *
 * Apagar el alias con el que se abrió la app hace que Android cierre la app al momento y te
 * mande al escritorio. Por eso el cambio se guarda como pendiente ([choose]) y se aplica al
 * salir de la app ([applyPending], desde MainActivity).
 */
object AppIcons {
    data class Icon(val alias: String, val name: String, @DrawableRes val foreground: Int)

    val all = listOf(
        Icon("LauncherAmarillo", "Amarillo", R.drawable.ic_launcher_foreground),
        Icon("LauncherNoche", "Noche", R.drawable.ic_launcher_noche_foreground),
        Icon("LauncherMenta", "Menta", R.drawable.ic_launcher_foreground),
    )

    private const val PREFS = "screenpact_icons"
    private const val KEY_PENDING = "pending"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** El icono elegido: el pendiente si lo hay, si no el activo. */
    fun chosen(context: Context): String = prefs(context).getString(KEY_PENDING, null) ?: current(context)

    /** Elige un icono; se pone al salir de la app. */
    fun choose(context: Context, alias: String) {
        if (alias == current(context)) prefs(context).edit().remove(KEY_PENDING).apply()
        else prefs(context).edit().putString(KEY_PENDING, alias).apply()
    }

    /** Pone el icono pendiente, si lo hay. Llamar solo cuando la persona sale de la app. */
    fun applyPending(context: Context) {
        val alias = prefs(context).getString(KEY_PENDING, null) ?: return
        prefs(context).edit().remove(KEY_PENDING).apply()
        set(context, alias)
    }

    private fun component(context: Context, alias: String) = ComponentName(context, "com.screenpact.app.$alias")

    fun current(context: Context): String {
        val pm = context.packageManager
        return all.firstOrNull { icon ->
            val state = pm.getComponentEnabledSetting(component(context, icon.alias))
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon.alias == all.first().alias)
        }?.alias ?: all.first().alias
    }

    private fun set(context: Context, alias: String) {
        val pm = context.packageManager
        // Primero se activa el nuevo y luego se apagan los demás, para no quedarse nunca sin icono.
        pm.setComponentEnabledSetting(component(context, alias), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        all.filter { it.alias != alias }.forEach {
            pm.setComponentEnabledSetting(component(context, it.alias), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
}
