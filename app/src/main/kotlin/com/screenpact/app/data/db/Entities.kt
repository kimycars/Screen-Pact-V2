package com.screenpact.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /**
     * Key the friend gave us. We use it to generate codes that THEIR overlay verifies.
     * Only the friend's phone generated this key, so codes derived from it cannot unlock our own overlay.
     */
    val generateKey: ByteArray,
    /**
     * Key we generated and gave to the friend. Our overlay verifies codes against this.
     * Only we have this in our DB; it is not readable through any in-app "generate" screen.
     */
    val verifyKey: ByteArray,
    val addedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Friend) return false
        return id == other.id &&
            name == other.name &&
            generateKey.contentEquals(other.generateKey) &&
            verifyKey.contentEquals(other.verifyKey) &&
            addedAt == other.addedAt
    }

    override fun hashCode(): Int {
        var r = id.hashCode()
        r = 31 * r + name.hashCode()
        r = 31 * r + generateKey.contentHashCode()
        r = 31 * r + verifyKey.contentHashCode()
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
