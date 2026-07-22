package com.sleeproulette.app.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sleeproulette.app.MainActivity
import com.sleeproulette.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_COUNTDOWN = "sleep_countdown"
        const val CHANNEL_NUDGE = "bedtime_nudge"
        const val ID_COUNTDOWN = 1001
        const val ID_NUDGE = 1002
    }

    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COUNTDOWN,
                context.getString(R.string.channel_countdown_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_countdown_desc)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NUDGE,
                context.getString(R.string.channel_nudge_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_nudge_desc)
            },
        )
    }

    fun buildCountdownNotification(
        goal: ZonedDateTime,
        sunrise: ZonedDateTime?,
        remaining: Duration,
    ): Notification {
        val remainingText = formatDuration(remaining)
        val sunriseText = sunrise?.format(timeFmt) ?: "—"
        val content = context.getString(R.string.goal_bedtime) +
            ": ${goal.format(timeFmt)}  ·  " +
            context.getString(R.string.sunrise) +
            ": $sunriseText"

        return NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${context.getString(R.string.notif_countdown_title)} — $remainingText")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent())
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    fun showNudge() {
        val notification = NotificationCompat.Builder(context, CHANNEL_NUDGE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_nudge_title))
            .setContentText(context.getString(R.string.notif_nudge_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(ID_NUDGE, notification)
        }
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.seconds
        if (totalSeconds <= 0) return "past goal"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
