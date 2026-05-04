package com.skeshmiri.aphotoaday.export

import android.net.Uri
import com.skeshmiri.aphotoaday.model.DailyPhoto

data class ExportedGalleryVideo(
    val uri: Uri,
    val displayName: String,
    val relativePath: String,
    val fps: Int,
    val frameCount: Int,
    val durationSeconds: Double,
)

interface GalleryVideoExporter {
    suspend fun export(photos: List<DailyPhoto>, fps: Int): ExportedGalleryVideo
}
