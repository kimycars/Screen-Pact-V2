package com.screenpact.app.util

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

    /**
     * Devuelve milisegundos *visibles* en primer plano de cada paquete desde la medianoche local.
     *
     * Implementación basada en eventos (FG/BG pairing) en lugar de
     * [UsageStats.totalTimeInForeground], que sobre-estima ~2× el tiempo real
     * porque incluye periodos en los que el proceso está en el bucket "foreground"
     * del scheduler aunque la UI ya no sea visible. Este algoritmo es equivalente
     * al que usa Digital Wellbeing del sistema.
     */
    fun foregroundTimeTodayMs(context: Context): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val events = usm.queryEvents(startOfDay, now)
        val ev = UsageEvents.Event()
        val openedAt = HashMap<String, Long>()  // pkg -> timestamp último MOVE_TO_FOREGROUND
        val totals   = HashMap<String, Long>()  // pkg -> ms acumulados

        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            val pkg = ev.packageName ?: continue
            when (ev.eventType) {
                // MOVE_TO_FOREGROUND (=1) == ACTIVITY_RESUMED en API 29+ (mismo valor entero,
                // disponible desde API 21). Usamos el nombre antiguo para soportar minSdk 26.
                @Suppress("DEPRECATION")
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    openedAt[pkg] = ev.timeStamp
                }
                // MOVE_TO_BACKGROUND (=2) == ACTIVITY_PAUSED.
                @Suppress("DEPRECATION")
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val opened = openedAt.remove(pkg) ?: continue
                    val delta = ev.timeStamp - opened
                    if (delta > 0) totals.merge(pkg, delta) { a, b -> a + b }
                }
            }
        }

        // Cola: el usuario está justo ahora dentro de la app y no hay evento de cierre.
        for ((pkg, opened) in openedAt) {
            val delta = now - opened
            if (delta > 0) totals.merge(pkg, delta) { a, b -> a + b }
        }

        return totals
    }

    /**
     * Paquete actualmente en primer plano, usando una máquina de estados FG/BG sobre
     * todos los eventos del día.
     *
     * El enfoque anterior usaba solo una ventana de 60 segundos, lo que hacía que
     * devolviera null cuando el usuario llevaba más de 60 s en la misma app
     * (el evento MOVE_TO_FOREGROUND salía de la ventana). Consecuencias:
     *   • El overlay no reaparecía tras expirar el grace period mientras el usuario
     *     permanecía en la app (UsageMonitorService recibía current=null y omitía la comprobación).
     *   • Al salir y volver, el nuevo MOVE_TO_FOREGROUND sí caía en la ventana y el
     *     overlay reaparecía de inmediato, aunque el grace period aún fuera válido.
     *
     * La solución es iterar todos los eventos desde startOfDay y hacer tracking de estado:
     * FOREGROUND establece el paquete activo; BACKGROUND lo limpia si coincide.
     * El valor final refleja qué app está ahora mismo en primer plano.
     */
    fun currentForegroundPackage(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val events = usm.queryEvents(startOfDay, now)
        val ev = UsageEvents.Event()
        var fg: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            @Suppress("DEPRECATION")
            when (ev.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> fg = ev.packageName   // == ACTIVITY_RESUMED (misma constante)
                UsageEvents.Event.MOVE_TO_BACKGROUND ->                        // == ACTIVITY_PAUSED
                    if (fg == ev.packageName) fg = null
            }
        }
        return fg
    }
}
