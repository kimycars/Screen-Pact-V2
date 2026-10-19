package com.screenpact.app.data

import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.AppLimit
import com.screenpact.app.data.db.DayRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Historial diario y rachas (Pro). Se registra siempre; solo verlo es de Pro.
 * Racha = días seguidos sin pedir ninguna llave, contando hoy si hoy aún no se ha pedido.
 */
object History {

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun dayKey(millis: Long = System.currentTimeMillis()): String = dayFormat.format(Date(millis))

    fun daysAgo(n: Int): String = dayKey(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -n) }.timeInMillis)

    /** Guarda el uso de hoy de cada app con límite (lo llama el servicio cada minuto). */
    suspend fun recordToday(db: AppDatabase, limits: List<AppLimit>, todayMs: Map<String, Long>) {
        val day = dayKey()
        val dao = db.dayRecordDao()
        for (l in limits) {
            val old = dao.get(day, l.packageName)
            dao.upsert(
                DayRecord(
                    day = day,
                    packageName = l.packageName,
                    usedMs = todayMs[l.packageName] ?: 0L,
                    limitMinutes = l.dailyLimitMinutes,
                    unlocks = old?.unlocks ?: 0
                )
            )
        }
    }

    /** Suma una llave usada hoy en esa app. */
    suspend fun countUnlock(db: AppDatabase, pkg: String) {
        val day = dayKey()
        val dao = db.dayRecordDao()
        val old = dao.get(day, pkg) ?: DayRecord(day, pkg)
        dao.upsert(old.copy(unlocks = old.unlocks + 1))
    }

    suspend fun streak(db: AppDatabase): Int {
        val dao = db.dayRecordDao()
        val recorded = dao.days().toSet()
        var n = 0
        // Hoy cuenta si aún no se ha pedido llave; si se pidió, la racha de hoy es 0.
        if (dao.unlocksOn(dayKey()) > 0) return 0
        if (dayKey() in recorded) n++
        var i = 1
        while (true) {
            val d = daysAgo(i)
            if (d !in recorded || dao.unlocksOn(d) > 0) break
            n++; i++
        }
        return n
    }

    /** Uso total por día de los últimos [days] días (incluido hoy), del más antiguo al más reciente. */
    suspend fun lastDays(db: AppDatabase, days: Int): List<Pair<String, List<DayRecord>>> {
        val from = daysAgo(days - 1)
        val all = db.dayRecordDao().since(from).groupBy { it.day }
        return (days - 1 downTo 0).map { daysAgo(it) }.map { it to (all[it] ?: emptyList()) }
    }
}
