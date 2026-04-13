package com.skeshmiri.everyday.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Clock

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        DailyReminderScheduler(
            context = context.applicationContext,
            clock = Clock.systemDefaultZone(),
        ).initialize()
    }
}
