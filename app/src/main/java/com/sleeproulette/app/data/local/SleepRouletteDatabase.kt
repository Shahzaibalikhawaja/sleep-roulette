package com.sleeproulette.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sleeproulette.app.data.local.dao.LifeEventDao
import com.sleeproulette.app.data.local.dao.PlaceDao
import com.sleeproulette.app.data.local.dao.SleepSessionDao
import com.sleeproulette.app.data.local.entity.LifeEventEntity
import com.sleeproulette.app.data.local.entity.PlaceEntity
import com.sleeproulette.app.data.local.entity.SleepSessionEntity

@Database(
    entities = [
        PlaceEntity::class,
        LifeEventEntity::class,
        SleepSessionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SleepRouletteDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun lifeEventDao(): LifeEventDao
    abstract fun sleepSessionDao(): SleepSessionDao
}
