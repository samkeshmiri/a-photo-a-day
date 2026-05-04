package com.skeshmiri.aphotoaday.data

import com.skeshmiri.aphotoaday.model.DailyPhoto
import java.io.File

interface DailyPhotoRepository {
    suspend fun getToday(dateKey: String): DailyPhoto?
    suspend fun listAll(): List<DailyPhoto>
    suspend fun saveFromTemp(tempFile: File, dateKey: String): DailyPhoto
}

