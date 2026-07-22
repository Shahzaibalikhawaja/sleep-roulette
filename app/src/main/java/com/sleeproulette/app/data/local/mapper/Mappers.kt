package com.sleeproulette.app.data.local.mapper

import com.sleeproulette.app.data.local.entity.LifeEventEntity
import com.sleeproulette.app.data.local.entity.PlaceEntity
import com.sleeproulette.app.data.local.entity.SleepSessionEntity
import com.sleeproulette.app.domain.model.LifeEvent
import com.sleeproulette.app.domain.model.Place
import com.sleeproulette.app.domain.model.SleepSession

fun PlaceEntity.toDomain() = Place(
    id = id,
    kind = kind,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    label = label,
)

fun Place.toEntity() = PlaceEntity(
    id = id,
    kind = kind,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    label = label,
)

fun LifeEventEntity.toDomain() = LifeEvent(
    id = id,
    type = type,
    occurredAt = occurredAt,
    payloadJson = payloadJson,
)

fun SleepSessionEntity.toDomain() = SleepSession(
    id = id,
    startAt = startAt,
    endAt = endAt,
    source = source,
    notes = notes,
)

fun SleepSession.toEntity() = SleepSessionEntity(
    id = id,
    startAt = startAt,
    endAt = endAt,
    source = source,
    notes = notes,
)
