package com.sleeproulette.app.domain.model

/**
 * Stable place identifiers used by geofencing and analytics.
 * Prefer enums over free-form strings for types that drive logic.
 */
enum class PlaceKind {
    HOME,
    WORK,
}

/**
 * Append-only life events — the observability spine of the app.
 * Analytics and interventions both read this stream.
 */
enum class LifeEventType {
    ENTER_HOME,
    EXIT_HOME,
    ENTER_WORK,
    EXIT_WORK,
    USAGE_SPIKE,
    BEDTIME_GOAL_HIT,
    BEDTIME_GOAL_MISSED,
    COUNTDOWN_STARTED,
    COUNTDOWN_STOPPED,
}

enum class SleepSource {
    MANUAL,
    SUGGESTED,
}

enum class BedtimeGoalMode {
    /** Fixed clock time, e.g. 04:00 local. */
    FIXED_CLOCK,

    /** N minutes before today's sunrise at Home. */
    MINUTES_BEFORE_SUNRISE,
}
