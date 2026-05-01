package com.skeshmiri.everyday.ui.camera

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skeshmiri.everyday.camera.CameraController
import com.skeshmiri.everyday.ui.common.FourThreePortraitFrame
import com.skeshmiri.everyday.ui.common.OnResume

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    cameraController: CameraController,
    onOpenGallery: () -> Unit,
    onOpenReview: (dateKey: String, tempPath: String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onPermissionResult,
    )

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
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onCapture = {
            viewModel.capture(cameraController) { dateKey, tempFile ->
                onOpenReview(dateKey, tempFile.absolutePath)
            }
        },
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
    onRequestPermission: () -> Unit,
    onCapture: () -> Unit,
    preview: @Composable () -> Unit,
) {
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
                        text = "The camera stays local to this app. Grant access to take your daily selfie.",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = uiState.errorMessage ?: "No network, no account, just one photo a day.",
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
                                CameraFramingOverlay(
                                    modifier = Modifier.fillMaxSize(),
                                )
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
                            Button(
                                onClick = onCapture,
                                modifier = Modifier
                                    .width(captureButtonWidth)
                                    .height(captureButtonHeight),
                                enabled = !uiState.isCapturing,
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                if (uiState.isCapturing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = "Take photo",
                                        modifier = Modifier.size(captureButtonHeight * 0.3f),
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
