package com.skeshmiri.aphotoaday

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.skeshmiri.aphotoaday.di.AppContainer
import com.skeshmiri.aphotoaday.ui.EverydayApp
import com.skeshmiri.aphotoaday.ui.onboarding.PermissionIntroScreen
import com.skeshmiri.aphotoaday.ui.theme.EverydayTheme

class MainActivity : ComponentActivity() {
    private var showPermissionIntro by mutableStateOf(false)
    private val appContainer by lazy { AppContainer(applicationContext) }
    private val cameraController by lazy { appContainer.createCameraController() }
    private val permissionIntroPreferences by lazy {
        getSharedPreferences(PERMISSION_INTRO_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
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
        showPermissionIntro = shouldShowStartupPermissionIntro()

        setContent {
            EverydayTheme {
                if (showPermissionIntro) {
                    PermissionIntroScreen(
                        onContinue = {
                            permissionIntroPreferences.edit()
                                .putBoolean(PERMISSION_INTRO_SEEN_KEY, true)
                                .apply()
                            showPermissionIntro = false
                            requestStartupPermissionsIfNeeded()
                        },
                    )
                } else {
                    EverydayApp(
                        container = appContainer,
                        cameraController = cameraController,
                    )
                }
            }
        }

        if (!showPermissionIntro) {
            requestStartupPermissionsIfNeeded()
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
        if (needsNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return true
        }
        return false
    }

    private fun shouldShowStartupPermissionIntro(): Boolean =
        !permissionIntroPreferences.getBoolean(PERMISSION_INTRO_SEEN_KEY, false) &&
            (needsNotificationPermission() || !hasMediaReadPermission())

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED

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

    private companion object {
        const val PERMISSION_INTRO_PREFERENCES_NAME = "permission_intro"
        const val PERMISSION_INTRO_SEEN_KEY = "seen"
    }
}
