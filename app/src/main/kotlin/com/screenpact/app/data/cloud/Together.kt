package com.screenpact.app.data.cloud

import android.content.Context
import com.screenpact.app.data.History
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.Challenge
import com.screenpact.app.data.db.DayRecord
import com.screenpact.app.data.db.Friend
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64

/**
 * Juntos en el servidor: el ranking, el tiempo ganado del grupo y los retos de tus amigos.
 *
 * Es opcional y empieza apagado. Al encenderlo, tus minutos por app (de las apps con límite),
 * tus llaves usadas, tu media de antes y tu reto los ve solo la gente con la que tienes pacto.
 * Al apagarlo se borra todo lo tuyo del servidor.
 *
 * Cada pacto es un grupo de 2. Los dos móviles calculan por su cuenta el mismo id de grupo y el
 * mismo secreto a partir de las llaves del pacto, así que no hace falta mandar nada más. El
 * servidor solo guarda el hash del secreto y nunca ve las llaves.
 */
object Together {

    private const val PREFS = "scrollban_together"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_JOINED = "joined"
    private const val KEY_LAST_PUSH = "last_push"

    /** Cada cuánto se suben los minutos mientras el móvil se usa. */
    private const val PUSH_EVERY_MS = 15 * 60_000L

    /** Días que se suben en cada envío (hoy y los 7 anteriores, por si un envío falló). */
    private const val PUSH_DAYS = 8

    /** Días que se piden al servidor (el tiempo ganado del grupo mira 30). */
    const val BOARD_DAYS = 31

    private val syncLock = Mutex()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    /** Enciende Juntos y hace la primera subida. Lanza excepción si no hay conexión. */
    suspend fun enable(context: Context) {
        prefs(context).edit().putBoolean(KEY_ENABLED, true).apply()
        sync(context, force = true)
    }

    /** Apaga Juntos y borra del servidor todo lo de esta persona. Lanza excepción si no hay conexión. */
    suspend fun disable(context: Context) {
        if (Cloud.hasSession(context)) Cloud.rpc(context, "delete_my_data")
        Cloud.forget(context)
        prefs(context).edit().clear().apply()
    }

    /** Id del grupo y secreto del pacto con [friend], iguales en los dos móviles. */
    fun groupOf(friend: Friend): Pair<String, String> {
        // Un móvil tiene (mía, suya) y el otro (suya, mía): ordenadas, son las mismas.
        val (a, b) = listOf(friend.verifyKey, friend.generateKey).sortedBy { Base64.getEncoder().encodeToString(it) }
        fun hash(label: String) = MessageDigest.getInstance("SHA-256").digest(label.toByteArray() + a + b)
        val g = hash("scrollban-group:")
        g[6] = ((g[6].toInt() and 0x0f) or 0x40).toByte()   // UUID versión 4
        g[8] = ((g[8].toInt() and 0x3f) or 0x80).toByte()
        val hex = g.copyOf(16).joinToString("") { "%02x".format(it) }
        val id = "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
        val secret = hash("scrollban-join:").joinToString("") { "%02x".format(it) }
        return id to secret
    }

    /** Lo llama el servicio cada minuto: sube los minutos si toca. Nunca lanza excepción. */
    suspend fun maybeSync(context: Context) {
        if (!isEnabled(context)) return
        if (System.currentTimeMillis() - prefs(context).getLong(KEY_LAST_PUSH, 0L) < PUSH_EVERY_MS) return
        runCatching { sync(context, force = false) }
    }

    /** Entra en los grupos de los pactos nuevos y sube los minutos y el reto. */
    suspend fun sync(context: Context, force: Boolean) = syncLock.withLock {
        if (!isEnabled(context)) return@withLock
        val p = prefs(context)
        if (!force && System.currentTimeMillis() - p.getLong(KEY_LAST_PUSH, 0L) < PUSH_EVERY_MS) return@withLock
        val db = AppDatabase.get(context)

        val joined = p.getStringSet(KEY_JOINED, emptySet())!!.toMutableSet()
        for (f in db.friendDao().getAll()) {
            val (id, secret) = groupOf(f)
            if (id in joined) continue
            try {
                Cloud.rpc(context, "join_group", JSONObject().put("p_group", id).put("p_secret", secret))
            } catch (e: Cloud.CloudException) {
                // 400: el servidor rechaza el grupo (lleno, o demasiados grupos). Se sigue con el resto.
                if (e.code == 400) continue else throw e
            }
            joined += id
            p.edit().putStringSet(KEY_JOINED, joined.toSet()).apply()
        }

        val names = db.appLimitDao().observeAll().first().associate { it.packageName to it.appName }
        val baselines = db.baselineDao().getAll().associate { it.packageName to it.avgDailyMs }
        val usage = JSONArray()
        db.dayRecordDao().since(History.daysAgo(PUSH_DAYS - 1)).forEach { r ->
            usage.put(JSONObject().apply {
                put("day", r.day)
                put("package", r.packageName)
                put("app_name", names[r.packageName] ?: r.packageName)
                put("used_ms", r.usedMs.coerceIn(0L, 86_400_000L))
                put("limit_min", r.limitMinutes)
                put("unlocks", r.unlocks)
                put("baseline_ms", baselines[r.packageName] ?: JSONObject.NULL)
            })
        }
        val challenges = JSONArray()
        db.challengeDao().observeLatest().first()?.let { c ->
            challenges.put(JSONObject().apply {
                put("id", c.id)
                put("package", c.packageName)
                put("app_name", c.appName)
                put("daily_minutes", c.dailyMinutes)
                put("start_day", c.startDay)
                put("days", c.days)
            })
        }
        Cloud.rpc(context, "push_usage", JSONObject().put("p_usage", usage).put("p_challenges", challenges))
        p.edit().putLong(KEY_LAST_PUSH, System.currentTimeMillis()).apply()
    }

