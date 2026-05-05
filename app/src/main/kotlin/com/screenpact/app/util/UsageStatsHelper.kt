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

    /** Paquete actualmente en primer plano (basado en eventos recientes). Null si desconocido. */
    fun currentForegroundPackage(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 60_000L, now)
        val ev = android.app.usage.UsageEvents.Event()
        var lastPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            if (ev.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND ||
                ev.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                lastPkg = ev.packageName
            }
        }
        return lastPkg
    }
}
