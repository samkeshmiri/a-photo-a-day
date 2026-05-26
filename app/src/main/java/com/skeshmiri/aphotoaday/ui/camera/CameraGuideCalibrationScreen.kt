package com.skeshmiri.aphotoaday.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skeshmiri.aphotoaday.camera.CameraController
import com.skeshmiri.aphotoaday.ui.common.FourThreePortraitFrame
import com.skeshmiri.aphotoaday.ui.common.OnResume

const val CameraGuideVerticalSliderTag = "camera_guide_vertical_slider"
const val CameraGuideHorizontalSliderTag = "camera_guide_horizontal_slider"
const val CameraGuideSaveButtonTag = "camera_guide_save_button"
const val CameraGuideResetButtonTag = "camera_guide_reset_button"

@Composable
fun CameraGuideCalibrationScreen(
    cameraController: CameraController,
    guideSettings: CameraGuideSettings,
    onSaveGuideSettings: (CameraGuideSettings) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val savedGuideSettings = guideSettings.normalized()
    var draftGuideSettings by remember(savedGuideSettings) {
        mutableStateOf(savedGuideSettings)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted },
    )

    OnResume {
        hasCameraPermission = context.hasCameraPermission()
    }

    LaunchedEffect(hasCameraPermission, previewView, lifecycleOwner) {
        val boundPreview = previewView
        if (hasCameraPermission && boundPreview != null) {
            runCatching { cameraController.bind(lifecycleOwner, boundPreview) }
        } else {
            cameraController.unbind()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.unbind()
        }
    }

    CameraGuideCalibrationScreenContent(
        hasCameraPermission = hasCameraPermission,
        guideSettings = draftGuideSettings,
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onVerticalGuideProgressChange = { progress ->
            draftGuideSettings = draftGuideSettings.copy(verticalGuideProgress = progress).normalized()
        },
        onHorizontalGuideProgressChange = { progress ->
            draftGuideSettings = draftGuideSettings.copy(horizontalGuideProgress = progress).normalized()
        },
        hasUnsavedChanges = draftGuideSettings != savedGuideSettings,
        onSaveGuideSettings = { onSaveGuideSettings(draftGuideSettings) },
        onResetGuideSettings = { draftGuideSettings = savedGuideSettings },
        onClose = onClose,
        preview = {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewContext ->
                    PreviewView(previewContext).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = this
                    }
                },
                update = { previewView = it },
            )
        },
    )
}

@Composable
fun CameraGuideCalibrationScreenContent(
    hasCameraPermission: Boolean,
    guideSettings: CameraGuideSettings,
    onRequestPermission: () -> Unit,
    onVerticalGuideProgressChange: (Float) -> Unit,
    onHorizontalGuideProgressChange: (Float) -> Unit,
    hasUnsavedChanges: Boolean,
    onSaveGuideSettings: () -> Unit,
    onResetGuideSettings: () -> Unit,
    onClose: () -> Unit,
    preview: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.systemBarsPadding(),
    ) { innerPadding ->
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Camera access is needed to calibrate the eye guides.",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "The preview stays on this device.",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Text("Allow camera")
                }
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Black),
            ) {
                val previewSidePadding = 12.dp
                val previewTopPadding = 16.dp
                val previewBottomPadding = 6.dp
                val minBottomSectionHeight = 270.dp
                val previewWidth = maxWidth - (previewSidePadding * 2)
                val desiredFrameHeight = previewWidth * (4f / 3f)
                val maxFrameHeight = (maxHeight - minBottomSectionHeight - previewTopPadding - previewBottomPadding)
                    .coerceAtLeast(220.dp)
                val frameHeight = desiredFrameHeight.coerceAtMost(maxFrameHeight)
                val topSectionHeight = frameHeight + previewTopPadding + previewBottomPadding
                val bottomSectionHeight = (maxHeight - topSectionHeight).coerceAtLeast(minBottomSectionHeight)

                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topSectionHeight)
                            .padding(
                                start = previewSidePadding,
                                top = previewTopPadding,
                                end = previewSidePadding,
                                bottom = previewBottomPadding,
                            ),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        FourThreePortraitFrame(
                            modifier = Modifier.height(frameHeight),
                        ) {
                            preview()
                            CameraFramingOverlay(
                                modifier = Modifier.fillMaxSize(),
                                guideSettings = guideSettings,
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close guide settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = bottomSectionHeight)
                            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        GuideSlider(
                            label = "Eye width",
                            value = guideSettings.normalized().verticalGuideProgress,
                            onValueChange = onVerticalGuideProgressChange,
                            contentDescription = "Eye width guide",
                            modifier = Modifier.testTag(CameraGuideVerticalSliderTag),
                        )
                        GuideSlider(
                            label = "Eye height",
                            value = guideSettings.normalized().horizontalGuideProgress,
                            onValueChange = onHorizontalGuideProgressChange,
                            contentDescription = "Eye height guide",
                            modifier = Modifier.testTag(CameraGuideHorizontalSliderTag),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Button(
                                onClick = onSaveGuideSettings,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .testTag(CameraGuideSaveButtonTag),
                                enabled = hasUnsavedChanges,
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = "Save guide position",
                                )
                            }

                            OutlinedButton(
                                onClick = onResetGuideSettings,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .testTag(CameraGuideResetButtonTag),
                                enabled = hasUnsavedChanges,
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay,
                                    contentDescription = "Reset guide position",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = CameraGuideSettings.MIN_PROGRESS..CameraGuideSettings.MAX_PROGRESS,
            modifier = modifier
                .fillMaxWidth()
                .semantics {
                    this.contentDescription = contentDescription
                },
        )
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
