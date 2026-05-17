package com.skeshmiri.aphotoaday.ui.camera

data class CameraGuideSettings(
    val verticalGuideProgress: Float = DEFAULT_VERTICAL_GUIDE_PROGRESS,
    val horizontalGuideProgress: Float = DEFAULT_HORIZONTAL_GUIDE_PROGRESS,
) {
    fun normalized(): CameraGuideSettings = copy(
        verticalGuideProgress = verticalGuideProgress.coerceIn(MIN_PROGRESS, MAX_PROGRESS),
        horizontalGuideProgress = horizontalGuideProgress.coerceIn(MIN_PROGRESS, MAX_PROGRESS),
    )

    companion object {
        const val MIN_PROGRESS = 0f
        const val MAX_PROGRESS = 1f
        const val DEFAULT_VERTICAL_GUIDE_PROGRESS = 0.5f
        const val DEFAULT_HORIZONTAL_GUIDE_PROGRESS = 0.5f
    }
}
