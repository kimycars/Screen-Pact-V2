package com.screenpact.app.data

import android.content.Context
import com.screenpact.app.data.crypto.GroupKeys
import com.screenpact.app.data.crypto.TOTPManager
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.Friend
import com.screenpact.app.data.db.PactGroup

/**
 * Único punto donde se decide si un código de 6 dígitos desbloquea algo.
 *
 * Un amigo recién emparejado no tiene poder de desbloqueo durante [NEW_FRIEND_COOLDOWN_MS].
 * Así no sirve emparejarse en caliente con un segundo móvil (o una app clonada) para salir de
 * un bloqueo. El primer amigo sellado en persona no espera: sin él la app no podría empezar a
 * usarse. Los pactos a distancia esperan siempre (ver data/Pairing.kt).
 *
 * En un grupo de 3 o más, cualquier miembro puede darte la llave. Las llaves del grupo valen a
 * las 24 h de entrar en él, siempre.
 */
object FriendCodes {

    const val NEW_FRIEND_COOLDOWN_MS = 24 * 60 * 60_000L

    fun canUnlock(friend: Friend, all: List<Friend>, now: Long = System.currentTimeMillis()): Boolean {
        val first = all.minByOrNull { it.addedAt }
        // El primer amigo no espera solo si el pacto se selló en persona. A distancia es más fácil
        // hacerse amigo de uno mismo con otro móvil, así que ahí siempre hay 24 h de espera.
        val exempt = friend.id == first?.id && !friend.pairedRemotely
        return exempt || now - friend.addedAt >= NEW_FRIEND_COOLDOWN_MS
    }

    /** Milisegundos que le faltan a [friend] para poder desbloquear (0 si ya puede). */
    fun cooldownLeftMs(friend: Friend, all: List<Friend>, now: Long = System.currentTimeMillis()): Long =
        if (canUnlock(friend, all, now)) 0 else NEW_FRIEND_COOLDOWN_MS - (now - friend.addedAt)

    fun canUnlock(group: PactGroup, now: Long = System.currentTimeMillis()): Boolean =
        now - group.joinedAt >= NEW_FRIEND_COOLDOWN_MS

    fun cooldownLeftMs(group: PactGroup, now: Long = System.currentTimeMillis()): Long =
        (NEW_FRIEND_COOLDOWN_MS - (now - group.joinedAt)).coerceAtLeast(0)

    /** Quién te ha dado la llave: un amigo del pacto o alguien de un grupo. */
    sealed class Keyholder {
        data class OfFriend(val friend: Friend) : Keyholder()
        data class OfGroup(val group: PactGroup) : Keyholder()
    }

    suspend fun match(context: Context, code: String): Keyholder? {
        val db = AppDatabase.get(context)
        val all = db.friendDao().getAll()
        all.firstOrNull { canUnlock(it, all) && TOTPManager.verifyCode(it.verifyKey, code) }?.let { return Keyholder.OfFriend(it) }
        val me = Prefs.getInstallId(context)
        return db.pactGroupDao().getAll()
            .firstOrNull { canUnlock(it) && TOTPManager.verifyCode(GroupKeys.unlockKey(it.secret, me), code) }
            ?.let { Keyholder.OfGroup(it) }
    }

    /** Usa una llave: si vale, apunta quién te ha dado una más y lo devuelve. */
    suspend fun redeem(context: Context, code: String): Keyholder? {
        val db = AppDatabase.get(context)
        return match(context, code)?.also {
            when (it) {
                is Keyholder.OfFriend -> db.friendDao().countKey(it.friend.id)
                is Keyholder.OfGroup -> db.pactGroupDao().countKey(it.group.id)
            }
        }
    }

    /** Nombres de quien puede darte la llave ahora mismo: amigos y grupos. */
    suspend fun keyholderNames(context: Context): List<String> {
        val db = AppDatabase.get(context)
        val all = db.friendDao().getAll()
        return all.sortedBy { it.addedAt }.filter { canUnlock(it, all) }.map { it.name } +
            db.pactGroupDao().getAll().filter { canUnlock(it) }.map { "alguien del grupo ${it.name}" }
    }
}
