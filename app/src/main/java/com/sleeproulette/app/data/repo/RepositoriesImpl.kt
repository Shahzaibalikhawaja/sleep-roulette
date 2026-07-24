package com.sleeproulette.app.data.repo

import com.sleeproulette.app.data.local.dao.LifeEventDao
import com.sleeproulette.app.data.local.dao.PlaceDao
import com.sleeproulette.app.data.local.dao.SleepSessionDao
import com.sleeproulette.app.data.local.entity.LifeEventEntity
import com.sleeproulette.app.data.local.entity.SleepSessionEntity
import com.sleeproulette.app.data.local.mapper.toDomain
import com.sleeproulette.app.data.local.mapper.toEntity
import com.sleeproulette.app.domain.model.ConsistencyStats
import com.sleeproulette.app.domain.model.LifeEvent
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.Place
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.model.SleepSession
import com.sleeproulette.app.domain.model.SleepSource
import com.sleeproulette.app.domain.repo.LifeEventRepository
import com.sleeproulette.app.domain.repo.PlaceRepository
import com.sleeproulette.app.domain.repo.SleepSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaceRepositoryImpl @Inject constructor(
    private val placeDao: PlaceDao,
) : PlaceRepository {
    override fun observePlaces(): Flow<List<Place>> =
        placeDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getPlace(kind: PlaceKind): Place? =
        placeDao.getByKind(kind)?.toDomain()

    override suspend fun upsert(place: Place): Long {
        val existing = placeDao.getByKind(place.kind)
        val entity = place.toEntity().copy(id = existing?.id ?: place.id)
        return placeDao.upsert(entity)
    }

    override suspend fun delete(kind: PlaceKind) = placeDao.deleteByKind(kind)
}

@Singleton
class LifeEventRepositoryImpl @Inject constructor(
    private val lifeEventDao: LifeEventDao,
) : LifeEventRepository {
    override fun observeRecent(limit: Int): Flow<List<LifeEvent>> =
        lifeEventDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun record(type: LifeEventType, at: Instant, payloadJson: String?) {
        lifeEventDao.insert(
            LifeEventEntity(type = type, occurredAt = at, payloadJson = payloadJson),
        )
    }

    override suspend fun latestOf(type: LifeEventType): LifeEvent? =
        lifeEventDao.latestOf(type)?.toDomain()
}

@Singleton
class SleepSessionRepositoryImpl @Inject constructor(
    private val sleepSessionDao: SleepSessionDao,
) : SleepSessionRepository {
    override fun observeSessions(): Flow<List<SleepSession>> =
        sleepSessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeOngoing(): Flow<SleepSession?> =
        sleepSessionDao.observeOngoing().map { it?.toDomain() }

    override suspend fun start(source: SleepSource, at: Instant): Long {
        // End any dangling session first — avoids duplicate "ongoing" rows.
        endOngoing(at)
        return sleepSessionDao.insert(
            SleepSessionEntity(startAt = at, endAt = null, source = source),
        )
    }

    override suspend fun endOngoing(at: Instant) {
        val ongoing = sleepSessionDao.getOngoing() ?: return
        sleepSessionDao.update(ongoing.copy(endAt = at))
    }

    override suspend fun upsert(session: SleepSession): Long =
        sleepSessionDao.upsert(session.toEntity())

    override suspend fun delete(id: Long) = sleepSessionDao.delete(id)

    override suspend fun stats(since: Instant): ConsistencyStats {
        val sessions = sleepSessionDao.completedSince(since)
        val durations = sessions.mapNotNull { s ->
            s.endAt?.let { Duration.between(s.startAt, it).toMinutes() }
        }
        val avgHours = durations.takeIf { it.isNotEmpty() }
            ?.average()
            ?.div(60.0) // minutes → hours; based on session startAt→endAt only


        // home→bed median + real goal hit rate need ENTER_HOME joins / goal snapshots.
        // Ship averages + streak first; refine analytics in a follow-up.
        return ConsistencyStats(
            nightCount = sessions.size,
            averageDurationHours = avgHours,
            medianHomeToBedMinutes = null,
            goalHitRate = null,
            currentStreak = computeStreak(sessions),
        )
    }

    private fun computeStreak(sessions: List<SleepSessionEntity>): Int {
        if (sessions.isEmpty()) return 0
        // Count consecutive calendar nights ending with the most recent session.
        val nights = sessions
            .mapNotNull { it.endAt }
            .map { it.atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()
        if (nights.isEmpty()) return 0
        var streak = 1
        for (i in 0 until nights.lastIndex) {
            if (nights[i].minusDays(1) == nights[i + 1]) streak++ else break
        }
        return streak
    }
}
