package com.screenpact.app.data

import android.content.Context
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.AppLimit
import com.screenpact.app.data.db.Baseline
import com.screenpact.app.data.db.DayRecord
import com.screenpact.app.util.UsageStatsHelper

/**
 * Tiempo ganado: lo que has dejado de usar frente a tu media de antes del límite.
 *
 * - La media de cada app se mide una sola vez, la primera vez que tiene límite, con los 7 días
 *   anteriores ([ensureBaselines], lo llama el servicio). Así no se contamina con días ya limitados.
 * - Solo cuentan los días terminados (hoy no, porque a media tarde parecería que has ganado el día
 *   entero) y solo las apps con media medida.
 * - Es neto: si un día usas más que antes, ese día resta.
 */
object TimeWon {

    data class Summary(
        /** Días terminados que entran en la cuenta (como mucho [WINDOW_DAYS]). */
        val days: Int,
        /** Neto ganado en esos días. Puede ser negativo. */
        val wonMs: Long,
        /** Media diaria de antes y de ahora, en esos días y esas apps. */
        val beforePerDayMs: Long,
        val nowPerDayMs: Long,
        /** Neto ganado desde que empezaste. */
        val totalWonMs: Long,
    )

    const val WINDOW_DAYS = 7

    /** Mide la media de antes de cada app con límite que aún no la tenga. */
    suspend fun ensureBaselines(context: Context, db: AppDatabase, limits: List<AppLimit>) {
        val dao = db.baselineDao()
        val known = dao.getAll().map { it.packageName }.toSet()
        val missing = limits.map { it.packageName }.filter { it !in known }
        if (missing.isEmpty()) return
        val averages = UsageStatsHelper.dailyAverageMs(context, WINDOW_DAYS)
        missing.forEach { dao.insert(Baseline(it, averages[it] ?: 0L)) }
    }

    suspend fun summary(db: AppDatabase): Summary {
        val baselines = db.baselineDao().getAll().associate { it.packageName to it.avgDailyMs }
        val today = History.dayKey()
        val closed = db.dayRecordDao().since("0000-00-00").filter { it.day < today && it.packageName in baselines }
        val recentDays = closed.map { it.day }.distinct().sortedDescending().take(WINDOW_DAYS).toSet()
        val recent = closed.filter { it.day in recentDays }

        fun won(recs: List<DayRecord>) = recs.sumOf { baselines.getValue(it.packageName) - it.usedMs }
        val n = recentDays.size
        return Summary(
            days = n,
            wonMs = won(recent),
            beforePerDayMs = if (n == 0) 0 else recent.sumOf { baselines.getValue(it.packageName) } / n,
            nowPerDayMs = if (n == 0) 0 else recent.sumOf { it.usedMs } / n,
            totalWonMs = won(closed),
        )
    }
}
