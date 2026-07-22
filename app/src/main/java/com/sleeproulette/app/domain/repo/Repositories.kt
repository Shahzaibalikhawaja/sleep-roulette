package com.sleeproulette.app.domain.repo

import com.sleeproulette.app.domain.model.ConsistencyStats
import com.sleeproulette.app.domain.model.LifeEvent
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.Place
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.model.SleepSession
import com.sleeproulette.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository interfaces live in domain so ViewModels depend on abstractions,
 * not Room/DataStore. Implementations sit in data/.
 */
interface PlaceRepository {
    fun observePlaces(): Flow<List<Place>>
    suspend fun getPlace(kind: PlaceKind): Place?
    suspend fun upsert(place: Place): Long
    suspend fun delete(kind: PlaceKind)
}

interface LifeEventRepository {
    fun observeRecent(limit: Int = 100): Flow<List<LifeEvent>>
    suspend fun record(type: LifeEventType, at: Instant = Instant.now(), payloadJson: String? = null)
    suspend fun latestOf(type: LifeEventType): LifeEvent?
}

interface SleepSessionRepository {
    fun observeSessions(): Flow<List<SleepSession>>
    fun observeOngoing(): Flow<SleepSession?>
    suspend fun start(source: com.sleeproulette.app.domain.model.SleepSource, at: Instant = Instant.now()): Long
    suspend fun endOngoing(at: Instant = Instant.now())
    suspend fun upsert(session: SleepSession): Long
    suspend fun delete(id: Long)
    suspend fun stats(since: Instant): ConsistencyStats
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun current(): UserSettings
    suspend fun update(transform: (UserSettings) -> UserSettings)
}
