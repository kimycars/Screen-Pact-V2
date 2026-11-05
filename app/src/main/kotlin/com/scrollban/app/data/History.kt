package com.scrollban.app.data

import com.scrollban.app.data.db.AppDatabase
import com.scrollban.app.data.db.AppLimit
import com.scrollban.app.data.db.DayRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Historial diario y rachas. Se registra siempre. Las rachas y la semana se ven gratis en Hoy;
 * el historial completo, los récords y las insignias son de Pro.
 * Dos rachas: días seguidos sin pedir llave, y días seguidos sin que ninguna app llegue a su límite.
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

    /** Cómo fue un día, para pintarlo en la tira de la semana y en el calendario. */
    enum class DayState {
        /** Sin datos (aún no usabas ScrollBan o no había límites). */
        EMPTY,
        /** Ninguna app llegó a su límite y no se pidió llave. */
        CLEAN,
        /** Alguna app llegó al límite, pero aguantaste sin pedir llave. */
        LIMIT,
        /** Se usó al menos una llave. */
        KEY,
    }

    data class Day(val key: String, val state: DayState, val usedMs: Long)

    /** Las dos rachas, sus récords y la siguiente meta. Hoy cuenta mientras siga limpio. */
    data class Streaks(
        val noKey: Int,
        val noLimit: Int,
        val bestNoKey: Int,
        val bestNoLimit: Int,
    ) {
        /** Siguiente meta de la racha sin pedir llave (null si ya pasó la última). */
        val nextGoal: Int? get() = GOALS.firstOrNull { it > noKey }
    }

    /** Metas de racha. Cada una que alcanzas es una insignia (Pro). */
    val GOALS = listOf(3, 7, 14, 30, 60, 100, 365)

    private fun stateOf(recs: List<DayRecord>): DayState = when {
        recs.isEmpty() -> DayState.EMPTY
        recs.any { it.unlocks > 0 } -> DayState.KEY
        recs.any { it.limitMinutes > 0 && it.usedMs >= it.limitMinutes * 60_000L } -> DayState.LIMIT
        else -> DayState.CLEAN
    }

    /** Los últimos [days] días (incluido hoy), del más antiguo al más reciente. */
    suspend fun days(db: AppDatabase, days: Int): List<Day> =
        lastDays(db, days).map { (day, recs) -> Day(day, stateOf(recs), recs.sumOf { it.usedMs }) }

    /** Calcula las dos rachas y sus récords con todo el historial. */
    suspend fun streaks(db: AppDatabase): Streaks {
        val byDay = db.dayRecordDao().since("0000-00-00").groupBy { it.day }
        if (byDay.isEmpty()) return Streaks(0, 0, 0, 0)
        // Todos los días desde el primero con datos hasta hoy; un día sin datos corta la racha.
        val first = byDay.keys.min()
        val states = ArrayList<DayState>()
        var i = 0
        while (true) {
            val d = daysAgo(i)
            states.add(stateOf(byDay[d] ?: emptyList()))
            if (d <= first) break
            i++
        }
        // states[0] es hoy. Racha actual: hoy cuenta si va bien; si hoy aún no hay datos, se mira desde ayer.
        fun current(ok: (DayState) -> Boolean): Int {
            val start = if (states[0] == DayState.EMPTY) 1 else 0
            return states.drop(start).takeWhile(ok).size
        }
        fun best(ok: (DayState) -> Boolean): Int {
            var run = 0; var max = 0
            for (st in states) { if (ok(st)) { run++; if (run > max) max = run } else run = 0 }
            return max
        }
        val noKey: (DayState) -> Boolean = { it == DayState.CLEAN || it == DayState.LIMIT }
        val noLimit: (DayState) -> Boolean = { it == DayState.CLEAN }
        return Streaks(current(noKey), current(noLimit), best(noKey), best(noLimit))
    }

    /** Uso total por día de los últimos [days] días (incluido hoy), del más antiguo al más reciente. */
    suspend fun lastDays(db: AppDatabase, days: Int): List<Pair<String, List<DayRecord>>> {
        val from = daysAgo(days - 1)
        val all = db.dayRecordDao().since(from).groupBy { it.day }
        return (days - 1 downTo 0).map { daysAgo(it) }.map { it to (all[it] ?: emptyList()) }
    }
}
