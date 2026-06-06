package com.skeshmiri.aphotoaday.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skeshmiri.aphotoaday.camera.CameraController
import com.skeshmiri.aphotoaday.ui.common.FourThreePortraitFrame
import com.skeshmiri.aphotoaday.ui.common.OnResume

const val CameraCaptureButtonTag = "camera_capture_button"
const val CameraFirstPhotoInstructionsTag = "camera_first_photo_instructions"
const val CameraFirstPhotoInstructionsContinueTag = "camera_first_photo_instructions_continue"

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    cameraController: CameraController,
    onOpenGallery: () -> Unit,
    onOpenReview: (dateKey: String, tempPath: String) -> Unit,
    showFramingOverlay: Boolean,
    guideSettings: CameraGuideSettings,
    onToggleFramingOverlay: () -> Unit,
    hasSeenFirstPhotoInstructions: Boolean,
    onFirstPhotoInstructionsCompleted: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    OnResume {
        viewModel.syncPermission(context)
        viewModel.refresh()
    }

    LaunchedEffect(uiState.hasCameraPermission, uiState.todayPhoto, previewView, lifecycleOwner) {
        val boundPreview = previewView
        if (uiState.hasCameraPermission && uiState.todayPhoto == null && boundPreview != null) {
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

    LaunchedEffect(uiState.todayPhoto) {
        if (uiState.todayPhoto != null) {
            onOpenGallery()
        }
    }

    CameraScreenContent(
        uiState = uiState,
        showFramingOverlay = showFramingOverlay,
        guideSettings = guideSettings,
        hasSeenFirstPhotoInstructions = hasSeenFirstPhotoInstructions,
        onCapture = {
            viewModel.capture(cameraController) { dateKey, tempFile ->
                onOpenReview(dateKey, tempFile.absolutePath)
            }
        },
        onToggleFramingOverlay = onToggleFramingOverlay,
        onFirstPhotoInstructionsCompleted = onFirstPhotoInstructionsCompleted,
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
fun CameraScreenContent(
    uiState: CameraUiState,
    showFramingOverlay: Boolean = true,
    guideSettings: CameraGuideSettings = CameraGuideSettings(),
    hasSeenFirstPhotoInstructions: Boolean = true,
    onCapture: () -> Unit,
    onToggleFramingOverlay: () -> Unit = {},
    onFirstPhotoInstructionsCompleted: () -> Unit = {},
    preview: @Composable () -> Unit,
) {
    val showFirstPhotoInstructions = !hasSeenFirstPhotoInstructions &&
        uiState.hasCameraPermission &&
        !uiState.isLoading &&
        uiState.todayPhoto == null &&
        !uiState.hasAnySavedPhotos &&
        uiState.errorMessage == null
    var firstPhotoInstructionStep by remember(showFirstPhotoInstructions) {
        mutableStateOf(FirstPhotoInstructionStep.AlignEyes)
    }

    Scaffold(modifier = Modifier.systemBarsPadding()) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            !uiState.hasCameraPermission -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Camera access is needed to take your daily photo.",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = uiState.errorMessage ?: "Enable camera access in Android Settings, then return to the app.",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            uiState.todayPhoto != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.Black),
                ) {
                    val previewSidePadding = 12.dp
                    val previewTopPadding = 16.dp
                    val previewBottomPadding = 6.dp
                    val minBottomSectionHeight = 180.dp
                    val previewWidth = maxWidth - (previewSidePadding * 2)
                    val desiredFrameHeight = (maxWidth - (previewSidePadding * 2)) * (4f / 3f)
                    val maxFrameHeight = (maxHeight - minBottomSectionHeight - previewTopPadding - previewBottomPadding)
                        .coerceAtLeast(220.dp)
                    val frameHeight = desiredFrameHeight.coerceAtMost(maxFrameHeight)
                    val topSectionHeight = frameHeight + previewTopPadding + previewBottomPadding
                    val bottomSectionHeight = (maxHeight - topSectionHeight).coerceAtLeast(160.dp)
                    val captureButtonWidth = previewWidth
                    val captureButtonHeight = (bottomSectionHeight - 36.dp)
                        .coerceAtMost(220.dp)
                        .coerceAtLeast(120.dp)

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
                                if (showFramingOverlay || showFirstPhotoInstructions) {
                                    CameraFramingOverlay(
                                        modifier = Modifier.fillMaxSize(),
                                        guideSettings = guideSettings,
                                    )
                                }
                                if (showFirstPhotoInstructions) {
                                    FirstPhotoInstructionsOverlay(
                                        step = firstPhotoInstructionStep,
                                        onContinue = {
                                            if (firstPhotoInstructionStep == FirstPhotoInstructionStep.AlignEyes) {
                                                firstPhotoInstructionStep = FirstPhotoInstructionStep.ToggleGrid
                                            } else {
                                                onFirstPhotoInstructionsCompleted()
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }

                            if (uiState.errorMessage != null) {
                                ElevatedCard(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 12.dp),
                                ) {
                                    Text(
                                        text = uiState.errorMessage,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = bottomSectionHeight)
                                .padding(
                                    start = previewSidePadding,
                                    top = 20.dp,
                                    end = previewSidePadding,
                                    bottom = 16.dp,
                                ),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            val captureButtonShape = RoundedCornerShape(24.dp)
                            val captureButtonEnabled = !uiState.isCapturing && !showFirstPhotoInstructions
                            val captureButtonContentDescription = when {
                                uiState.isCapturing -> "Taking photo"
                                showFirstPhotoInstructions -> "Finish camera instructions"
                                else -> "Take photo"
                            }
                            val captureButtonContainerColor = if (captureButtonEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            }
                            val captureButtonContentColor = if (captureButtonEnabled) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            }

                            Box(
                                modifier = Modifier
                                    .width(captureButtonWidth)
                                    .height(captureButtonHeight)
                                    .clip(captureButtonShape)
                                    .background(captureButtonContainerColor)
                                    .captureButtonInteractions(
                                        enabled = captureButtonEnabled,
                                        onCapture = onCapture,
                                        onToggleFramingOverlay = onToggleFramingOverlay,
                                        showFramingOverlay = showFramingOverlay,
                                        captureContentDescription = captureButtonContentDescription,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (uiState.isCapturing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = captureButtonContentColor,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(captureButtonHeight * 0.3f),
                                        tint = captureButtonContentColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstPhotoInstructionsOverlay(
    step: FirstPhotoInstructionStep,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.testTag(CameraFirstPhotoInstructionsTag),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.68f))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Line your eyes up with the grid cross-sections.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Start,
            )
            if (step == FirstPhotoInstructionStep.ToggleGrid) {
                Text(
                    text = "Press and hold the camera button to turn the grid lines on or off.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                )
            }
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CameraFirstPhotoInstructionsContinueTag),
            ) {
                Text("Continue")
            }
        }
    }
}

private enum class FirstPhotoInstructionStep {
    AlignEyes,
    ToggleGrid,
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.captureButtonInteractions(
    enabled: Boolean,
    onCapture: () -> Unit,
    onToggleFramingOverlay: () -> Unit,
    showFramingOverlay: Boolean,
    captureContentDescription: String,
): Modifier = semantics {
    contentDescription = captureContentDescription
}
    .testTag(CameraCaptureButtonTag)
    .combinedClickable(
        enabled = enabled,
        role = Role.Button,
        onClickLabel = "Take photo",
        onLongClickLabel = if (showFramingOverlay) "Hide camera guides" else "Show camera guides",
        onClick = onCapture,
        onLongClick = onToggleFramingOverlay,
    )
