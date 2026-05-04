package com.skeshmiri.aphotoaday.domain

import com.skeshmiri.aphotoaday.model.DailyPhoto

fun interface DailyCapturePolicy {
    fun canCapture(todayPhoto: DailyPhoto?): Boolean
}

