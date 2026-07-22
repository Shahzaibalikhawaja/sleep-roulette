package com.sleeproulette.app.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Samples interactive phone use via UsageStatsManager.
 * Requires the special Usage Access grant (not a normal runtime permission).
 */
@Singleton
class UsageStatsSampler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /**
     * Rough count of minutes where *some* app was in the foreground between [from] and [to].
     *
     * Uses ACTIVITY_RESUMED / ACTIVITY_PAUSED only — MOVE_TO_FOREGROUND / BACKGROUND
     * were deprecated in API 29.
     */
    fun interactiveForegroundMinutes(from: Instant, to: Instant): Long {
        if (!hasUsageAccess()) return 0L
        if (!to.isAfter(from)) return 0L

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(from.toEpochMilli(), to.toEpochMilli())
        val event = UsageEvents.Event()

        var lastResumeMs: Long? = null
        var totalMs = 0L
        val ownPackage = context.packageName

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // Ignore our own package so opening Sleep Roulette doesn't count against you.
            if (event.packageName == ownPackage) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastResumeMs = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = lastResumeMs
                    if (start != null && event.timeStamp >= start) {
                        totalMs += (event.timeStamp - start)
                    }
                    lastResumeMs = null
                }
            }
        }

        lastResumeMs?.let { start ->
            totalMs += (to.toEpochMilli() - start).coerceAtLeast(0L)
        }

        return totalMs / 60_000L
    }
}
