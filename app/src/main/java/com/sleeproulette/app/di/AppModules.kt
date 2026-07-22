package com.sleeproulette.app.di

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
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Process-wide scope for BroadcastReceivers / one-shot background work.
     * Prefer this over ad-hoc SupervisorJob() scopes that never cancel.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
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
