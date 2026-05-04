package com.skeshmiri.aphotoaday.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skeshmiri.aphotoaday.ui.common.FourThreePortraitFrame
import com.skeshmiri.aphotoaday.ui.common.UriImage

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onSaved: () -> Unit,
    onRetake: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = { viewModel.discard(onRetake) })

    Scaffold(
        modifier = Modifier.background(Color.Black),
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val previewSidePadding = 12.dp
            val previewTopPadding = 16.dp
            val previewBottomPadding = 6.dp
            val minBottomSectionHeight = 180.dp
            val previewWidth = maxWidth - (previewSidePadding * 2)
            val desiredFrameHeight = previewWidth * (4f / 3f)
            val maxFrameHeight = (maxHeight - minBottomSectionHeight - previewTopPadding - previewBottomPadding)
                .coerceAtLeast(220.dp)
            val frameHeight = desiredFrameHeight.coerceAtMost(maxFrameHeight)
            val topSectionHeight = frameHeight + previewTopPadding + previewBottomPadding
            val bottomSectionHeight = (maxHeight - topSectionHeight).coerceAtLeast(160.dp)
            val actionButtonHeight = ((bottomSectionHeight - 56.dp) / 2f)
                .coerceAtLeast(72.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                val tempUri = viewModel.tempUri
                if (tempUri != null) {
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
                            UriImage(
                                uri = tempUri,
                                contentDescription = "Review captured photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "No photo to review.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomSectionHeight)
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    uiState.errorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Button(
                            onClick = { viewModel.save { onSaved() } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(actionButtonHeight),
                            enabled = uiState.tempFile != null && !uiState.isSaving,
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = "Save photo",
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.discard(onRetake) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(actionButtonHeight),
                            enabled = !uiState.isSaving,
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay,
                                contentDescription = "Retake photo",
                            )
                        }
                    }
                }
            }
        }
    }
}
