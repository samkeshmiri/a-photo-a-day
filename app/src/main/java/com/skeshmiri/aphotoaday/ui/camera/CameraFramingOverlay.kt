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

private const val LeftEyeGuideX = 0.41f
private const val RightEyeGuideX = 0.59f
private const val EyesGuideY = 0.48f
private const val HorizontalInsetFraction = 0.04f
private const val VerticalInsetFraction = 0.03f

@Composable
fun CameraFramingOverlay(
    modifier: Modifier = Modifier,
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

        drawLine(
            color = lineColor,
            start = Offset(horizontalStartX, size.height * EyesGuideY),
            end = Offset(horizontalEndX, size.height * EyesGuideY),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = lineColor,
            start = Offset(size.width * LeftEyeGuideX, verticalStartY),
            end = Offset(size.width * LeftEyeGuideX, verticalEndY),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = lineColor,
            start = Offset(size.width * RightEyeGuideX, verticalStartY),
            end = Offset(size.width * RightEyeGuideX, verticalEndY),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}
