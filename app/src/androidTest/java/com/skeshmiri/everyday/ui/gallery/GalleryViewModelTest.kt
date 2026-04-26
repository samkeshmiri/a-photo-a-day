package com.skeshmiri.everyday.ui.gallery

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skeshmiri.everyday.data.DailyPhotoRepository
import com.skeshmiri.everyday.export.ExportedGalleryVideo
import com.skeshmiri.everyday.export.GalleryVideoExporter
import com.skeshmiri.everyday.model.DailyPhoto
import com.skeshmiri.everyday.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GalleryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refreshCalculatesTheDefaultEstimatedDuration() = runTest {
        val repository = FakeDailyPhotoRepository(
            photos = List(36) { index ->
                photo(
                    id = index.toLong(),
                    capturedAt = "2026-03-${((index % 28) + 1).toString().padStart(2, '0')}T10:00:00Z",
                )
            },
        )
        val viewModel = GalleryViewModel(
            repository = repository,
            videoExporter = FakeGalleryVideoExporter(),
        )

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.selectedFps)
        assertEquals(7.2, viewModel.uiState.value.estimatedDurationSeconds, 0.0)
    }

    @Test
    fun selectFpsUpdatesTheEstimatedDuration() = runTest {
        val viewModel = GalleryViewModel(
            repository = FakeDailyPhotoRepository(
                photos = List(36) { index ->
                    photo(
                        id = index.toLong(),
                        capturedAt = "2026-03-${((index % 28) + 1).toString().padStart(2, '0')}T10:00:00Z",
                    )
                },
            ),
            videoExporter = FakeGalleryVideoExporter(),
        )

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.selectFps(3)

        assertEquals(3, viewModel.uiState.value.selectedFps)
        assertEquals(12.0, viewModel.uiState.value.estimatedDurationSeconds, 0.0)
    }

    @Test
    fun exportVideoSortsPhotosOldestToNewestAndStoresTheExportResult() = runTest {
        val exporter = FakeGalleryVideoExporter()
        val viewModel = GalleryViewModel(
            repository = FakeDailyPhotoRepository(
                photos = listOf(
                    photo(id = 3L, capturedAt = "2026-03-29T10:00:00Z"),
                    photo(id = 2L, capturedAt = "2026-03-28T10:00:00Z"),
                    photo(id = 1L, capturedAt = "2026-03-27T10:00:00Z"),
                ),
            ),
            videoExporter = exporter,
        )

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.selectFps(8)
        viewModel.exportVideo()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L), exporter.lastExportedPhotoIds)
        assertEquals(8, exporter.lastExportedFps)
        assertNotNull(viewModel.uiState.value.exportedVideo)
        assertNull(viewModel.uiState.value.exportErrorMessage)
    }

    @Test
    fun exportVideoSurfacesExporterFailures() = runTest {
        val viewModel = GalleryViewModel(
            repository = FakeDailyPhotoRepository(
                photos = listOf(photo(id = 1L, capturedAt = "2026-03-27T10:00:00Z")),
            ),
            videoExporter = FakeGalleryVideoExporter(
                failureMessage = "Encoder failed.",
            ),
        )

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.exportVideo()
        advanceUntilIdle()

        assertEquals("Encoder failed.", viewModel.uiState.value.exportErrorMessage)
        assertNull(viewModel.uiState.value.exportedVideo)
    }

    private class FakeDailyPhotoRepository(
        private val photos: List<DailyPhoto>,
    ) : DailyPhotoRepository {
        override suspend fun getToday(dateKey: String): DailyPhoto? = photos.firstOrNull { it.dateKey == dateKey }

        override suspend fun listAll(): List<DailyPhoto> = photos

        override suspend fun saveFromTemp(tempFile: File, dateKey: String): DailyPhoto {
            throw UnsupportedOperationException("Not needed for GalleryViewModel tests.")
        }
    }

    private class FakeGalleryVideoExporter(
        private val failureMessage: String? = null,
    ) : GalleryVideoExporter {
        var lastExportedPhotoIds: List<Long> = emptyList()
        var lastExportedFps: Int? = null

        override suspend fun export(photos: List<DailyPhoto>, fps: Int): ExportedGalleryVideo {
            failureMessage?.let { message ->
                throw IllegalStateException(message)
            }

            lastExportedPhotoIds = photos.map { it.id }
            lastExportedFps = fps
            return ExportedGalleryVideo(
                uri = Uri.parse("content://everyday/video/exported"),
                displayName = "Everyday_2026-03-27_to_2026-03-29_${fps}fps.mp4",
                relativePath = "Movies/Everyday/",
                fps = fps,
                frameCount = photos.size,
                durationSeconds = GalleryVideoExportDefaults.estimatedDurationSeconds(photos.size, fps),
            )
        }
    }

    private fun photo(
        id: Long,
        capturedAt: String,
    ) = DailyPhoto(
        id = id,
        uri = Uri.parse("content://everyday/photo/$id"),
        displayName = "2026-03-27_084500.jpg",
        dateKey = "2026-03-27",
        capturedAt = Instant.parse(capturedAt),
        width = 1200,
        height = 1600,
    )
}
