package com.scrollban.app.data.db

import androidx.room.ColumnInfo
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
    val addedAt: Long = System.currentTimeMillis(),
    /** Llaves que este amigo te ha dado (cuenta desde la versión de base de datos 4). */
    @ColumnInfo(defaultValue = "0") val keysGiven: Int = 0,
    /** Pacto sellado a distancia (por enlace): espera siempre 24 h antes de poder dar llaves. */
    @ColumnInfo(defaultValue = "0") val pairedRemotely: Boolean = false,
    /** Id de instalación del amigo (desde la v5), para no sellar dos pactos con la misma persona. */
    val peerId: String? = null,
    /** Amigo con iPhone (desde la v8): te da la llave desde la web y tú le das su código de Tiempo de uso. */
    @ColumnInfo(defaultValue = "0") val ios: Boolean = false,
    /** Código de 4 cifras de Tiempo de uso que pones en su iPhone. Solo si [ios]. */
    val screenTimeCode: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Friend) return false
        return id == other.id &&
            name == other.name &&
            generateKey.contentEquals(other.generateKey) &&
            verifyKey.contentEquals(other.verifyKey) &&
            addedAt == other.addedAt &&
            keysGiven == other.keysGiven &&
            pairedRemotely == other.pairedRemotely &&
            peerId == other.peerId &&
            ios == other.ios &&
            screenTimeCode == other.screenTimeCode
    }

    override fun hashCode(): Int {
        var r = id.hashCode()
        r = 31 * r + name.hashCode()
        r = 31 * r + generateKey.contentHashCode()
        r = 31 * r + verifyKey.contentHashCode()
        r = 31 * r + addedAt.hashCode()
        r = 31 * r + keysGiven
        r = 31 * r + pairedRemotely.hashCode()
        r = 31 * r + (peerId?.hashCode() ?: 0)
        r = 31 * r + ios.hashCode()
        r = 31 * r + (screenTimeCode?.hashCode() ?: 0)
        return r
    }
}

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val enabled: Boolean = true,
    /** Horario de cierre (Pro), en minutos desde medianoche. Si fin < inicio, cruza la medianoche. */
    val scheduleStart: Int? = null,
    val scheduleEnd: Int? = null
) {
    val hasSchedule: Boolean get() = scheduleStart != null && scheduleEnd != null && scheduleStart != scheduleEnd
}

/** Tras desbloquear con un código válido, damos una ventana de gracia para no relanzar el overlay al instante. */
@Entity(tableName = "unlock_grants")
data class UnlockGrant(
    @PrimaryKey val packageName: String,
    val expiresAt: Long
)

/**
 * Resumen de un día y una app, para el historial y las rachas. Se escribe siempre, tenga o
 * no Pro la persona, para que al hacerse Pro ya vea su historia. [day] es AAAA-MM-DD local.
 */
@Entity(tableName = "day_records", primaryKeys = ["day", "packageName"])
data class DayRecord(
    val day: String,
    val packageName: String,
    val usedMs: Long = 0,
    val limitMinutes: Int = 0,
    /** Veces que se usó una llave para abrir esta app ese día. */
    val unlocks: Int = 0
)

/**
 * Cuánto usabas cada app antes de ponerle límite (media diaria de los 7 días anteriores). Es la
 * vara de medir del tiempo ganado. Se mide una vez, la primera vez que la app tiene límite.
 */
@Entity(tableName = "baselines")
data class Baseline(
    @PrimaryKey val packageName: String,
    val avgDailyMs: Long,
    val measuredAt: Long = System.currentTimeMillis()
)

/**
 * Reto con fecha: [days] días seguidos desde [startDay] (AAAA-MM-DD) usando [packageName] como
 * mucho [dailyMinutes] al día (0 = sin la app). [id] es un UUID para poder compartirlo con el
 * grupo cuando haya servidor. Solo hay un reto activo a la vez.
 */
@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey val id: String,
    val packageName: String,
    val appName: String,
    val dailyMinutes: Int,
    val startDay: String,
    val days: Int,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Grupo de 3 o más (desde la v7). Todos los miembros guardan el mismo [secret]: de él salen la
 * llave de cada miembro, el secreto para entrar en el grupo del servidor y el cifrado de los
 * nombres. Llega cifrado de un amigo con pacto (ver data/cloud/Together.kt) y nunca va en claro
 * por ningún enlace ni al servidor. Sus llaves valen a las 24 h de entrar ([joinedAt]).
 */
@Entity(tableName = "pact_groups")
data class PactGroup(
    @PrimaryKey val id: String,
    val name: String,
    val secret: ByteArray,
    val joinedAt: Long = System.currentTimeMillis(),
    /** Llaves que te ha dado alguien del grupo. */
    @ColumnInfo(defaultValue = "0") val keysGiven: Int = 0
) {
    override fun equals(other: Any?): Boolean =
        other is PactGroup && id == other.id && name == other.name && secret.contentEquals(other.secret) &&
            joinedAt == other.joinedAt && keysGiven == other.keysGiven

    override fun hashCode(): Int = id.hashCode()
}
