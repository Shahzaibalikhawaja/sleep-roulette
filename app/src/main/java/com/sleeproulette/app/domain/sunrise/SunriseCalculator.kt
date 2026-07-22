package com.sleeproulette.app.domain.sunrise

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Local sunrise calculation (no network).
 *
 * Based on the NOAA solar calculator approximation.
 * Pure Kotlin — easy to unit test without Android framework.
 *
 * Learning note: keep science/math in the domain layer so UI and Android
 * APIs never own the formula. That makes bugs easier to catch with JUnit.
 */
object SunriseCalculator {

    /**
     * @return Local sunrise at [latitude]/[longitude] on [date] in [zone],
     *         or null near polar day/night when the sun does not rise.
     */
    fun sunrise(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ZonedDateTime? {
        val minutes = sunriseMinutesUtc(latitude, longitude, date) ?: return null
        val hours = minutes / 60.0
        val h = floor(hours).toInt().coerceIn(0, 23)
        val m = ((hours - h) * 60.0).toInt().coerceIn(0, 59)
        val utc = ZonedDateTime.of(date, LocalTime.of(h, m), ZoneId.of("UTC"))
        return utc.withZoneSameInstant(zone)
    }

    /**
     * Returns sunrise as fractional minutes past UTC midnight, or null if undefined.
     */
    internal fun sunriseMinutesUtc(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
    ): Double? {
        val dayOfYear = date.dayOfYear.toDouble()
        val zenith = 90.833 // official sunrise zenith (degrees)

        // Convert longitude to hour value and calculate approximate time
        val lngHour = longitude / 15.0
        val t = dayOfYear + ((6.0 - lngHour) / 24.0)

        val m = (0.9856 * t) - 3.289
        var l = m + (1.916 * sin(rad(m))) + (0.020 * sin(rad(2 * m))) + 282.634
        l = normalizeDegrees(l)

        var ra = deg(kotlin.math.atan(0.91764 * tan(rad(l))))
        ra = normalizeDegrees(ra)

        val lQuadrant = (floor(l / 90.0) * 90.0)
        val raQuadrant = (floor(ra / 90.0) * 90.0)
        ra += (lQuadrant - raQuadrant)
        ra /= 15.0

        val sinDec = 0.39782 * sin(rad(l))
        val cosDec = cos(asin(sinDec))

        val cosH = (cos(rad(zenith)) - (sinDec * sin(rad(latitude)))) /
            (cosDec * cos(rad(latitude)))

        if (cosH > 1.0 || cosH < -1.0) {
            // Sun never rises / never sets at this location on this date
            return null
        }

        val h = (360.0 - deg(acos(cosH))) / 15.0
        val time = h + ra - (0.06571 * t) - 6.622
        var ut = time - lngHour
        ut = normalizeHours(ut)
        return ut * 60.0
    }

    private fun rad(deg: Double): Double = deg * PI / 180.0
    private fun deg(rad: Double): Double = rad * 180.0 / PI

    private fun normalizeDegrees(value: Double): Double {
        var v = value % 360.0
        if (v < 0) v += 360.0
        return v
    }

    private fun normalizeHours(value: Double): Double {
        var v = value % 24.0
        if (v < 0) v += 24.0
        return v
    }
}
