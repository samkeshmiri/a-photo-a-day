package com.skeshmiri.everyday.di

import android.content.Context
import com.skeshmiri.everyday.camera.CameraController
import com.skeshmiri.everyday.camera.CameraXCameraController
import com.skeshmiri.everyday.data.DailyPhotoRepository
import com.skeshmiri.everyday.data.MediaStoreDailyPhotoRepository
import com.skeshmiri.everyday.domain.DailyCapturePolicy
import com.skeshmiri.everyday.domain.DefaultDailyCapturePolicy
import com.skeshmiri.everyday.export.GalleryVideoExporter
import com.skeshmiri.everyday.export.MediaStoreGalleryVideoExporter
import com.skeshmiri.everyday.notifications.DailyReminderScheduler
import com.skeshmiri.everyday.storage.TempPhotoStore
import com.skeshmiri.everyday.ui.camera.CameraOverlayPreferences
import java.time.Clock

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()
    val tempPhotoStore = TempPhotoStore(appContext, clock)
    val dailyPhotoRepository: DailyPhotoRepository = MediaStoreDailyPhotoRepository(appContext, clock)
    val galleryVideoExporter: GalleryVideoExporter = MediaStoreGalleryVideoExporter(appContext, clock)
    val dailyCapturePolicy: DailyCapturePolicy = DefaultDailyCapturePolicy()
    val dailyReminderScheduler = DailyReminderScheduler(appContext, clock)
    val cameraOverlayPreferences = CameraOverlayPreferences(appContext)

    fun createCameraController(): CameraController =
        CameraXCameraController(
            context = appContext,
            tempPhotoStore = tempPhotoStore,
        )
}
