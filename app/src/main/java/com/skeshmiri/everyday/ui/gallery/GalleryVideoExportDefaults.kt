package com.skeshmiri.everyday.ui.gallery

import com.skeshmiri.everyday.model.DailyPhoto
import java.util.Locale
import kotlin.math.round

object GalleryVideoExportDefaults {
    val fpsPresets: List<Int> = listOf(3, 5, 8)
    const val defaultFps: Int = 5

    fun estimatedDurationSeconds(photoCount: Int, fps: Int): Double {
        if (photoCount <= 0 || fps <= 0) {
            return 0.0
        }
        return round((photoCount.toDouble() / fps.toDouble()) * 10.0) / 10.0
    }

    fun formatDurationSeconds(durationSeconds: Double): String =
        String.format(Locale.getDefault(), "%.1f", durationSeconds)

    fun sortPhotosForExport(photos: List<DailyPhoto>): List<DailyPhoto> =
        photos.sortedWith(
            compareBy<DailyPhoto> { it.capturedAt }
                .thenBy { it.id },
        )
}
