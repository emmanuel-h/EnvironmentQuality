package fr.mandarine.environmentquality.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedResponseDao {
    @Query("SELECT * FROM cached_response WHERE url = :url")
    suspend fun get(url: String): CachedResponseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: CachedResponseEntity)

    @Query("DELETE FROM cached_response WHERE fetchedAtMillis < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface FollowedCommuneDao {
    @Query("SELECT * FROM followed_commune ORDER BY followedAtMillis")
    fun observeAll(): Flow<List<FollowedCommuneEntity>>

    @Query("SELECT * FROM followed_commune ORDER BY followedAtMillis")
    suspend fun all(): List<FollowedCommuneEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM followed_commune WHERE code = :code)")
    fun observeIsFollowed(code: String): Flow<Boolean>

    @Query("SELECT * FROM followed_commune WHERE code = :code")
    suspend fun get(code: String): FollowedCommuneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: FollowedCommuneEntity)

    @Query("DELETE FROM followed_commune WHERE code = :code")
    suspend fun delete(code: String)

    @Query("UPDATE followed_commune SET hasUpdate = 0 WHERE code = :code")
    suspend fun markSeen(code: String)
}

@Dao
interface RecentCommuneDao {
    @Query("SELECT * FROM recent_commune ORDER BY viewedAtMillis DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<RecentCommuneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: RecentCommuneEntity)

    @Query("DELETE FROM recent_commune WHERE code NOT IN (SELECT code FROM recent_commune ORDER BY viewedAtMillis DESC LIMIT :keep)")
    suspend fun trim(keep: Int)
}

@Dao
interface AlertSettingsDao {
    @Query("SELECT * FROM alert_settings WHERE id = 0")
    fun observe(): Flow<AlertSettingsEntity?>

    @Query("SELECT * FROM alert_settings WHERE id = 0")
    suspend fun get(): AlertSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: AlertSettingsEntity)
}
