package com.sleeproulette.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sleeproulette.app.data.local.entity.LifeEventEntity
import com.sleeproulette.app.data.local.entity.PlaceEntity
import com.sleeproulette.app.data.local.entity.SleepSessionEntity
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.PlaceKind
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY kind ASC")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE kind = :kind LIMIT 1")
    suspend fun getByKind(kind: PlaceKind): PlaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaceEntity): Long

    @Query("DELETE FROM places WHERE kind = :kind")
    suspend fun deleteByKind(kind: PlaceKind)
}

@Dao
interface LifeEventDao {
    @Query("SELECT * FROM life_events ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<LifeEventEntity>>

    @Insert
    suspend fun insert(entity: LifeEventEntity): Long

    @Query(
        """
        SELECT * FROM life_events
        WHERE type = :type
        ORDER BY occurredAt DESC
        LIMIT 1
        """,
    )
    suspend fun latestOf(type: LifeEventType): LifeEventEntity?

    @Query(
        """
        SELECT * FROM life_events
        WHERE type = :type AND occurredAt >= :since
        ORDER BY occurredAt ASC
        """,
    )
    suspend fun ofTypeSince(type: LifeEventType, since: Instant): List<LifeEventEntity>
}

@Dao
interface SleepSessionDao {
    @Query("SELECT * FROM sleep_sessions ORDER BY startAt DESC")
    fun observeAll(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions WHERE endAt IS NULL ORDER BY startAt DESC LIMIT 1")
    fun observeOngoing(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE endAt IS NULL ORDER BY startAt DESC LIMIT 1")
    suspend fun getOngoing(): SleepSessionEntity?

    @Insert
    suspend fun insert(entity: SleepSessionEntity): Long

    @Update
    suspend fun update(entity: SleepSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SleepSessionEntity): Long

    @Query("DELETE FROM sleep_sessions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
        SELECT * FROM sleep_sessions
        WHERE startAt >= :since AND endAt IS NOT NULL
        ORDER BY startAt ASC
        """,
    )
    suspend fun completedSince(since: Instant): List<SleepSessionEntity>
}
