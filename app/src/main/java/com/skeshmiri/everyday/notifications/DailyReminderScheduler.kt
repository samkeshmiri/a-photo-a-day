package com.skeshmiri.everyday.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.skeshmiri.everyday.MainActivity
import com.skeshmiri.everyday.R
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyReminderScheduler(
    context: Context,
    private val clock: Clock,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun initialize() {
        ensureNotificationChannel()
        scheduleNextReminder()
    }

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.notification_channel_description)
        }

        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleNextReminder() {
        val now = ZonedDateTime.now(clock)
        val lastHandledDate = getLastHandledDate()
        val triggerAt = ReminderScheduleCalculator.nextTriggerAt(now, lastHandledDate)

        alarmManager.cancel(reminderPendingIntent(appContext))
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerAt.toInstant().toEpochMilli(),
            reminderPendingIntent(appContext),
        )
    }

    fun markHandled(date: LocalDate = LocalDate.now(clock)) {
        preferences.edit()
            .putString(KEY_LAST_HANDLED_DATE, date.toString())
            .apply()
    }

    private fun getLastHandledDate(): LocalDate? =
        preferences.getString(KEY_LAST_HANDLED_DATE, null)?.let(LocalDate::parse)

    companion object {
        const val CHANNEL_ID = "daily_photo_reminder"
        const val NOTIFICATION_ID = 1001

        private const val PREFERENCES_NAME = "daily_reminder"
        private const val KEY_LAST_HANDLED_DATE = "last_handled_date"
        private const val REQUEST_CODE_REMINDER = 3001

        fun reminderPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, DailyReminderReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_REMINDER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun contentPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

internal object ReminderScheduleCalculator {
    private const val START_HOUR = 9
    private const val END_HOUR = 18
    private const val WINDOW_MINUTES = (END_HOUR - START_HOUR) * 60
    private val minimumLeadTime: Duration = Duration.ofMinutes(5)

    fun nextTriggerAt(
        now: ZonedDateTime,
        lastHandledDate: LocalDate?,
    ): ZonedDateTime {
        val today = now.toLocalDate()
        val todayWindowEnd = today.atTime(END_HOUR, 0).atZone(now.zone)
        if (lastHandledDate == today || !now.isBefore(todayWindowEnd)) {
            return triggerForDay(today.plusDays(1), now.zone)
        }

        val todayTrigger = triggerForDay(today, now.zone)
        val earliestAvailable = now.plus(minimumLeadTime)
        return when {
            todayTrigger.isAfter(earliestAvailable) -> todayTrigger
            earliestAvailable.isBefore(todayWindowEnd) -> earliestAvailable
            else -> triggerForDay(today.plusDays(1), now.zone)
        }
    }

    fun triggerForDay(
        date: LocalDate,
        zoneId: ZoneId,
    ): ZonedDateTime {
        val dayStart = date.atTime(START_HOUR, 0).atZone(zoneId)
        val offsetMinutes = floorMod(date.toEpochDay().toInt() * 37 + 17, WINDOW_MINUTES)
        return dayStart.plusMinutes(offsetMinutes.toLong())
    }

    private fun floorMod(value: Int, divisor: Int): Int = ((value % divisor) + divisor) % divisor
}
