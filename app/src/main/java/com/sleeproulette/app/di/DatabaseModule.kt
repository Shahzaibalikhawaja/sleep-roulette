package com.sleeproulette.app.di

import android.content.Context
import androidx.room.Room
import com.sleeproulette.app.data.local.SleepRouletteDatabase
import com.sleeproulette.app.data.local.dao.LifeEventDao
import com.sleeproulette.app.data.local.dao.PlaceDao
import com.sleeproulette.app.data.local.dao.SleepSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SleepRouletteDatabase =
        Room.databaseBuilder(
            context,
            SleepRouletteDatabase::class.java,
            "sleep_roulette.db",
        )
            // Room 2.7+: explicit dropAllTables. Fine for personal MVP; add real
            // Migration / AutoMigration before any data you care about keeping.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun providePlaceDao(db: SleepRouletteDatabase): PlaceDao = db.placeDao()

    @Provides
    fun provideLifeEventDao(db: SleepRouletteDatabase): LifeEventDao = db.lifeEventDao()

    @Provides
    fun provideSleepSessionDao(db: SleepRouletteDatabase): SleepSessionDao = db.sleepSessionDao()
}
