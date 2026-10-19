package com.screenpact.app.util

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/**
 * Detecta las trampas que se saltan el pacto sin pasar por el código de un amigo.
 *
 * Mientras haya trampa, TODAS las apps con límite quedan bloqueadas (sin mirar el uso) hasta
 * que un amigo dé su código. Dos tipos:
 *  - En vivo: la hora del sistema está en manual. Cambiar la fecha reinicia el contador del día,
 *    así que con hora manual no se puede usar ninguna app limitada. Se quita sola al volver a
 *    hora automática.
 *  - Guardada: se quitaron permisos o se paró el servicio. Solo se borra con código de amigo.
 *
 * Solo se arma cuando hay al menos un amigo y los permisos se concedieron alguna vez: sin amigos
 * nadie podría dar el código y el usuario quedaría encerrado.
 *
 * Usa SharedPreferences y no DataStore porque se escribe un latido cada vuelta del servicio.
 */
object TamperGuard {

    const val REASON_MANUAL_CLOCK = "Tienes la fecha y hora en manual. Activa la hora automática en Ajustes para volver a usar esta app."
    const val REASON_USAGE_ACCESS = "Se quitó el acceso a datos de uso de ScreenPact."
    const val REASON_OVERLAY = "Se quitó el permiso de mostrarse encima de otras apps."
    const val REASON_SERVICE_STOPPED = "ScreenPact estuvo parado un rato."
    const val REASON_STRICT_OFF = "Se quitó el modo estricto desde los Ajustes de Android."

    private const val PREFS = "screenpact_guard"
    private const val KEY_ARMED = "armed"
    private const val KEY_REASON = "reason"
    private const val KEY_BEAT_UPTIME = "beat_uptime"
    private const val KEY_BEAT_BOOT = "beat_boot"

    /**
     * Hueco máximo entre latidos dentro del mismo arranque. Se mide con uptimeMillis, que no
     * avanza con el móvil dormido, así que una noche con la pantalla apagada no cuenta como hueco.
     */
    private const val MAX_GAP_MS = 15 * 60_000L

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Trampa guardada, si la hay. */
    fun savedReason(context: Context): String? = prefs(context).getString(KEY_REASON, null)

    /** Apunta una trampa detectada fuera del servicio (por ejemplo, al quitar el modo estricto). */
    fun flag(context: Context, reason: String) {
        if (isArmed(context)) prefs(context).edit().putString(KEY_REASON, reason).apply()
    }

    /** Un amigo ha dado su código: se perdona la trampa guardada. */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_REASON).apply()
    }

    fun isAutoTime(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 1) == 1

    private fun bootCount(context: Context): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    /**
     * Se llama en cada vuelta del servicio. Registra el latido, detecta trampas nuevas y devuelve
     * el motivo de bloqueo vigente, o null si todo está en orden.
     */
    fun evaluate(context: Context, hasFriends: Boolean): String? {
        val p = prefs(context)
        val hasUsage = UsageStatsHelper.hasUsageAccess(context)
        val hasOverlay = PermissionsHelper.canDrawOverlays(context)
        val boot = bootCount(context)
        val uptime = SystemClock.uptimeMillis()

        if (!hasFriends) {
            // Sin amigos no hay quien desbloquee: desarmado y sin trampas pendientes.
            p.edit().putBoolean(KEY_ARMED, false).remove(KEY_REASON)
                .putLong(KEY_BEAT_UPTIME, uptime).putInt(KEY_BEAT_BOOT, boot).apply()
            return null
        }

        val armed = p.getBoolean(KEY_ARMED, false)
        val editor = p.edit()
        if (armed && savedReason(context) == null) {
            val lastBoot = p.getInt(KEY_BEAT_BOOT, Int.MIN_VALUE)
            val lastUptime = p.getLong(KEY_BEAT_UPTIME, uptime)
            val newReason = when {
                !hasUsage -> REASON_USAGE_ACCESS
                !hasOverlay -> REASON_OVERLAY
                boot != -1 && boot == lastBoot && uptime - lastUptime > MAX_GAP_MS -> REASON_SERVICE_STOPPED
                else -> null
            }
            if (newReason != null) editor.putString(KEY_REASON, newReason)
        }
        if (!armed && hasUsage && hasOverlay) editor.putBoolean(KEY_ARMED, true)
        editor.putLong(KEY_BEAT_UPTIME, uptime).putInt(KEY_BEAT_BOOT, boot).apply()

        if (!(armed || (hasUsage && hasOverlay))) return null
        return savedReason(context) ?: if (!isAutoTime(context)) REASON_MANUAL_CLOCK else null
    }

    /** True si el servicio debe arrancar aunque falten permisos, para poder registrar la trampa. */
    fun isArmed(context: Context): Boolean = prefs(context).getBoolean(KEY_ARMED, false)
}
