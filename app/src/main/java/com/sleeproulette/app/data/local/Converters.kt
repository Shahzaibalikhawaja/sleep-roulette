package com.sleeproulette.app.data.local

import androidx.room.TypeConverter
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.model.SleepSource
import java.time.Instant

/**
 * Room only persists primitives / Strings. Converters bridge domain types.
 * Prefer Instant epoch millis over String dates — sortable and unambiguous.
 */
class Converters {
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun placeKindToString(value: PlaceKind): String = value.name

    @TypeConverter
    fun stringToPlaceKind(value: String): PlaceKind = PlaceKind.valueOf(value)

    @TypeConverter
    fun lifeEventTypeToString(value: LifeEventType): String = value.name

    @TypeConverter
    fun stringToLifeEventType(value: String): LifeEventType = LifeEventType.valueOf(value)

    @TypeConverter
    fun sleepSourceToString(value: SleepSource): String = value.name

    @TypeConverter
    fun stringToSleepSource(value: String): SleepSource = SleepSource.valueOf(value)
}
