package com.sleeproulette.app.domain.sunrise

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class SunriseCalculatorTest {

    @Test
    fun sunrise_forKarachi_isMorningLocal() {
        // Approximate Karachi coordinates
        val lat = 24.8607
        val lng = 67.0011
        val date = LocalDate.of(2026, 6, 21)

        val sunrise = SunriseCalculator.sunrise(lat, lng, date, ZoneOffset.ofHours(5))
        assertThat(sunrise).isNotNull()
        val hour = sunrise!!.hour
        // June sunrise in Karachi is typically around 05:40–06:00 PKT
        assertThat(hour).isAtLeast(4)
        assertThat(hour).isAtMost(7)
    }

    @Test
    fun sunriseMinutesUtc_isFiniteForEquator() {
        val minutes = SunriseCalculator.sunriseMinutesUtc(0.0, 0.0, LocalDate.of(2026, 3, 20))
        assertThat(minutes).isNotNull()
        assertThat(minutes!!).isGreaterThan(0.0)
        assertThat(minutes).isLessThan(24 * 60.0)
    }
}
