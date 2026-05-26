package com.skeshmiri.aphotoaday.ui.camera

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraOverlayPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _isOverlayEnabled = MutableStateFlow(
        preferences.getBoolean(KEY_OVERLAY_ENABLED, true),
    )
    private val _guideSettings = MutableStateFlow(
        CameraGuideSettings(
            verticalGuideProgress = preferences.getFloat(
                KEY_VERTICAL_GUIDE_PROGRESS,
                CameraGuideSettings.DEFAULT_VERTICAL_GUIDE_PROGRESS,
            ),
            horizontalGuideProgress = preferences.getFloat(
                KEY_HORIZONTAL_GUIDE_PROGRESS,
                CameraGuideSettings.DEFAULT_HORIZONTAL_GUIDE_PROGRESS,
            ),
        ).normalized(),
    )

    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()
    val guideSettings: StateFlow<CameraGuideSettings> = _guideSettings.asStateFlow()

    fun setOverlayEnabled(enabled: Boolean) {
        if (_isOverlayEnabled.value == enabled) return
        preferences.edit()
            .putBoolean(KEY_OVERLAY_ENABLED, enabled)
            .apply()
        _isOverlayEnabled.value = enabled
    }

    fun toggleOverlay() {
        setOverlayEnabled(!_isOverlayEnabled.value)
    }

    fun setVerticalGuideProgress(progress: Float) {
        setGuideSettings(_guideSettings.value.copy(verticalGuideProgress = progress))
    }

    fun setHorizontalGuideProgress(progress: Float) {
        setGuideSettings(_guideSettings.value.copy(horizontalGuideProgress = progress))
    }

    fun setGuideSettings(settings: CameraGuideSettings) {
        val normalizedSettings = settings.normalized()
        if (_guideSettings.value == normalizedSettings) return
        preferences.edit()
            .putFloat(KEY_VERTICAL_GUIDE_PROGRESS, normalizedSettings.verticalGuideProgress)
            .putFloat(KEY_HORIZONTAL_GUIDE_PROGRESS, normalizedSettings.horizontalGuideProgress)
            .apply()
        _guideSettings.value = normalizedSettings
    }

    private companion object {
        private const val PREFERENCES_NAME = "camera_overlay_preferences"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_VERTICAL_GUIDE_PROGRESS = "vertical_guide_progress"
        private const val KEY_HORIZONTAL_GUIDE_PROGRESS = "horizontal_guide_progress"
    }
}
