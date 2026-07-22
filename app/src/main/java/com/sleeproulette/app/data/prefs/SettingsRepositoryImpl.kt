package com.sleeproulette.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sleeproulette.app.domain.model.BedtimeGoalMode
import com.sleeproulette.app.domain.model.UserSettings
import com.sleeproulette.app.domain.repo.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sleep_roulette_settings",
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val GOAL_MODE = stringPreferencesKey("goal_mode")
        val FIXED_MINUTES = intPreferencesKey("fixed_goal_minutes")
        val BEFORE_SUNRISE = intPreferencesKey("minutes_before_sunrise")
        val USAGE_THRESHOLD = intPreferencesKey("usage_nudge_threshold")
        val BATTERY_ACK = booleanPreferencesKey("battery_opt_ack")
    }

    override val settings: Flow<UserSettings> =
        context.settingsDataStore.data.map { prefs -> prefs.toSettings() }

    override suspend fun current(): UserSettings = settings.first()

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.ONBOARDING] = next.onboardingComplete
            prefs[Keys.GOAL_MODE] = next.goalMode.name
            prefs[Keys.FIXED_MINUTES] = next.fixedGoalMinutesFromMidnight
            prefs[Keys.BEFORE_SUNRISE] = next.minutesBeforeSunrise
            prefs[Keys.USAGE_THRESHOLD] = next.usageNudgeThresholdMinutes
            prefs[Keys.BATTERY_ACK] = next.batteryOptimizationAcknowledged
        }
    }

    private fun Preferences.toSettings(): UserSettings {
        val modeName = this[Keys.GOAL_MODE]
        val mode = modeName
            ?.let { runCatching { BedtimeGoalMode.valueOf(it) }.getOrNull() }
            ?: BedtimeGoalMode.FIXED_CLOCK
        return UserSettings(
            onboardingComplete = this[Keys.ONBOARDING] ?: false,
            goalMode = mode,
            fixedGoalMinutesFromMidnight = this[Keys.FIXED_MINUTES] ?: (4 * 60),
            minutesBeforeSunrise = this[Keys.BEFORE_SUNRISE] ?: 90,
            usageNudgeThresholdMinutes = this[Keys.USAGE_THRESHOLD] ?: 10,
            batteryOptimizationAcknowledged = this[Keys.BATTERY_ACK] ?: false,
        )
    }
}
