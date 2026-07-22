package com.sleeproulette.app.notify

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.policy.BedtimePolicy
import com.sleeproulette.app.domain.repo.LifeEventRepository
import com.sleeproulette.app.domain.repo.PlaceRepository
import com.sleeproulette.app.domain.repo.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Foreground service that keeps the sleep countdown alive on ColorOS.
 * Only runs after arriving home until left home / stopped.
 */
@AndroidEntryPoint
class SleepCountdownService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var placeRepository: PlaceRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var lifeEventRepository: LifeEventRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCountdown()
                return START_NOT_STICKY
            }
            else -> startCountdown()
        }
        return START_STICKY
    }

    private fun startCountdown() {
        notificationHelper.ensureChannels()
        // Must call startForeground quickly after startForegroundService.
        val placeholder = notificationHelper.buildCountdownNotification(
            goal = java.time.ZonedDateTime.now(),
            sunrise = null,
            remaining = java.time.Duration.ZERO,
        )
        startForeground(NotificationHelper.ID_COUNTDOWN, placeholder)

        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                updateNotification()
                delay(1_000L)
            }
        }
    }

    private suspend fun updateNotification() {
        val home = placeRepository.getPlace(PlaceKind.HOME) ?: return
        val settings = settingsRepository.current()
        val arrived = lifeEventRepository.latestOf(LifeEventType.ENTER_HOME)?.occurredAt
        val window = BedtimePolicy.resolveSleepWindow(
            settings = settings,
            homeLatitude = home.latitude,
            homeLongitude = home.longitude,
            arrivedHomeAt = arrived,
            now = Instant.now(),
        )
        val notification = notificationHelper.buildCountdownNotification(
            goal = window.goalBedtime,
            sunrise = window.sunrise,
            remaining = window.remaining(),
        )
        startForeground(NotificationHelper.ID_COUNTDOWN, notification)
    }

    private fun stopCountdown() {
        ticker?.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        ticker?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.sleeproulette.app.action.START_COUNTDOWN"
        const val ACTION_STOP = "com.sleeproulette.app.action.STOP_COUNTDOWN"
    }
}
