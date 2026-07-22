package com.sleeproulette.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.model.SleepSource
import java.time.Instant

@Entity(
    tableName = "places",
    indices = [Index(value = ["kind"], unique = true)],
)
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: PlaceKind,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val label: String,
)

@Entity(
    tableName = "life_events",
    indices = [
        Index(value = ["type"]),
        Index(value = ["occurredAt"]),
    ],
)
data class LifeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: LifeEventType,
    val occurredAt: Instant,
    val payloadJson: String? = null,
)

@Entity(
    tableName = "sleep_sessions",
    indices = [Index(value = ["startAt"])],
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startAt: Instant,
    val endAt: Instant?,
    val source: SleepSource,
    val notes: String? = null,
)
