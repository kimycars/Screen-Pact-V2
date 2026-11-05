package com.scrollban.app.data

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.scrollban.app.BuildConfig
import com.scrollban.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Estadísticas anónimas. Salen encendidas en la bienvenida (la persona puede apagarlas ahí o en
 * Ajustes); hasta llegar a ese paso no se envía nada.
 *
 * Qué se envía, una vez al día: un id aleatorio propio de las estadísticas (no es el id de
 * emparejamiento ni nada que identifique a la persona), versión de la app, versión de Android,
 * fabricante, cuántos amigos y límites hay, qué apps tienen límite (nombre de paquete, sin
 * minutos de uso) y cuántas veces pasó cada evento desde el último envío.
 * Qué NO se envía: nombres de amigos, llaves, minutos de uso ni nada que salga de otras apps.
 *
 * Toda la app pasa por aquí: para contar algo nuevo, [count] con un nombre de [Event].
 */
object Stats {

    private const val ENDPOINT = "https://www.kimycars.com/api/scrollban-stats"
    private const val PREFS = "screenpact_stats"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_ASKED = "asked"
    private const val KEY_ID = "stats_id"
    private const val WORK = "screenpact-stats"

    /** Eventos que se cuentan. El servidor solo acepta estos nombres. */
    enum class Event(val key: String) {
        ONBOARDING_DONE("onboarding_done"),
        PACT_SEALED("pact_sealed"),
        LIMIT_ADDED("limit_added"),
        BLOCK_SHOWN("block_shown"),
        TAMPER_BLOCK("tamper_block"),
        UNLOCKED("unlocked"),
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    /** True si ya se le preguntó al usuario (para no volver a insistir). */
    fun wasAsked(context: Context): Boolean = prefs(context).getBoolean(KEY_ASKED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).putBoolean(KEY_ASKED, true).apply()
        val wm = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<StatsWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            wm.cancelUniqueWork(WORK)
            // Al desactivar se borra todo lo pendiente y el id: si se reactiva, es un id nuevo.
            prefs(context).edit().remove(KEY_ID).apply()
            Event.entries.forEach { prefs(context).edit().remove("n_" + it.key).apply() }
        }
    }

    /** Suma uno a un evento. No hace nada si las estadísticas están desactivadas. */
    fun count(context: Context, event: Event) {
        if (!isEnabled(context)) return
        val p = prefs(context)
        p.edit().putInt("n_" + event.key, p.getInt("n_" + event.key, 0) + 1).apply()
    }

    private fun statsId(context: Context): String {
        val p = prefs(context)
        return p.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also { p.edit().putString(KEY_ID, it).apply() }
    }

    /** Envía el informe del día. Devuelve true si el servidor lo aceptó (y entonces pone los contadores a cero). */
    internal suspend fun send(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!isEnabled(context)) return@withContext true
        val db = AppDatabase.get(context)
        val limits = db.appLimitDao().getEnabled()
        val p = prefs(context)
        val events = JSONObject().apply { Event.entries.forEach { put(it.key, p.getInt("n_" + it.key, 0)) } }
        val body = JSONObject().apply {
            put("sid", statsId(context))
            put("app", BuildConfig.VERSION_NAME)
            put("sdk", Build.VERSION.SDK_INT)
            put("maker", Build.MANUFACTURER.lowercase().take(30))
            put("friends", db.friendDao().getAll().size)
            put("limits", limits.size)
            put("apps", JSONArray(limits.map { it.packageName.take(100) }.take(30)))
            put("events", events)
        }
        val ok = runCatching {
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        }.getOrDefault(false)
        if (ok) Event.entries.forEach { p.edit().remove("n_" + it.key).apply() }
        ok
    }
}

/** Trabajo diario de WorkManager que manda el informe; si falla, WorkManager lo reintenta. */
class StatsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = if (Stats.send(applicationContext)) Result.success() else Result.retry()
}
