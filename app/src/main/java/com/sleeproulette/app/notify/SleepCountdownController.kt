package com.sleeproulette.app.notify

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.policy.BedtimePolicy
import com.sleeproulette.app.domain.repo.LifeEventRepository
import com.sleeproulette.app.domain.repo.PlaceRepository
import com.sleeproulette.app.domain.repo.SettingsRepository
import com.sleeproulette.app.usage.UsageNudgeScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates arrive-home → countdown FGS + usage nudge scheduling.
 * Keeps BroadcastReceivers thin.
 */
@Singleton
class SleepCountdownController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val placeRepository: PlaceRepository,
    private val settingsRepository: SettingsRepository,
    private val lifeEventRepository: LifeEventRepository,
    private val usageNudgeScheduler: UsageNudgeScheduler,
) {
    suspend fun onArrivedHome() {
        val home = placeRepository.getPlace(PlaceKind.HOME) ?: return
        val settings = settingsRepository.current()
        val arrived = lifeEventRepository.latestOf(LifeEventType.ENTER_HOME)?.occurredAt
        val window = BedtimePolicy.resolveSleepWindow(
            settings = settings,
            homeLatitude = home.latitude,
            homeLongitude = home.longitude,
            arrivedHomeAt = arrived,
        )
        lifeEventRepository.record(LifeEventType.COUNTDOWN_STARTED)
        usageNudgeScheduler.scheduleAfterGoal(window.goalBedtime.toInstant())

        val intent = Intent(context, SleepCountdownService::class.java).apply {
            action = SleepCountdownService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    suspend fun onLeftHome() {
        lifeEventRepository.record(LifeEventType.COUNTDOWN_STOPPED)
        usageNudgeScheduler.cancel()
        val intent = Intent(context, SleepCountdownService::class.java).apply {
            action = SleepCountdownService.ACTION_STOP
        }
        context.startService(intent)
    }

    /** Manual start from UI (useful when geofence is flaky during development). */
    suspend fun startManually() = onArrivedHome()

    /** Stop countdown FGS without recording EXIT_HOME (e.g. user started sleep). */
    suspend fun stop() {
        lifeEventRepository.record(LifeEventType.COUNTDOWN_STOPPED)
        usageNudgeScheduler.cancel()
        val intent = Intent(context, SleepCountdownService::class.java).apply {
            action = SleepCountdownService.ACTION_STOP
        }
        context.startService(intent)
    }
}
