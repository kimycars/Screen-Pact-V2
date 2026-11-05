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

    @Query("UPDATE friends SET keysGiven = keysGiven + 1 WHERE id = :id")
    suspend fun countKey(id: Long)

    @Query("UPDATE friends SET screenTimeCode = :code WHERE id = :id")
    suspend fun setScreenTimeCode(id: Long, code: String)
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

@Dao
interface DayRecordDao {
    @Query("SELECT * FROM day_records WHERE day >= :fromDay ORDER BY day")
    suspend fun since(fromDay: String): List<DayRecord>

    @Query("SELECT * FROM day_records WHERE day = :day AND packageName = :pkg")
    suspend fun get(day: String, pkg: String): DayRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DayRecord)

    /** Días distintos con algún registro, del más reciente al más antiguo. */
    @Query("SELECT day FROM day_records GROUP BY day ORDER BY day DESC")
    suspend fun days(): List<String>
}

@Dao
interface BaselineDao {
    @Query("SELECT * FROM baselines")
    suspend fun getAll(): List<Baseline>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(baseline: Baseline)
}

@Dao
interface ChallengeDao {
    /** El reto más reciente (en curso o recién terminado). */
    @Query("SELECT * FROM challenges ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<Challenge?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(challenge: Challenge)

    @Query("DELETE FROM challenges WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PactGroupDao {
    @Query("SELECT * FROM pact_groups ORDER BY joinedAt")
    fun observeAll(): Flow<List<PactGroup>>

    @Query("SELECT * FROM pact_groups")
    suspend fun getAll(): List<PactGroup>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(group: PactGroup)

    @Query("DELETE FROM pact_groups WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE pact_groups SET keysGiven = keysGiven + 1 WHERE id = :id")
    suspend fun countKey(id: String)
}
