package com.sleeproulette.app.domain.trends

import com.sleeproulette.app.domain.model.LifeEvent
import com.sleeproulette.app.domain.model.SleepSession
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong

data class GoalHitPoint(
    val startAt: Instant,
    val hit: Boolean,
)

data class HomeToBedPoint(
    val startAt: Instant,
    val minutes: Long,
)

data class TrendsSnapshot(
    val nightCount: Int,
    val averageDurationHours: Double?,
    val goalHitRate: Double?,
    val goalHitStreak: Int,
    val medianHomeToBedMinutes: Long?,
    val goalHitSeries: List<GoalHitPoint>,
    val homeToBedSeries: List<HomeToBedPoint>,
)

/**
 * North-star analytics. Pure Kotlin — unit-testable.
 *
 * Goal hit: startAt <= goalAtStart. Sessions without goalAtStart are visible
 * elsewhere but excluded from goal-rate denominators and goal-hit streak.
 */
object TrendsAnalytics {

    /** Match ENTER_HOME that precedes sleep start within this window. */
    val HomeMatchWindow: Duration = Duration.ofHours(18)

    fun compute(
        sessions: List<SleepSession>,
        enterHomeEvents: List<LifeEvent>,
    ): TrendsSnapshot {
        val completed = sessions
            .filter { it.endAt != null }
            .sortedBy { it.startAt }

        val durations = completed.mapNotNull { s ->
            s.endAt?.let { Duration.between(s.startAt, it).toMinutes() }
        }
        val avgHours = durations.takeIf { it.isNotEmpty() }
            ?.average()
            ?.div(60.0)

        val withGoal = completed.filter { it.goalAtStart != null }
        val goalHits = withGoal.map { session ->
            val goal = session.goalAtStart!!
            GoalHitPoint(
                startAt = session.startAt,
                hit = !session.startAt.isAfter(goal),
            )
        }
        val hitRate = goalHits.takeIf { it.isNotEmpty() }
            ?.let { hits -> hits.count { it.hit }.toDouble() / hits.size }

        val homePoints = completed.mapNotNull { session ->
            matchHomeToBedMinutes(session.startAt, enterHomeEvents)?.let { minutes ->
                HomeToBedPoint(startAt = session.startAt, minutes = minutes)
            }
        }
        val medianHome = median(homePoints.map { it.minutes })

        return TrendsSnapshot(
            nightCount = completed.size,
            averageDurationHours = avgHours,
            goalHitRate = hitRate,
            goalHitStreak = goalHitStreak(withGoal.sortedByDescending { it.startAt }),
            medianHomeToBedMinutes = medianHome,
            goalHitSeries = goalHits,
            homeToBedSeries = homePoints,
        )
    }

    fun isGoalHit(session: SleepSession): Boolean? {
        val goal = session.goalAtStart ?: return null
        return !session.startAt.isAfter(goal)
    }

    fun matchHomeToBedMinutes(
        sleepStart: Instant,
        enterHomeEvents: List<LifeEvent>,
    ): Long? {
        val windowStart = sleepStart.minus(HomeMatchWindow)
        val match = enterHomeEvents
            .asSequence()
            .map { it.occurredAt }
            .filter { !it.isAfter(sleepStart) && !it.isBefore(windowStart) }
            .maxOrNull()
            ?: return null
        val minutes = Duration.between(match, sleepStart).toMinutes()
        return minutes.takeIf { it >= 0 }
    }

    /**
     * Consecutive goal hits from the most recent session with a goal snapshot.
     * Sessions without goalAtStart are skipped (do not break or extend the streak).
     */
    fun goalHitStreak(sessionsNewestFirst: List<SleepSession>): Int {
        var streak = 0
        for (session in sessionsNewestFirst) {
            val hit = isGoalHit(session) ?: continue
            if (hit) streak++ else break
        }
        return streak
    }

    fun validateSessionBounds(startAt: Instant, endAt: Instant?): String? {
        if (endAt != null && !endAt.isAfter(startAt)) {
            return "End must be after start"
        }
        return null
    }

    private fun median(values: List<Long>): Long? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            ((sorted[mid - 1] + sorted[mid]) / 2.0).roundToLong()
        } else {
            sorted[mid]
        }
    }
}
