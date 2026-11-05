package com.scrollban.app.data

import com.scrollban.app.data.db.AppDatabase
import com.scrollban.app.data.db.Challenge
import com.scrollban.app.data.db.DayRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Retos con fecha ("una semana sin TikTok"). Un reto no bloquea nada por sí mismo: mide cada
 * día con el mismo registro diario que las rachas. Si la app deja de tener límite a mitad del
 * reto, esos días cuentan como no cumplidos (no hay datos para demostrarlo).
 */
object Challenges {

    /** Duraciones y topes que se ofrecen al crear un reto. */
    val DAY_OPTIONS = listOf(3, 7, 14)
    val MINUTE_OPTIONS = listOf(0, 15, 30)

    /** Margen para que abrir la app un segundo por error no tumbe un reto de "0 minutos". */
    private const val TOLERANCE_MS = 59_999L

    enum class DayResult { DONE, FAILED, TODAY_OK, TODAY_OVER, UPCOMING }

    data class Progress(
        val challenge: Challenge,
        val days: List<Pair<String, DayResult>>,
    ) {
        val done: Int get() = days.count { it.second == DayResult.DONE }
        val failed: Int get() = days.count { it.second == DayResult.FAILED }
        val finished: Boolean get() = days.none { it.second == DayResult.UPCOMING || it.second == DayResult.TODAY_OK || it.second == DayResult.TODAY_OVER }
        val won: Boolean get() = finished && failed == 0
        /** Número del día de hoy dentro del reto (1…días), o null si aún no ha empezado o ya acabó. */
        val todayIndex: Int? get() = days.indexOfFirst { it.second == DayResult.TODAY_OK || it.second == DayResult.TODAY_OVER }.takeIf { it >= 0 }?.plus(1)
    }

    fun title(c: Challenge): String =
        if (c.dailyMinutes == 0) "${c.days} días sin ${c.appName}"
        else "${c.days} días con ${c.appName} a ${c.dailyMinutes} min o menos"

    /**
     * Crea el reto. Empieza hoy si hoy aún vas dentro del tope; si no, mañana, para no nacer
     * con un día perdido. Devuelve el reto creado.
     */
    suspend fun start(db: AppDatabase, packageName: String, appName: String, dailyMinutes: Int, days: Int): Challenge {
        val c = Challenge(UUID.randomUUID().toString(), packageName, appName, dailyMinutes, firstDay(db, packageName, dailyMinutes), days)
        db.challengeDao().upsert(c)
        return c
    }

    /**
     * Se une al reto de un amigo: el mismo reto (mismo id, app y tope), medido en tu móvil. Si ya
     * había empezado, para ti cuenta desde que te unes (hoy, o mañana si hoy ya te has pasado) y
     * termina el mismo día que el de tu amigo. Devuelve false si ya no le quedan días.
     */
    suspend fun join(db: AppDatabase, c: Challenge): Boolean {
        val today = History.dayKey()
        val mine = if (c.startDay >= today) c else {
            val start = firstDay(db, c.packageName, c.dailyMinutes)
            val left = daysLeftFrom(c, start)
            if (left < 1) return false
            c.copy(startDay = start, days = left)
        }
        db.challengeDao().upsert(mine.copy(createdAt = System.currentTimeMillis()))
        return true
    }

    /** Días del reto [c] que quedan contando desde [day] (incluido). */
    fun daysLeftFrom(c: Challenge, day: String): Int = dayKeys(c.startDay, c.days).count { it >= day }

    /** Hoy si hoy aún vas dentro del tope; si no, mañana, para no nacer con un día perdido. */
    private suspend fun firstDay(db: AppDatabase, packageName: String, dailyMinutes: Int): String {
        val today = History.dayKey()
        val usedToday = db.dayRecordDao().get(today, packageName)?.usedMs ?: 0L
        return if (usedToday <= capMs(dailyMinutes)) today else History.daysAgo(-1)
    }

    suspend fun progress(db: AppDatabase, c: Challenge): Progress = progressOf(c, db.dayRecordDao().since(c.startDay))

    /** El progreso con los días de cualquier persona (tú, o un amigo con los datos del servidor). */
    fun progressOf(c: Challenge, dayRecords: List<DayRecord>): Progress {
        val today = History.dayKey()
        val records = dayRecords.filter { it.packageName == c.packageName && it.day >= c.startDay }.associateBy { it.day }
        val cap = capMs(c.dailyMinutes)
        val days = dayKeys(c.startDay, c.days).map { day ->
            val used = records[day]?.usedMs
            day to when {
                day > today -> DayResult.UPCOMING
                day == today -> if ((used ?: 0L) <= cap) DayResult.TODAY_OK else DayResult.TODAY_OVER
                used != null && used <= cap -> DayResult.DONE
                else -> DayResult.FAILED
            }
        }
        return Progress(c, days)
    }

    /** Texto para compartir el resultado por el chat (mientras no haya grupos en el servidor). */
    fun shareText(p: Progress): String = when {
        p.won -> "Reto superado: ${title(p.challenge)}. ¿Te apuntas al siguiente?"
        p.finished -> "Terminé el reto «${title(p.challenge)}»: ${p.done} de ${p.days.size} días. El próximo cae."
        else -> "Voy por el día ${p.todayIndex ?: 1} de ${p.days.size} del reto «${title(p.challenge)}». ¿Te apuntas?"
    }

    private fun capMs(minutes: Int) = minutes * 60_000L + TOLERANCE_MS

    private fun dayKeys(start: String, count: Int): List<String> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance().apply { time = fmt.parse(start)!! }
        return List(count) { i -> if (i > 0) cal.add(Calendar.DAY_OF_YEAR, 1); fmt.format(cal.time) }
    }
}
