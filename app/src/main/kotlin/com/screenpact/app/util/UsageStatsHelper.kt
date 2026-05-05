package com.screenpact.app.util

import android.app.AppOpsManager
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

    /** Devuelve milisegundos en primer plano de cada paquete *desde la medianoche local de hoy*. */
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

        // queryAndAggregateUsageStats already merges duplicate entries for the same
        // package and always measures from the exact startOfDay timestamp, so it does
        // not suffer from the stale-daily-bucket bug of queryUsageStats(INTERVAL_DAILY).
        val stats = usm.queryAndAggregateUsageStats(startOfDay, now)
        return stats
            .mapValues { it.value.totalTimeInForeground }
            .filter { it.value > 0 }
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
