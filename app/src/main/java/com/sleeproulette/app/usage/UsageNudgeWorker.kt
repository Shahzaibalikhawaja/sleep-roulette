package com.sleeproulette.app.usage

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.PlaceKind
import com.sleeproulette.app.domain.policy.BedtimePolicy
import com.sleeproulette.app.domain.repo.LifeEventRepository
import com.sleeproulette.app.domain.repo.PlaceRepository
import com.sleeproulette.app.domain.repo.SettingsRepository
import com.sleeproulette.app.notify.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import androidx.hilt.work.HiltWorker

@Singleton
class UsageNudgeScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scheduleAfterGoal(goalAt: Instant) {
        val delayMs = (goalAt.toEpochMilli() - Instant.now().toEpochMilli())
            .coerceAtLeast(0L)
        // Also wait the threshold after goal before first check.
        val request = OneTimeWorkRequestBuilder<UsageNudgeWorker>()
            .setInitialDelay(delayMs + 60_000L, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }

    companion object {
        const val UNIQUE_WORK = "usage_nudge"
    }
}

@HiltWorker
class UsageNudgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val placeRepository: PlaceRepository,
    private val lifeEventRepository: LifeEventRepository,
    private val usageStatsSampler: UsageStatsSampler,
    private val notificationHelper: NotificationHelper,
    private val usageNudgeScheduler: UsageNudgeScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val home = placeRepository.getPlace(PlaceKind.HOME) ?: return Result.success()
        val settings = settingsRepository.current()
        val arrived = lifeEventRepository.latestOf(LifeEventType.ENTER_HOME)?.occurredAt
        val window = BedtimePolicy.resolveSleepWindow(
            settings = settings,
            homeLatitude = home.latitude,
            homeLongitude = home.longitude,
            arrivedHomeAt = arrived,
        )
        if (!window.isPastGoal()) return Result.success()

        val minutes = usageStatsSampler.interactiveForegroundMinutes(
            from = window.goalBedtime.toInstant(),
            to = Instant.now(),
        )
        if (BedtimePolicy.shouldNudgeForUsage(settings, window, minutes)) {
            lifeEventRepository.record(LifeEventType.USAGE_SPIKE)
            notificationHelper.ensureChannels()
            notificationHelper.showNudge()
            // Re-check later so we don't spam every minute — one nudge per work run,
            // then schedule another check in 30 minutes while still past goal.
            usageNudgeScheduler.scheduleAfterGoal(Instant.now().plusSeconds(30 * 60))
        } else {
            // Not enough usage yet — check again soon.
            usageNudgeScheduler.scheduleAfterGoal(Instant.now().plusSeconds(5 * 60))
        }
        return Result.success()
    }
}
