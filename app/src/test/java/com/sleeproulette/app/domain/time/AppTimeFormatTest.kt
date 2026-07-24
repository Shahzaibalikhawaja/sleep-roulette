package com.sleeproulette.app.domain.time

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

class AppTimeFormatTest {

    @Test
    fun formats24Hour() {
        assertThat(AppTimeFormat.formatLocalTime(LocalTime.of(4, 5), use24HourClock = true))
            .isEqualTo("04:05")
        assertThat(AppTimeFormat.formatMinutesFromMidnight(16 * 60 + 30, use24HourClock = true))
            .isEqualTo("16:30")
    }

    @Test
    fun formats12Hour() {
        assertThat(AppTimeFormat.formatLocalTime(LocalTime.of(4, 5), use24HourClock = false))
            .isEqualTo("4:05 AM")
        assertThat(AppTimeFormat.formatMinutesFromMidnight(16 * 60 + 30, use24HourClock = false))
            .isEqualTo("4:30 PM")
    }
}
