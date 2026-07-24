package com.sleeproulette.app.domain.time

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

/**
 * App-wide clock display helpers driven by [UserSettings.use24HourClock].
 */
object AppTimeFormat {

    fun timePattern(use24HourClock: Boolean): String =
        if (use24HourClock) "HH:mm" else "h:mm a"

    fun dateTimePattern(use24HourClock: Boolean): String =
        if (use24HourClock) "EEE d MMM HH:mm" else "EEE d MMM h:mm a"

    fun weekdayTimePattern(use24HourClock: Boolean): String =
        if (use24HourClock) "EEE HH:mm" else "EEE h:mm a"

    fun timeFormatter(
        use24HourClock: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DateTimeFormatter =
        DateTimeFormatter.ofPattern(timePattern(use24HourClock)).withZone(zone)

    fun dateTimeFormatter(
        use24HourClock: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DateTimeFormatter =
        DateTimeFormatter.ofPattern(dateTimePattern(use24HourClock)).withZone(zone)

    fun weekdayTimeFormatter(
        use24HourClock: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DateTimeFormatter =
        DateTimeFormatter.ofPattern(weekdayTimePattern(use24HourClock)).withZone(zone)

    /** For [ZonedDateTime] / [LocalTime] (already temporal; no zone needed on formatter). */
    fun formatTemporal(value: TemporalAccessor, use24HourClock: Boolean): String =
        DateTimeFormatter.ofPattern(timePattern(use24HourClock)).format(value)

    fun formatInstant(value: Instant, use24HourClock: Boolean, zone: ZoneId = ZoneId.systemDefault()): String =
        timeFormatter(use24HourClock, zone).format(value)

    fun formatLocalTime(value: LocalTime, use24HourClock: Boolean): String =
        formatTemporal(value, use24HourClock)

    fun formatMinutesFromMidnight(minutes: Int, use24HourClock: Boolean): String {
        val clamped = minutes.coerceIn(0, 24 * 60 - 1)
        return formatLocalTime(LocalTime.of(clamped / 60, clamped % 60), use24HourClock)
    }
}