    /** Al romper un pacto: sale de su grupo. Si no hay conexión, no pasa nada grave (el grupo deja de verse en la app). */
    suspend fun leave(context: Context, friend: Friend) {
        if (!isEnabled(context)) return
        val (id, _) = groupOf(friend)
        runCatching { Cloud.rpc(context, "leave_group", JSONObject().put("p_group", id)) }
        val p = prefs(context)
        p.edit().putStringSet(KEY_JOINED, p.getStringSet(KEY_JOINED, emptySet())!! - id).apply()
    }

    /** Lo que se sabe de una persona del grupo: sus días, su media de antes por app y su reto. */
    data class Person(
        val records: List<DayRecord>,
        val baselines: Map<String, Long>,
        val challenges: List<Challenge>,
    )

    /** Tú y cada amigo con el que compartes grupo (por id local del amigo). */
    data class Board(val me: Person, val friends: Map<Long, Person>)

    /** Pide al servidor lo de tus amigos. Lanza excepción si no hay conexión. */
    suspend fun board(context: Context): Board {
        val json = JSONObject(Cloud.rpc(context, "my_board", JSONObject().put("p_days", BOARD_DAYS)))
        val me = json.getString("me")

        val records = HashMap<String, MutableList<DayRecord>>()
        val baselines = HashMap<String, MutableMap<String, Long>>()
        json.getJSONArray("usage").forEachObject { o ->
            val u = o.getString("u")
            val pkg = o.getString("package")
            records.getOrPut(u) { mutableListOf() } += DayRecord(
                day = o.getString("day"),
                packageName = pkg,
                usedMs = o.getLong("used_ms"),
                limitMinutes = o.getInt("limit_min"),
                unlocks = o.getInt("unlocks")
            )
            if (!o.isNull("baseline_ms")) baselines.getOrPut(u) { mutableMapOf() }[pkg] = o.getLong("baseline_ms")
        }
        val challenges = HashMap<String, MutableList<Challenge>>()
        json.getJSONArray("challenges").forEachObject { o ->
            challenges.getOrPut(o.getString("u")) { mutableListOf() } += Challenge(
                id = o.getString("id"),
                packageName = o.getString("package"),
                appName = o.getString("app_name"),
                dailyMinutes = o.getInt("daily_minutes"),
                startDay = o.getString("start_day"),
                days = o.getInt("days")
            )
        }
        fun person(u: String) = Person(records[u] ?: emptyList(), baselines[u] ?: emptyMap(), challenges[u] ?: emptyList())

        val membersByGroup = HashMap<String, MutableList<String>>()
        json.getJSONArray("members").forEachObject { o -> membersByGroup.getOrPut(o.getString("g")) { mutableListOf() } += o.getString("u") }

        val friends = HashMap<Long, Person>()
        for (f in AppDatabase.get(context).friendDao().getAll()) {
            val other = membersByGroup[groupOf(f).first]?.firstOrNull { it != me } ?: continue
            friends[f.id] = person(other)
        }
        return Board(person(me), friends)
    }

    private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (i in 0 until length()) block(getJSONObject(i))
    }
}

/** Uso de las apps con límite en los últimos 7 días, hoy incluido. Es lo que ordena el ranking. */
fun Together.Person.weekUsedMs(): Long {
    val from = History.daysAgo(6)
    return records.filter { it.day >= from }.sumOf { it.usedMs }
}

/** True si esta persona ya ha subido algo (si no, aún no ha encendido Juntos). */
val Together.Person.hasData: Boolean get() = records.isNotEmpty()
