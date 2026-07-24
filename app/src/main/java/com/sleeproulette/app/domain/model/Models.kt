package com.sleeproulette.app.domain.model

import java.time.Instant
import java.time.LocalTime

data class Place(
    val id: Long = 0,
    val kind: PlaceKind,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = DEFAULT_RADIUS_METERS,
    val label: String = kind.name.lowercase().replaceFirstChar { it.titlecase() },
) {
    companion object {
        const val DEFAULT_RADIUS_METERS = 120f
    }
}

data class LifeEvent(
    val id: Long = 0,
    val type: LifeEventType,
    val occurredAt: Instant,
    val payloadJson: String? = null,
)

data class SleepSession(
    val id: Long = 0,
    val startAt: Instant,
    val endAt: Instant?,
    val source: SleepSource,
    val notes: String? = null,
    /** Goal bedtime instant captured when sleep started; null for legacy rows. */
    val goalAtStart: Instant? = null,
) {
    val isOngoing: Boolean get() = endAt == null

    fun durationMinutes(now: Instant = Instant.now()): Long? {
        val end = endAt ?: now
        val minutes = java.time.Duration.between(startAt, end).toMinutes()
        return minutes.takeIf { it >= 0 }
    }
}

/**
 * User preferences that shape interventions.
 * Stored in DataStore (key-value), not Room — they are config, not history.
 */
data class UserSettings(
    val onboardingComplete: Boolean = false,
    val goalMode: BedtimeGoalMode = BedtimeGoalMode.FIXED_CLOCK,
    /** Minutes from midnight local for FIXED_CLOCK (e.g. 4:00 → 240). */
    val fixedGoalMinutesFromMidnight: Int = 4 * 60,
    /** Used when goalMode == MINUTES_BEFORE_SUNRISE. */
    val minutesBeforeSunrise: Int = 90,
    /** Phone foreground minutes after goal before nudging. */
    val usageNudgeThresholdMinutes: Int = 10,
    val batteryOptimizationAcknowledged: Boolean = false,
) {
    fun fixedGoalLocalTime(): LocalTime =
        LocalTime.of(fixedGoalMinutesFromMidnight / 60, fixedGoalMinutesFromMidnight % 60)
}

data class ConsistencyStats(
    val nightCount: Int,
    val averageDurationHours: Double?,
    val medianHomeToBedMinutes: Long?,
    val goalHitRate: Double?,
    /** Consecutive nights that hit the goal (not mere logging streak). */
    val goalHitStreak: Int,
)
