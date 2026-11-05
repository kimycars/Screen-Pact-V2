package com.scrollban.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Friend::class, AppLimit::class, UnlockGrant::class, DayRecord::class, Baseline::class, Challenge::class, PactGroup::class],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun unlockGrantDao(): UnlockGrantDao
    abstract fun dayRecordDao(): DayRecordDao
    abstract fun baselineDao(): BaselineDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun pactGroupDao(): PactGroupDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** v3: horarios en los límites y tabla de resumen diario (historial y rachas). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_limits ADD COLUMN scheduleStart INTEGER")
                db.execSQL("ALTER TABLE app_limits ADD COLUMN scheduleEnd INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS day_records (" +
                        "day TEXT NOT NULL, packageName TEXT NOT NULL, usedMs INTEGER NOT NULL, " +
                        "limitMinutes INTEGER NOT NULL, unlocks INTEGER NOT NULL, " +
                        "PRIMARY KEY(day, packageName))"
                )
            }
        }

        /** v4: cuántas llaves te ha dado cada amigo. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE friends ADD COLUMN keysGiven INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v5: pactos a distancia (esperan 24 h) e id de instalación del amigo. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE friends ADD COLUMN pairedRemotely INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE friends ADD COLUMN peerId TEXT")
            }
        }

        /** v6: uso de antes del límite (tiempo ganado) y retos con fecha. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS baselines (" +
                        "packageName TEXT NOT NULL, avgDailyMs INTEGER NOT NULL, measuredAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(packageName))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS challenges (" +
                        "id TEXT NOT NULL, packageName TEXT NOT NULL, appName TEXT NOT NULL, " +
                        "dailyMinutes INTEGER NOT NULL, startDay TEXT NOT NULL, days INTEGER NOT NULL, " +
                        "createdAt INTEGER NOT NULL, PRIMARY KEY(id))"
                )
            }
        }

        /** v7: grupos de 3 o más. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pact_groups (" +
                        "id TEXT NOT NULL, name TEXT NOT NULL, secret BLOB NOT NULL, joinedAt INTEGER NOT NULL, " +
                        "keysGiven INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))"
                )
            }
        }

        /** v8: amigos con iPhone (pacto mixto) y su código de Tiempo de uso. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE friends ADD COLUMN ios INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE friends ADD COLUMN screenTimeCode TEXT")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screenpact.db"
                )
                    // Cada cambio de estructura lleva su migración: actualizar la app NUNCA
                    // borra pactos ni límites. Solo se borra si se instala una versión más vieja.
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
