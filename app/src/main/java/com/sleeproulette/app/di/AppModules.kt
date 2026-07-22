package com.sleeproulette.app.di

import android.content.Context
import androidx.room.Room
import com.sleeproulette.app.data.local.SleepRouletteDatabase
import com.sleeproulette.app.data.local.dao.LifeEventDao
import com.sleeproulette.app.data.local.dao.PlaceDao
import com.sleeproulette.app.data.local.dao.SleepSessionDao
import com.sleeproulette.app.data.prefs.SettingsRepositoryImpl
import com.sleeproulette.app.data.repo.LifeEventRepositoryImpl
import com.sleeproulette.app.data.repo.PlaceRepositoryImpl
import com.sleeproulette.app.data.repo.SleepSessionRepositoryImpl
import com.sleeproulette.app.domain.repo.LifeEventRepository
import com.sleeproulette.app.domain.repo.PlaceRepository
import com.sleeproulette.app.domain.repo.SettingsRepository
import com.sleeproulette.app.domain.repo.SleepSessionRepository
import dagger.Binds
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
            // Personal MVP: wipe on schema change. Add real migrations before shipping
            // any data you care about keeping across upgrades.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePlaceDao(db: SleepRouletteDatabase): PlaceDao = db.placeDao()

    @Provides
    fun provideLifeEventDao(db: SleepRouletteDatabase): LifeEventDao = db.lifeEventDao()

    @Provides
    fun provideSleepSessionDao(db: SleepRouletteDatabase): SleepSessionDao = db.sleepSessionDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlaceRepository(impl: PlaceRepositoryImpl): PlaceRepository

    @Binds
    @Singleton
    abstract fun bindLifeEventRepository(impl: LifeEventRepositoryImpl): LifeEventRepository

    @Binds
    @Singleton
    abstract fun bindSleepSessionRepository(impl: SleepSessionRepositoryImpl): SleepSessionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
