package com.skeshmiri.aphotoaday

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.skeshmiri.aphotoaday.di.AppContainer
import com.skeshmiri.aphotoaday.ui.EverydayApp
import com.skeshmiri.aphotoaday.ui.theme.EverydayTheme

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }
    private val cameraController by lazy { appContainer.createCameraController() }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Reminders keep scheduling even if the user denies the permission.
        requestMediaReadPermissionIfNeeded()
    }
    private val mediaReadPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* the gallery still shows app-owned photos if the user denies access */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appContainer.tempPhotoStore.cleanupStaleFiles()
        appContainer.dailyReminderScheduler.initialize()
        requestStartupPermissionsIfNeeded()

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

    private fun requestStartupPermissionsIfNeeded() {
        if (!requestNotificationPermissionIfNeeded()) {
            requestMediaReadPermissionIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

        val isGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return true
        }
        return false
    }

    private fun requestMediaReadPermissionIfNeeded(): Boolean {
        if (!hasMediaReadPermission()) {
            mediaReadPermissionLauncher.launch(mediaReadPermissions())
            return true
        }
        return false
    }

    private fun hasMediaReadPermission(): Boolean {
        val permissions = mediaReadPermissions()
        return permissions.any { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun mediaReadPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
