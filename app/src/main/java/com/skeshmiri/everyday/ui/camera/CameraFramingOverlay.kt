package com.skeshmiri.everyday.ui.camera

import android.content.Context
import android.util.Xml
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import org.xmlpull.v1.XmlPullParser

const val CameraFramingOverlayTag = "camera_framing_overlay"

private const val CameraFramingOverlayAssetPath = "camera-overlays/head_shoulders_overlay.svg"

private val DefaultFramingOverlayColor = Color(0xFFF3F0E8)

@Composable
fun CameraFramingOverlay(
    modifier: Modifier = Modifier,
    strokeColor: Color = DefaultFramingOverlayColor,
    strokeWidth: Dp = 7.dp,
    bottomInset: Dp = 15.dp,
) {
    val context = LocalContext.current
    val assetSpec = remember(context) {
        runCatching { loadOverlaySpec(context) }.getOrNull()
    }

    Canvas(
        modifier = modifier.testTag(CameraFramingOverlayTag),
    ) {
        val mainStrokeWidth = strokeWidth.toPx()
        val bottomInsetPx = bottomInset.toPx()
        val overlaySpec = assetSpec
        if (overlaySpec != null) {
            drawAssetOverlay(
                spec = overlaySpec,
                strokeColor = strokeColor,
                mainStrokeWidth = mainStrokeWidth,
                bottomInset = bottomInsetPx,
                canvasSize = size,
            )
        } else {
            drawFallbackOverlay(
                strokeColor = strokeColor,
                mainStrokeWidth = mainStrokeWidth,
                canvasSize = size,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAssetOverlay(
    spec: OverlaySpec,
    strokeColor: Color,
    mainStrokeWidth: Float,
    bottomInset: Float,
    canvasSize: Size,
) {
    val visibleWidth = spec.visibleBounds.width
    val visibleHeight = spec.visibleBounds.height
    val scale = minOf(
        canvasSize.width / visibleWidth,
        canvasSize.height / visibleHeight,
    )
    val scaledWidth = visibleWidth * scale
    val offsetX = (canvasSize.width - scaledWidth) / 2f - spec.visibleBounds.left * scale - 18.dp.toPx()
    val offsetY = canvasSize.height - bottomInset - spec.visibleBounds.bottom * scale - 15.dp.toPx()

    withTransform({
        translate(left = offsetX, top = offsetY)
        scale(scaleX = scale, scaleY = scale)
    }) {
        val haloStrokeWidth = maxOf(mainStrokeWidth * 1.8f / scale, spec.pathStrokeWidth * 1.4f)
        val bodyStrokeWidth = maxOf(mainStrokeWidth / scale, spec.pathStrokeWidth)
        val haloHeadStrokeWidth = maxOf(mainStrokeWidth * 1.5f / scale, spec.headStrokeWidth * 1.6f)
        val headStrokeWidth = maxOf(mainStrokeWidth / scale, spec.headStrokeWidth)

        clipPath(spec.shouldersPath) {
            drawPath(
                path = spec.shouldersPath,
                color = strokeColor.copy(alpha = 0.24f),
                style = Stroke(
                    width = haloStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            drawPath(
                path = spec.shouldersPath,
                color = strokeColor,
                style = Stroke(
                    width = bodyStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        val headRect = Rect(
            left = spec.headCenter.x - spec.headRadius.width,
            top = spec.headCenter.y - spec.headRadius.height,
            right = spec.headCenter.x + spec.headRadius.width,
            bottom = spec.headCenter.y + spec.headRadius.height,
        )
        drawOval(
            color = strokeColor.copy(alpha = 0.24f),
            topLeft = headRect.topLeft,
            size = headRect.size,
            style = Stroke(width = haloHeadStrokeWidth),
        )
        drawOval(
            color = strokeColor,
            topLeft = headRect.topLeft,
            size = headRect.size,
            style = Stroke(width = headStrokeWidth),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallbackOverlay(
    strokeColor: Color,
    mainStrokeWidth: Float,
    canvasSize: Size,
) {
    val path = buildFallbackFramingPath(canvasSize)

    drawPath(
        path = path,
        color = strokeColor.copy(alpha = 0.24f),
        style = Stroke(
            width = mainStrokeWidth * 1.8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(
            width = mainStrokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private data class OverlaySpec(
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val shouldersPath: Path,
    val visibleBounds: Rect,
    val pathStrokeWidth: Float,
    val headCenter: Offset,
    val headRadius: Size,
    val headStrokeWidth: Float,
)

private fun loadOverlaySpec(context: Context): OverlaySpec {
    var viewBoxWidth: Float? = null
    var viewBoxHeight: Float? = null
    var shouldersPathData: String? = null
    var pathStrokeWidth: Float? = null
    var headCenterX: Float? = null
    var headCenterY: Float? = null
    var headRadiusX: Float? = null
    var headRadiusY: Float? = null
    var headStrokeWidth: Float? = null

    context.assets.open(CameraFramingOverlayAssetPath).bufferedReader().use { reader ->
        val parser = Xml.newPullParser().apply {
            setInput(reader)
        }

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "svg" -> {
                        val viewBox = parser.getAttributeValue(null, "viewBox")
                            ?: error("Missing SVG viewBox.")
                        val viewBoxParts = viewBox.trim().split(Regex("\\s+"))
                        require(viewBoxParts.size == 4) { "Unexpected SVG viewBox: $viewBox" }
                        viewBoxWidth = viewBoxParts[2].toFloat()
                        viewBoxHeight = viewBoxParts[3].toFloat()
                    }

                    "path" -> {
                        if (parser.getAttributeValue(null, "stroke-width") != null) {
                            shouldersPathData = parser.getAttributeValue(null, "d")
                            pathStrokeWidth = parser.getAttributeValue(null, "stroke-width")?.toFloat()
                        }
                    }

                    "ellipse" -> {
                        headCenterX = parser.getAttributeValue(null, "cx")?.toFloat()
                        headCenterY = parser.getAttributeValue(null, "cy")?.toFloat()
                        headRadiusX = parser.getAttributeValue(null, "rx")?.toFloat()
                        headRadiusY = parser.getAttributeValue(null, "ry")?.toFloat()
                        headStrokeWidth = parser.getAttributeValue(null, "stroke-width")?.toFloat()
                    }
                }
            }
            parser.next()
        }
    }

    val shouldersPathString = requireNotNull(shouldersPathData) { "Missing shoulder path data." }
    val platformPath = requireNotNull(PathParser.createPathFromPathData(shouldersPathString)) {
        "Unable to parse shoulder path."
    }

    val shouldersPath = platformPath.asComposePath()
    val headCenter = Offset(
        x = requireNotNull(headCenterX) { "Missing head center x." },
        y = requireNotNull(headCenterY) { "Missing head center y." },
    )
    val headRadius = Size(
        width = requireNotNull(headRadiusX) { "Missing head radius x." },
        height = requireNotNull(headRadiusY) { "Missing head radius y." },
    )
    val shouldersBounds = shouldersPath.getBounds()
    val headBounds = Rect(
        left = headCenter.x - headRadius.width,
        top = headCenter.y - headRadius.height,
        right = headCenter.x + headRadius.width,
        bottom = headCenter.y + headRadius.height,
    )
    val visibleBounds = Rect(
        left = minOf(shouldersBounds.left, headBounds.left),
        top = minOf(shouldersBounds.top, headBounds.top),
        right = maxOf(shouldersBounds.right, headBounds.right),
        bottom = maxOf(shouldersBounds.bottom, headBounds.bottom),
    )

    return OverlaySpec(
        viewBoxWidth = requireNotNull(viewBoxWidth) { "Missing SVG width." },
        viewBoxHeight = requireNotNull(viewBoxHeight) { "Missing SVG height." },
        shouldersPath = shouldersPath,
        visibleBounds = visibleBounds,
        pathStrokeWidth = requireNotNull(pathStrokeWidth) { "Missing shoulder stroke width." },
        headCenter = headCenter,
        headRadius = headRadius,
        headStrokeWidth = requireNotNull(headStrokeWidth) { "Missing head stroke width." },
    )
}

private fun buildFallbackFramingPath(size: Size): Path = Path().apply {
    fun x(value: Float) = size.width * value
    fun y(value: Float) = size.height * value

    moveTo(x(0.14f), y(0.82f))
    cubicTo(x(0.16f), y(0.76f), x(0.21f), y(0.71f), x(0.28f), y(0.67f))
    cubicTo(x(0.33f), y(0.64f), x(0.36f), y(0.60f), x(0.36f), y(0.53f))
    cubicTo(x(0.36f), y(0.45f), x(0.35f), y(0.34f), x(0.35f), y(0.22f))
    cubicTo(x(0.35f), y(0.13f), x(0.38f), y(0.07f), x(0.43f), y(0.03f))
    quadraticTo(x(0.47f), y(0.00f), x(0.50f), y(0.03f))
    quadraticTo(x(0.53f), y(0.00f), x(0.57f), y(0.03f))
    cubicTo(x(0.62f), y(0.07f), x(0.65f), y(0.13f), x(0.65f), y(0.22f))
    cubicTo(x(0.65f), y(0.34f), x(0.64f), y(0.45f), x(0.64f), y(0.53f))
    cubicTo(x(0.64f), y(0.60f), x(0.67f), y(0.64f), x(0.72f), y(0.67f))
    cubicTo(x(0.79f), y(0.71f), x(0.84f), y(0.76f), x(0.86f), y(0.82f))
}
