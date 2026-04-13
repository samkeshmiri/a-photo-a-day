package com.skeshmiri.everyday.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeshmiri.everyday.data.DailyPhotoRepository
import com.skeshmiri.everyday.export.ExportedGalleryVideo
import com.skeshmiri.everyday.export.GalleryVideoExporter
import com.skeshmiri.everyday.model.DailyPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val isLoading: Boolean = true,
    val photos: List<DailyPhoto> = emptyList(),
    val errorMessage: String? = null,
    val selectedFps: Int = GalleryVideoExportDefaults.defaultFps,
    val estimatedDurationSeconds: Double = 0.0,
    val isExporting: Boolean = false,
    val exportErrorMessage: String? = null,
    val exportedVideo: ExportedGalleryVideo? = null,
)

class GalleryViewModel(
    private val repository: DailyPhotoRepository,
    private val videoExporter: GalleryVideoExporter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.listAll()
            }.onSuccess { photos ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        photos = photos,
                    ).withEstimatedDuration()
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load photos.",
                    )
                }
            }
        }
    }

    fun selectFps(fps: Int) {
        if (fps !in GalleryVideoExportDefaults.fpsPresets) {
            return
        }

        _uiState.update {
            it.copy(
                selectedFps = fps,
                exportErrorMessage = null,
                exportedVideo = null,
            ).withEstimatedDuration()
        }
    }

    fun clearExportFeedback() {
        _uiState.update {
            it.copy(
                exportErrorMessage = null,
                exportedVideo = null,
            )
        }
    }

    fun exportVideo() {
        val snapshot = _uiState.value
        if (snapshot.isExporting) {
            return
        }

        val orderedPhotos = GalleryVideoExportDefaults.sortPhotosForExport(snapshot.photos)
        if (orderedPhotos.isEmpty()) {
            _uiState.update {
                it.copy(exportErrorMessage = "Add at least one photo to export a video.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    exportErrorMessage = null,
                    exportedVideo = null,
                )
            }

            runCatching {
                videoExporter.export(
                    photos = orderedPhotos,
                    fps = snapshot.selectedFps,
                )
            }.onSuccess { exportedVideo ->
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportedVideo = exportedVideo,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportErrorMessage = error.message ?: "Failed to export the video.",
                    )
                }
            }
        }
    }
}

private fun GalleryUiState.withEstimatedDuration(): GalleryUiState =
    copy(
        estimatedDurationSeconds = GalleryVideoExportDefaults.estimatedDurationSeconds(
            photoCount = photos.size,
            fps = selectedFps,
        ),
    )
