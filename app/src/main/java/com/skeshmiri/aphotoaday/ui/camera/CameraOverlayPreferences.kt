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
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

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

    private companion object {
        private const val PREFERENCES_NAME = "camera_overlay_preferences"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    }
}
