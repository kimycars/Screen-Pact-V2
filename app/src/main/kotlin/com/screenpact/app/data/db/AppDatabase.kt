package com.screenpact.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Friend::class, AppLimit::class, UnlockGrant::class],
    version = 2,          // bumped: Friend.secret → generateKey + verifyKey (directional secrets)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun unlockGrantDao(): UnlockGrantDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screenpact.db"
                )
                    // Old single-secret rows cannot be safely split into two directional keys,
                    // so we wipe and let users re-pair. Acceptable at this stage of development.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
