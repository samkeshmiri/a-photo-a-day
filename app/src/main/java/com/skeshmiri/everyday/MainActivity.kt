package com.skeshmiri.everyday

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.skeshmiri.everyday.di.AppContainer
import com.skeshmiri.everyday.ui.EverydayApp
import com.skeshmiri.everyday.ui.theme.EverydayTheme

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }
    private val cameraController by lazy { appContainer.createCameraController() }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* reminders keep scheduling even if the user denies the permission */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appContainer.tempPhotoStore.cleanupStaleFiles()
        appContainer.dailyReminderScheduler.initialize()
        requestNotificationPermissionIfNeeded()

        setContent {
            EverydayTheme {
                EverydayApp(
                    container = appContainer,
                    cameraController = cameraController,
                )
            }
        }
    }

    override fun onDestroy() {
        cameraController.close()
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val isGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
