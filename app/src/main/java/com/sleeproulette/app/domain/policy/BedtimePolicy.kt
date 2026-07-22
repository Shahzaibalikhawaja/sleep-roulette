package com.sleeproulette.app.domain.policy

import com.sleeproulette.app.domain.model.BedtimeGoalMode
import com.sleeproulette.app.domain.model.UserSettings
import com.sleeproulette.app.domain.sunrise.SunriseCalculator
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure decision logic for bedtime goals and nudges.
 * No Android imports — unit-testable.
 */
object BedtimePolicy {

    data class SleepWindow(
        val goalBedtime: ZonedDateTime,
        val sunrise: ZonedDateTime?,
        val arrivedHomeAt: Instant?,
    ) {
        fun remaining(now: Instant = Instant.now()): java.time.Duration =
            java.time.Duration.between(now, goalBedtime.toInstant())

        fun isPastGoal(now: Instant = Instant.now()): Boolean =
            now.isAfter(goalBedtime.toInstant())
    }

    fun resolveSleepWindow(
        settings: UserSettings,
        homeLatitude: Double,
        homeLongitude: Double,
        arrivedHomeAt: Instant?,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): SleepWindow {
        val localNow = ZonedDateTime.ofInstant(now, zone)
        // For shift workers crossing midnight, "today's" sleep goal often belongs
        // to the upcoming morning. If we're after noon, prefer tomorrow's sunrise/goal.
        val targetDate = if (localNow.toLocalTime().hour >= 12) {
            localNow.toLocalDate().plusDays(1)
        } else {
            localNow.toLocalDate()
        }

        val sunrise = SunriseCalculator.sunrise(
            latitude = homeLatitude,
            longitude = homeLongitude,
            date = targetDate,
            zone = zone,
        )

        val goal = when (settings.goalMode) {
            BedtimeGoalMode.FIXED_CLOCK -> {
                val time = settings.fixedGoalLocalTime()
                ZonedDateTime.of(LocalDateTime.of(targetDate, time), zone)
            }
            BedtimeGoalMode.MINUTES_BEFORE_SUNRISE -> {
                val base = sunrise
                    ?: ZonedDateTime.of(targetDate, settings.fixedGoalLocalTime(), zone)
                base.minusMinutes(settings.minutesBeforeSunrise.toLong())
            }
        }

        return SleepWindow(
            goalBedtime = goal,
            sunrise = sunrise,
            arrivedHomeAt = arrivedHomeAt,
        )
    }

    fun shouldNudgeForUsage(
        settings: UserSettings,
        window: SleepWindow,
        interactiveForegroundMinutesSinceGoal: Long,
        now: Instant = Instant.now(),
    ): Boolean {
        if (!window.isPastGoal(now)) return false
        return interactiveForegroundMinutesSinceGoal >= settings.usageNudgeThresholdMinutes
    }
}
