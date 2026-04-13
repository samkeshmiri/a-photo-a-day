package com.skeshmiri.everyday.notifications

import android.Manifest
import android.content.BroadcastReceiver.PendingResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.skeshmiri.everyday.R
import com.skeshmiri.everyday.data.MediaStoreDailyPhotoRepository
import com.skeshmiri.everyday.domain.DailyPhotoNaming
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            handleReminder(context.applicationContext, pendingResult)
        }
    }

    private suspend fun handleReminder(
        context: Context,
        pendingResult: PendingResult,
    ) {
        try {
            val clock = Clock.systemDefaultZone()
            val scheduler = DailyReminderScheduler(context, clock)
            val repository = MediaStoreDailyPhotoRepository(context, clock)
            val todayDateKey = DailyPhotoNaming.todayDateKey(clock)
            val hasPhotoToday = repository.getToday(todayDateKey) != null

            if (!hasPhotoToday && notificationsEnabled(context)) {
                scheduler.ensureNotificationChannel()
                NotificationManagerCompat.from(context).notify(
                    DailyReminderScheduler.NOTIFICATION_ID,
                    buildReminderNotification(context),
                )
            }

            scheduler.markHandled(LocalDate.now(clock))
            scheduler.scheduleNextReminder()
        } finally {
            pendingResult.finish()
        }
    }

    private fun notificationsEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildReminderNotification(context: Context) =
        NotificationCompat.Builder(context, DailyReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(DailyReminderScheduler.contentPendingIntent(context))
            .build()
}
