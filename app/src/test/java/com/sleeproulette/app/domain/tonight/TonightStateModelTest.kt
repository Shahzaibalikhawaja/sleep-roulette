package com.sleeproulette.app.domain.tonight

import com.google.common.truth.Truth.assertThat
import com.sleeproulette.app.domain.model.SleepSession
import com.sleeproulette.app.domain.model.SleepSource
import org.junit.Test
import java.time.Instant

class TonightStateModelTest {

    private val t0 = Instant.parse("2026-07-24T18:00:00Z")

    @Test
    fun sleeping_whenOngoing() {
        val ongoing = session(start = t0)
        val phase = TonightStateModel.derive(
            ongoing = ongoing,
            lastCompleted = null,
            latestEnterHome = t0.minusSeconds(3600),
            latestExitHome = null,
            now = t0.plusSeconds(600),
        )
        assertThat(phase).isEqualTo(TonightPhase.Sleeping)
    }

    @Test
    fun windDown_whenEnterHomeAfterLastSleep() {
        val completed = session(
            start = t0.minusSeconds(50_000),
            end = t0.minusSeconds(20_000),
        )
        val enter = t0.minusSeconds(600)
        val phase = TonightStateModel.derive(
            ongoing = null,
            lastCompleted = completed,
            latestEnterHome = enter,
            latestExitHome = t0.minusSeconds(30_000),
            now = t0,
        )
        assertThat(phase).isEqualTo(TonightPhase.WindDown)
    }

    @Test
    fun complete_soonAfterWake_evenAtHome() {
        val end = t0.minusSeconds(3600)
        val completed = session(start = end.minusSeconds(28_800), end = end)
        val phase = TonightStateModel.derive(
            ongoing = null,
            lastCompleted = completed,
            latestEnterHome = end.minusSeconds(40_000),
            latestExitHome = null,
            now = t0,
        )
        assertThat(phase).isEqualTo(TonightPhase.Complete)
    }

    @Test
    fun away_whenLeftHomeAndNoRecentSleep() {
        val phase = TonightStateModel.derive(
            ongoing = null,
            lastCompleted = null,
            latestEnterHome = t0.minusSeconds(10_000),
            latestExitHome = t0.minusSeconds(100),
            now = t0,
        )
        assertThat(phase).isEqualTo(TonightPhase.Away)
    }

    private fun session(start: Instant, end: Instant? = null) = SleepSession(
        id = 1,
        startAt = start,
        endAt = end,
        source = SleepSource.MANUAL,
    )
}
