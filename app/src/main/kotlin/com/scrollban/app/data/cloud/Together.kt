package com.scrollban.app.data.cloud

import android.content.Context
import com.scrollban.app.data.History
import com.scrollban.app.data.Prefs
import com.scrollban.app.data.crypto.GroupKeys
import com.scrollban.app.data.crypto.PactKeys
import com.scrollban.app.data.db.AppDatabase
import com.scrollban.app.data.db.Challenge
import com.scrollban.app.data.db.DayRecord
import com.scrollban.app.data.db.Friend
import com.scrollban.app.data.db.PactGroup
import java.util.UUID
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
 *
 * Además hay grupos de 3 o más ([PactGroup]). Quien lo crea inventa su secreto; para meter a un
 * amigo con pacto, se lo manda cifrado con la clave de su pacto a través del servidor
 * ([invite]). Cada miembro sube su nombre cifrado con el secreto del grupo, así el resto sabe
 * quién es aunque no tenga pacto con él (ver data/crypto/GroupKeys.kt).
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

    /** Largo máximo del nombre de un grupo y del de una persona. */
    const val MAX_GROUP_NAME = 30
    private const val MAX_NAME = 40

    private val syncLock = Mutex()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    /**
     * Enciende Juntos sin esperar a la red (bienvenida): la primera subida la hace el servicio en
     * el siguiente minuto (maybeSync). Es la persona quien lo elige; nunca se enciende solo.
     */
    fun turnOn(context: Context) = prefs(context).edit().putBoolean(KEY_ENABLED, true).apply()

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
        val before = joined.toSet()
        val toJoin = db.friendDao().getAll().filter { !it.ios }.map { groupOf(it) } +
            db.pactGroupDao().getAll().map { it.id to GroupKeys.joinSecret(it.secret) }
        for ((id, secret) in toJoin) {
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
        // Tu nombre, cifrado para el resto de cada grupo de 3 o más.
        // Al entrar, y en cada subida forzada por si ha cambiado el nombre.
        db.pactGroupDao().getAll().filter { it.id in joined && (force || it.id !in before) }.forEach { setProfile(context, it) }

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

    private suspend fun setProfile(context: Context, g: PactGroup) {
        val profile = JSONObject().put("n", Prefs.getOwnName(context).ifBlank { "Sin nombre" }).put("i", Prefs.getInstallId(context))
        Cloud.rpc(context, "set_profile", JSONObject().put("p_group", g.id).put("p_profile", GroupKeys.sealForGroup(g.secret, profile.toString())))
    }

    private fun markJoined(context: Context, id: String, joinedNow: Boolean) {
        val p = prefs(context)
        val set = p.getStringSet(KEY_JOINED, emptySet())!!
        p.edit().putStringSet(KEY_JOINED, if (joinedNow) set + id else set - id).apply()
    }

    /** Entra en un grupo del servidor y sube tu nombre. Lanza excepción si falla (400 = lleno). */
    private suspend fun joinNow(context: Context, g: PactGroup) {
        Cloud.rpc(context, "join_group", JSONObject().put("p_group", g.id).put("p_secret", GroupKeys.joinSecret(g.secret)))
        markJoined(context, g.id, true)
        setProfile(context, g)
    }

    /** Crea un grupo de 3 o más llamado [name]. Lanza excepción si no hay conexión. */
    suspend fun createGroup(context: Context, name: String): PactGroup {
        val g = PactGroup(UUID.randomUUID().toString(), name.trim().take(MAX_GROUP_NAME), GroupKeys.newSecret())
        val dao = AppDatabase.get(context).pactGroupDao()
        dao.insert(g)
        try {
            joinNow(context, g)
        } catch (e: Exception) {
            dao.delete(g.id)
            throw e
        }
        return g
    }

    /** Invita a [friend] (con pacto, y en el servidor como [friendUid]) al grupo [g]. */
    suspend fun invite(context: Context, g: PactGroup, friend: Friend, friendUid: String) {
        val box = GroupKeys.sealForFriend(friend, JSONObject().put("g", g.id).put("s", PactKeys.b64(g.secret)).put("n", g.name).toString())
        Cloud.rpc(context, "invite_to_group", JSONObject().put("p_group", g.id).put("p_to", friendUid).put("p_box", box))
    }

    /** Acepta una invitación y entra en el grupo. Lanza excepción si falla (400 = el grupo está lleno). */
    suspend fun accept(context: Context, inv: GroupInvite) {
        val g = PactGroup(inv.groupId, inv.name, inv.secret)
        val dao = AppDatabase.get(context).pactGroupDao()
        // Primero en el móvil: si se corta la conexión a medias, la próxima subida termina de entrar.
        dao.insert(g)
        try {
            joinNow(context, g)
        } catch (e: Cloud.CloudException) {
            if (e.code == 400) dao.delete(g.id)
            throw e
        }
    }

    suspend fun decline(context: Context, inv: GroupInvite) {
        Cloud.rpc(context, "decline_invite", JSONObject().put("p_group", inv.groupId))
    }

    /** Sale de un grupo de 3 o más. Sin conexión sale igual en el móvil: sus llaves dejan de valer. */
    suspend fun leaveGroup(context: Context, g: PactGroup) {
        if (isEnabled(context)) runCatching { Cloud.rpc(context, "leave_group", JSONObject().put("p_group", g.id)) }
        markJoined(context, g.id, false)
        AppDatabase.get(context).pactGroupDao().delete(g.id)
    }

    /** Al romper un pacto: sale de su grupo. Si no hay conexión, no pasa nada grave (el grupo deja de verse en la app). */
    suspend fun leave(context: Context, friend: Friend) {
        if (!isEnabled(context)) return
        val (id, _) = groupOf(friend)
        runCatching { Cloud.rpc(context, "leave_group", JSONObject().put("p_group", id)) }
        markJoined(context, id, false)
    }

    /** Lo que se sabe de una persona del grupo: sus días, su media de antes por app y su reto. */
    data class Person(
        val records: List<DayRecord>,
        val baselines: Map<String, Long>,
        val challenges: List<Challenge>,
    )

    /** Alguien de un grupo de 3 o más. [friendId]: si también tienes pacto con esa persona. */
    data class Member(val uid: String, val name: String, val installId: String?, val friendId: Long?)

    /** Invitación a un grupo de 3 o más, ya descifrada. [from] es el amigo que invita. */
    class GroupInvite(val groupId: String, val name: String, val secret: ByteArray, val from: Friend)

    /**
     * Lo que se ve en Juntos.
     * [friends]: los datos de cada amigo con pacto (por id local del amigo); [friendUids], su id
     * en el servidor. [groups]: el resto de miembros de cada grupo de 3 o más (sin contarte).
     * [people]: los datos de todas las personas con las que compartes algo, por id del servidor.
     */
    data class Board(
        val me: Person,
        val friends: Map<Long, Person>,
        val friendUids: Map<Long, String> = emptyMap(),
        val groups: Map<String, List<Member>> = emptyMap(),
        val people: Map<String, Person> = emptyMap(),
        val invites: List<GroupInvite> = emptyList(),
    )

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

        // Por grupo: (id del miembro, su nombre cifrado si es un grupo de 3 o más).
        val membersByGroup = HashMap<String, MutableList<Pair<String, String?>>>()
        json.getJSONArray("members").forEachObject { o ->
            val box = if (o.isNull("p")) null else o.getString("p")
            membersByGroup.getOrPut(o.getString("g")) { mutableListOf() } += o.getString("u") to box
        }

        val db = AppDatabase.get(context)
        val allFriends = db.friendDao().getAll()
        val friends = HashMap<Long, Person>()
        val friendUids = HashMap<Long, String>()
        for (f in allFriends) {
            val other = membersByGroup[groupOf(f).first]?.firstOrNull { it.first != me }?.first ?: continue
            friends[f.id] = person(other)
            friendUids[f.id] = other
        }
        val friendByUid = allFriends.filter { it.id in friendUids }.associateBy { friendUids.getValue(it.id) }

        val groups = HashMap<String, List<Member>>()
        for (g in db.pactGroupDao().getAll()) {
            groups[g.id] = membersByGroup[g.id].orEmpty().filter { it.first != me }.map { (uid, box) ->
                val profile = box?.let { GroupKeys.openFromGroup(g.secret, it) }?.let { runCatching { JSONObject(it) }.getOrNull() }
                val friend = friendByUid[uid]
                val name = friend?.name ?: profile?.optString("n")?.trim()?.take(MAX_NAME)?.ifBlank { null } ?: "Alguien"
                Member(uid, name, profile?.optString("i")?.ifBlank { null }, friend?.id)
            }
        }
        val people = (friendUids.values + groups.values.flatten().map { it.uid }).toSet().associateWith { person(it) }

        // Invitaciones: solo valen las de un amigo con pacto y que se abren con la clave de ese pacto.
        val invites = ArrayList<GroupInvite>()
        json.optJSONArray("invites")?.forEachObject { o ->
            val from = friendByUid[o.getString("from")] ?: return@forEachObject
            val inner = GroupKeys.openFromFriend(from, o.getString("box"))?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return@forEachObject
            if (inner.optString("g") != o.getString("g")) return@forEachObject
            val secret = runCatching { PactKeys.unb64(inner.getString("s")) }.getOrNull() ?: return@forEachObject
            invites += GroupInvite(o.getString("g"), inner.optString("n").trim().take(MAX_GROUP_NAME).ifBlank { "Grupo" }, secret, from)
        }
        return Board(person(me), friends, friendUids, groups, people, invites)
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
