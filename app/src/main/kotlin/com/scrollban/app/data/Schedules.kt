package com.scrollban.app.data

import com.scrollban.app.data.db.AppLimit
import java.util.Calendar

/**
 * Horarios de cierre (Pro): una app puede quedar cerrada a ciertas horas aunque no se haya
 * gastado su tiempo. Los minutos se cuentan desde medianoche; si fin < inicio, el horario cruza
 * la medianoche (por ejemplo, de 23:00 a 8:00).
 */
object Schedules {

    fun minutesNow(now: Long = System.currentTimeMillis()): Int {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    fun inWindow(minute: Int, start: Int, end: Int): Boolean =
        if (start <= end) minute in start until end else minute >= start || minute < end

    /** True si la app está ahora mismo dentro de su horario de cierre. */
    fun isClosedNow(limit: AppLimit, now: Long = System.currentTimeMillis()): Boolean {
        val s = limit.scheduleStart ?: return false
        val e = limit.scheduleEnd ?: return false
        return limit.hasSchedule && inWindow(minutesNow(now), s, e)
    }

    fun format(minute: Int): String = "%d:%02d".format(minute / 60, minute % 60)

    fun describe(start: Int, end: Int): String = "de ${format(start)} a ${format(end)}"

    /** Duración del cierre en minutos (teniendo en cuenta si cruza la medianoche). */
    private fun length(start: Int, end: Int): Int = if (start <= end) end - start else 1440 - start + end

    /**
     * True si el horario nuevo cubre todo el antiguo: endurecer, así que no pide llave.
     * Quitar el horario o dejar alguna hora antes cerrada ahora abierta, sí la pide.
     */
    fun covers(newStart: Int, newEnd: Int, oldStart: Int, oldEnd: Int): Boolean {
        val oldLen = length(oldStart, oldEnd)
        return (0 until oldLen).all { inWindow((oldStart + it) % 1440, newStart, newEnd) }
    }
}
