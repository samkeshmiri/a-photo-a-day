package com.skeshmiri.aphotoaday.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeshmiri.aphotoaday.data.DailyPhotoRepository
import com.skeshmiri.aphotoaday.model.DailyPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoViewerUiState(
    val isLoading: Boolean = true,
    val photos: List<DailyPhoto> = emptyList(),
    val errorMessage: String? = null,
)

class PhotoViewerViewModel(
    private val repository: DailyPhotoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoViewerUiState())
    val uiState: StateFlow<PhotoViewerUiState> = _uiState.asStateFlow()

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
                    )
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
}
