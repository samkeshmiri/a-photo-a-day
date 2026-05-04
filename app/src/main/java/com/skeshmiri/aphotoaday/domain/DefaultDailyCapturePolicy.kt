package com.skeshmiri.aphotoaday.domain

import com.skeshmiri.aphotoaday.model.DailyPhoto

class DefaultDailyCapturePolicy : DailyCapturePolicy {
    override fun canCapture(todayPhoto: DailyPhoto?): Boolean = todayPhoto == null
}

