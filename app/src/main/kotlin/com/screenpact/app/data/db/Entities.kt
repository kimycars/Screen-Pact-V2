package com.screenpact.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Secreto compartido (HMAC-SHA1 key). Almacenado tal cual; idealmente lo reforzaríamos con EncryptedFile. */
    val secret: ByteArray,
    val addedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Friend) return false
        return id == other.id && name == other.name && secret.contentEquals(other.secret) && addedAt == other.addedAt
    }
    override fun hashCode(): Int {
        var r = id.hashCode()
        r = 31 * r + name.hashCode()
        r = 31 * r + secret.contentHashCode()
        r = 31 * r + addedAt.hashCode()
        return r
    }
}

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val enabled: Boolean = true
)

/** Tras desbloquear con un código válido, damos una ventana de gracia para no relanzar el overlay al instante. */
@Entity(tableName = "unlock_grants")
data class UnlockGrant(
    @PrimaryKey val packageName: String,
    val expiresAt: Long
)
