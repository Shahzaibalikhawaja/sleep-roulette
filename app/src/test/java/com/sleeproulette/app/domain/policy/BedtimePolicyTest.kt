package com.sleeproulette.app.domain.policy

import com.google.common.truth.Truth.assertThat
import com.sleeproulette.app.domain.model.BedtimeGoalMode
import com.sleeproulette.app.domain.model.UserSettings
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class BedtimePolicyTest {

    private val zone: ZoneId = ZoneId.of("Asia/Karachi")
    // Karachi-ish
    private val lat = 24.8607
    private val lng = 67.0011

    @Test
    fun fixedClock_goalUsesTomorrowWhenAfterNoon() {
        val settings = UserSettings(
            goalMode = BedtimeGoalMode.FIXED_CLOCK,
            fixedGoalMinutesFromMidnight = 4 * 60, // 04:00
        )
        val now = LocalDateTime.of(2026, 7, 22, 22, 0).atZone(zone).toInstant()

        val window = BedtimePolicy.resolveSleepWindow(
            settings = settings,
            homeLatitude = lat,
            homeLongitude = lng,
            arrivedHomeAt = now,
            now = now,
            zone = zone,
        )

        assertThat(window.goalBedtime.toLocalDate().toString()).isEqualTo("2026-07-23")
        assertThat(window.goalBedtime.hour).isEqualTo(4)
        assertThat(window.goalBedtime.minute).isEqualTo(0)
    }

    @Test
    fun shouldNudge_onlyAfterGoalAndThreshold() {
        val settings = UserSettings(usageNudgeThresholdMinutes = 10)
        val goal = LocalDateTime.of(2026, 7, 23, 4, 0).atZone(zone)
        val window = BedtimePolicy.SleepWindow(
            goalBedtime = goal,
            sunrise = null,
            arrivedHomeAt = Instant.parse("2026-07-23T00:00:00Z"),
        )

        assertThat(
            BedtimePolicy.shouldNudgeForUsage(
                settings,
                window,
                interactiveForegroundMinutesSinceGoal = 5,
                now = goal.toInstant().plusSeconds(600),
            ),
        ).isFalse()

        assertThat(
            BedtimePolicy.shouldNudgeForUsage(
                settings,
                window,
                interactiveForegroundMinutesSinceGoal = 10,
                now = goal.toInstant().plusSeconds(600),
            ),
        ).isTrue()
    }
}
