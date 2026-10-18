package com.screenpact.app.data

import com.screenpact.app.data.crypto.TOTPManager
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.Friend

/**
 * Único punto donde se decide si un código de 6 dígitos desbloquea algo.
 *
 * Un amigo recién emparejado no tiene poder de desbloqueo durante [NEW_FRIEND_COOLDOWN_MS].
 * Así no sirve emparejarse en caliente con un segundo móvil (o una app clonada) para salir de
 * un bloqueo. El primer amigo no espera: sin él la app no podría empezar a usarse.
 */
object FriendCodes {

    const val NEW_FRIEND_COOLDOWN_MS = 24 * 60 * 60_000L

    fun canUnlock(friend: Friend, all: List<Friend>, now: Long = System.currentTimeMillis()): Boolean {
        val first = all.minByOrNull { it.addedAt }
        return friend.id == first?.id || now - friend.addedAt >= NEW_FRIEND_COOLDOWN_MS
    }

    /** Milisegundos que le faltan a [friend] para poder desbloquear (0 si ya puede). */
    fun cooldownLeftMs(friend: Friend, all: List<Friend>, now: Long = System.currentTimeMillis()): Long =
        if (canUnlock(friend, all, now)) 0 else NEW_FRIEND_COOLDOWN_MS - (now - friend.addedAt)

    suspend fun match(db: AppDatabase, code: String): Friend? {
        val all = db.friendDao().getAll()
        return all.firstOrNull { canUnlock(it, all) && TOTPManager.verifyCode(it.verifyKey, code) }
    }
}
