package com.screenpact.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<Friend>>

    @Query("SELECT * FROM friends")
    suspend fun getAll(): List<Friend>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(friend: Friend): Long

    @Delete
    suspend fun delete(friend: Friend)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits ORDER BY appName")
    fun observeAll(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits WHERE enabled = 1")
    suspend fun getEnabled(): List<AppLimit>

    @Query("SELECT * FROM app_limits WHERE packageName = :pkg")
    suspend fun get(pkg: String): AppLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: AppLimit)

    @Query("DELETE FROM app_limits WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}

@Dao
interface UnlockGrantDao {
    @Query("SELECT * FROM unlock_grants WHERE packageName = :pkg LIMIT 1")
    suspend fun get(pkg: String): UnlockGrant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(grant: UnlockGrant)

    @Query("DELETE FROM unlock_grants WHERE expiresAt < :now")
    suspend fun cleanExpired(now: Long)
}
