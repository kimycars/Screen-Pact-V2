package com.scrollban.app.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

object UsageStatsHelper {

    fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Lo que el servicio necesita en cada vuelta: app visible ahora y uso de hoy por paquete. */
    data class Snapshot(val foreground: String?, val todayMs: Map<String, Long>)

    // Constantes de UsageEvents.Event como literales: varias son de API 28/29 y minSdk es 26.
    // En móviles antiguos esos eventos simplemente no llegan y la lógica sigue siendo válida.
    private const val ACTIVITY_RESUMED = 1        // == MOVE_TO_FOREGROUND
    private const val ACTIVITY_PAUSED = 2         // == MOVE_TO_BACKGROUND
    private const val SCREEN_INTERACTIVE = 15
    private const val SCREEN_NON_INTERACTIVE = 16
    private const val KEYGUARD_SHOWN = 17
    private const val KEYGUARD_HIDDEN = 18
    private const val ACTIVITY_STOPPED = 23
    private const val DEVICE_SHUTDOWN = 26
    private const val DEVICE_STARTUP = 27

    /** Margen antes de medianoche para saber qué estaba abierto al empezar el día. */
    private const val LOOKBACK_MS = 6 * 60 * 60_000L

    fun foregroundTimeTodayMs(context: Context): Map<String, Long> = snapshot(context).todayMs

    fun currentForegroundPackage(context: Context): String? = snapshot(context).foreground

    /**
     * Reconstruye el uso de hoy con una máquina de estados sobre los eventos del sistema,
     * con el mismo criterio que Bienestar Digital: solo cuenta el tiempo en que la app está
     * en primer plano, la pantalla encendida y el móvil desbloqueado.
     *
     * Por qué así (bug de ~8 min de más en el límite):
     *  - El emparejado simple RESUMED/PAUSED por paquete seguía contando con la pantalla
     *    apagada o en la pantalla de bloqueo si el sistema no emitía PAUSED (pasa con bloqueo
     *    retardado, Smart Lock, vídeo, etc.).
     *  - Si un PAUSED se perdía, la sesión quedaba abierta y se sumaba hasta "ahora".
     *
     * Modelo: hay como mucho una app en primer plano. Abrir otra app cierra la anterior,
     * así que un PAUSED perdido ya no infla el total. Apagar pantalla o bloquear pausa el
     * contador; encender/desbloquear lo reanuda si la app sigue arriba.
     */
    fun snapshot(context: Context): Snapshot {
        val now = System.currentTimeMillis()
        val startOfDay = startOfDay(now)
        val totals = HashMap<String, Long>()
        val visibleFg = replay(context, startOfDay - LOOKBACK_MS, now) { pkg, from, to ->
            // Solo cuenta la parte del tramo que cae dentro de hoy.
            val delta = to - maxOf(from, startOfDay)
            if (delta > 0) totals.merge(pkg, delta) { x, y -> x + y }
        }
        return Snapshot(foreground = visibleFg, todayMs = totals)
    }

    /**
     * Media diaria de uso de cada app en los [days] días completos anteriores a hoy, con el mismo
     * criterio que el uso de hoy. Sirve para sugerir límites con datos reales del móvil.
     */
    fun dailyAverageMs(context: Context, days: Int = 7): Map<String, Long> {
        val today = startOfDay(System.currentTimeMillis())
        val from = Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis
        val totals = HashMap<String, Long>()
        replay(context, from - LOOKBACK_MS, today) { pkg, a, b ->
            val delta = b - maxOf(a, from)
            if (delta > 0) totals.merge(pkg, delta) { x, y -> x + y }
        }
        return totals.mapValues { it.value / days }
    }

    private fun startOfDay(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * Recorre los eventos entre [from] y [until] con la máquina de estados de arriba y llama a
     * [onSegment] con cada tramo en que una app estuvo delante, con pantalla encendida y móvil
     * desbloqueado. Devuelve la app que sigue visible en [until] (o null).
     */
    private fun replay(context: Context, from: Long, until: Long, onSegment: (String, Long, Long) -> Unit): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(from, until)
        val ev = UsageEvents.Event()

        var fgPkg: String? = null
        val fgActivities = HashSet<String>()   // actividades de fgPkg en estado resumed
        var screenOn = true
        var locked = false
        var segmentStart: Long? = null         // != null mientras se está contando

        fun close(at: Long) {
            val pkg = fgPkg
            val start = segmentStart
            if (pkg != null && start != null && at > start) onSegment(pkg, start, at)
            segmentStart = null
        }

        fun openIfVisible(at: Long) {
            if (segmentStart == null && fgPkg != null && screenOn && !locked) segmentStart = at
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            val t = ev.timeStamp
            when (ev.eventType) {
                ACTIVITY_RESUMED -> {
                    val pkg = ev.packageName ?: continue
                    if (pkg != fgPkg) {
                        close(t)
                        fgPkg = pkg
                        fgActivities.clear()
                    }
                    fgActivities.add(ev.className ?: "")
                    openIfVisible(t)
                }
                ACTIVITY_PAUSED, ACTIVITY_STOPPED -> {
                    if (ev.packageName != fgPkg) continue
                    fgActivities.remove(ev.className ?: "")
                    if (fgActivities.isEmpty()) {
                        close(t)
                        fgPkg = null
                    }
                }
                SCREEN_NON_INTERACTIVE -> { close(t); screenOn = false }
                SCREEN_INTERACTIVE -> { screenOn = true; openIfVisible(t) }
                KEYGUARD_SHOWN -> { close(t); locked = true }
                KEYGUARD_HIDDEN -> { locked = false; openIfVisible(t) }
                DEVICE_SHUTDOWN, DEVICE_STARTUP -> {
                    close(t)
                    fgPkg = null
                    fgActivities.clear()
                    screenOn = true
                    locked = false
                }
            }
        }

        val visible = segmentStart != null
        close(until)
        return if (visible) fgPkg else null
    }

    /**
     * True si desde [sinceMs] el usuario ha dejado [pkg]: otra app se ha puesto delante, la app
     * bloqueada se ha ido a segundo plano (gesto de inicio o de recientes) o la pantalla se ha
     * apagado o bloqueado. Solo lee los eventos de ese intervalo, así que es barato y se puede
     * llamar varias veces por segundo mientras la pantalla de bloqueo está visible.
     *
     * La pausa de [pkg] es la señal más temprana del gesto de inicio: llega antes de que el
     * escritorio se ponga delante. Si la app vuelve a ponerse delante en el mismo intervalo
     * (cambio entre pantallas de la propia app), no cuenta como salida.
     */
    fun leftApp(context: Context, pkg: String, sinceMs: Long): Boolean {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(sinceMs, System.currentTimeMillis())
        val ev = UsageEvents.Event()
        var paused = false
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            when (ev.eventType) {
                ACTIVITY_RESUMED -> when (ev.packageName) {
                    pkg -> paused = false
                    context.packageName -> {}
                    else -> return true
                }
                ACTIVITY_PAUSED -> if (ev.packageName == pkg) paused = true
                SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN -> return true
            }
        }
        return paused
    }
}
