package com.sleeproulette.app.domain.trends

import com.google.common.truth.Truth.assertThat
import com.sleeproulette.app.domain.model.LifeEvent
import com.sleeproulette.app.domain.model.LifeEventType
import com.sleeproulette.app.domain.model.SleepSession
import com.sleeproulette.app.domain.model.SleepSource
import org.junit.Test
import java.time.Instant

class TrendsAnalyticsTest {

    private val night1 = Instant.parse("2026-07-20T22:00:00Z")
    private val night2 = Instant.parse("2026-07-21T23:00:00Z")
    private val night3 = Instant.parse("2026-07-22T21:30:00Z")

    @Test
    fun goalHitRate_excludesSessionsWithoutSnapshot() {
        val sessions = listOf(
            session(night1, night1.plusSeconds(28_800), goal = night1.plusSeconds(1800)), // hit
            session(night2, night2.plusSeconds(28_800), goal = null), // excluded
            session(night3, night3.plusSeconds(28_800), goal = night3.minusSeconds(600)), // miss
        )
        val snap = TrendsAnalytics.compute(sessions, emptyList())
        assertThat(snap.goalHitRate).isEqualTo(0.5)
        assertThat(snap.goalHitSeries).hasSize(2)
        assertThat(snap.nightCount).isEqualTo(3)
    }

    @Test
    fun goalHitStreak_skipsLegacyThenStopsOnMiss() {
        val newestFirst = listOf(
            session(night3, night3.plusSeconds(1000), goal = night3.plusSeconds(60)), // hit
            session(night2, night2.plusSeconds(1000), goal = null), // skip
            session(night1, night1.plusSeconds(1000), goal = night1.minusSeconds(60)), // miss
        )
        assertThat(TrendsAnalytics.goalHitStreak(newestFirst)).isEqualTo(1)
    }

    @Test
    fun homeToBed_matchesNearestEnterWithinWindow() {
        val enter = LifeEvent(
            type = LifeEventType.ENTER_HOME,
            occurredAt = night1.minusSeconds(45 * 60),
        )
        val minutes = TrendsAnalytics.matchHomeToBedMinutes(night1, listOf(enter))
        assertThat(minutes).isEqualTo(45)
    }

    @Test
    fun homeToBed_ignoresEnterOutsideWindow() {
        val enter = LifeEvent(
            type = LifeEventType.ENTER_HOME,
            occurredAt = night1.minusSeconds(20 * 3600),
        )
        assertThat(TrendsAnalytics.matchHomeToBedMinutes(night1, listOf(enter))).isNull()
    }

    @Test
    fun validateSessionBounds_requiresEndAfterStart() {
        val start = night1
        assertThat(TrendsAnalytics.validateSessionBounds(start, start)).isNotNull()
        assertThat(TrendsAnalytics.validateSessionBounds(start, start.minusSeconds(1))).isNotNull()
        assertThat(TrendsAnalytics.validateSessionBounds(start, start.plusSeconds(60))).isNull()
        assertThat(TrendsAnalytics.validateSessionBounds(start, null)).isNull()
    }

    @Test
    fun medianHomeToBed_computedFromMatchedNights() {
        val sessions = listOf(
            session(night1, night1.plusSeconds(1000)),
            session(night2, night2.plusSeconds(1000)),
            session(night3, night3.plusSeconds(1000)),
        )
        val enters = listOf(
            LifeEvent(type = LifeEventType.ENTER_HOME, occurredAt = night1.minusSeconds(30 * 60)),
            LifeEvent(type = LifeEventType.ENTER_HOME, occurredAt = night2.minusSeconds(60 * 60)),
            LifeEvent(type = LifeEventType.ENTER_HOME, occurredAt = night3.minusSeconds(90 * 60)),
        )
        val snap = TrendsAnalytics.compute(sessions, enters)
        assertThat(snap.medianHomeToBedMinutes).isEqualTo(60)
    }

    private fun session(
        start: Instant,
        end: Instant,
        goal: Instant? = null,
    ) = SleepSession(
        id = start.epochSecond,
        startAt = start,
        endAt = end,
        source = SleepSource.MANUAL,
        goalAtStart = goal,
    )
}
