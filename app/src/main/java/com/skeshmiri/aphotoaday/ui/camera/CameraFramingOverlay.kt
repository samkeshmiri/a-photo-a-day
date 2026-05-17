package com.skeshmiri.aphotoaday.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skeshmiri.aphotoaday.ui.theme.BlushPop

const val CameraFramingOverlayTag = "camera_framing_overlay"

private val DefaultFramingOverlayColor = BlushPop

private const val MinimumEyeGuideOffset = 0.04f
private const val MaximumEyeGuideOffset = 0.14f
private const val TopEyeGuideY = 0.35f
private const val BottomEyeGuideY = 0.61f
private const val HorizontalInsetFraction = 0.04f
private const val VerticalInsetFraction = 0.03f

@Composable
fun CameraFramingOverlay(
    modifier: Modifier = Modifier,
    guideSettings: CameraGuideSettings = CameraGuideSettings(),
    strokeColor: Color = DefaultFramingOverlayColor,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(
        modifier = modifier.testTag(CameraFramingOverlayTag),
    ) {
        val lineColor = strokeColor.copy(alpha = 0.5f)
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )

        val horizontalStartX = size.width * HorizontalInsetFraction
        val horizontalEndX = size.width * (1f - HorizontalInsetFraction)
        val verticalStartY = size.height * VerticalInsetFraction
        val verticalEndY = size.height * (1f - VerticalInsetFraction)
        val normalizedSettings = guideSettings.normalized()
        val eyeGuideOffset = lerp(
            start = MaximumEyeGuideOffset,
            stop = MinimumEyeGuideOffset,
            fraction = normalizedSettings.verticalGuideProgress,
        )
        val leftEyeGuideX = 0.5f - eyeGuideOffset
        val rightEyeGuideX = 0.5f + eyeGuideOffset
        val eyesGuideY = lerp(
            start = TopEyeGuideY,
            stop = BottomEyeGuideY,
            fraction = normalizedSettings.horizontalGuideProgress,
        )

        drawLine(
            color = lineColor,
            start = Offset(horizontalStartX, size.height * eyesGuideY),
            end = Offset(horizontalEndX, size.height * eyesGuideY),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = lineColor,
            start = Offset(size.width * leftEyeGuideX, verticalStartY),
            end = Offset(size.width * leftEyeGuideX, verticalEndY),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = lineColor,
            start = Offset(size.width * rightEyeGuideX, verticalStartY),
            end = Offset(size.width * rightEyeGuideX, verticalEndY),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + ((stop - start) * fraction.coerceIn(CameraGuideSettings.MIN_PROGRESS, CameraGuideSettings.MAX_PROGRESS))
