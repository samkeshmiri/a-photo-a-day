package com.skeshmiri.aphotoaday.ui.camera

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skeshmiri.aphotoaday.data.DailyPhotoRepository
import com.skeshmiri.aphotoaday.domain.DailyCapturePolicy
import com.skeshmiri.aphotoaday.model.DailyPhoto
import com.skeshmiri.aphotoaday.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CameraViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refreshReportsNoSavedPhotosWhenRepositoryIsEmpty() = runTest {
        val viewModel = CameraViewModel(
            dailyPhotoRepository = FakeDailyPhotoRepository(photos = emptyList()),
            dailyCapturePolicy = DailyCapturePolicy { true },
            clock = TestClock,
        )

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.hasAnySavedPhotos)
    }

    @Test
    fun refreshReportsSavedPhotosWhenRepositoryContainsAnyPhoto() = runTest {
        val viewModel = CameraViewModel(
            dailyPhotoRepository = FakeDailyPhotoRepository(
                photos = listOf(
                    photo(id = 1L),
                ),
            ),
            dailyCapturePolicy = DailyCapturePolicy { true },
            clock = TestClock,
        )

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.hasAnySavedPhotos)
    }

    private class FakeDailyPhotoRepository(
        private val photos: List<DailyPhoto>,
    ) : DailyPhotoRepository {
        override suspend fun getToday(dateKey: String): DailyPhoto? = photos.firstOrNull { it.dateKey == dateKey }

        override suspend fun listAll(): List<DailyPhoto> = photos

        override suspend fun saveFromTemp(tempFile: File, dateKey: String): DailyPhoto {
            throw UnsupportedOperationException("Not needed for CameraViewModel tests.")
        }
    }

    private companion object {
        val TestClock: Clock = Clock.fixed(
            Instant.parse("2026-03-27T08:45:00Z"),
            ZoneOffset.UTC,
        )

        fun photo(id: Long) = DailyPhoto(
            id = id,
            uri = Uri.parse("content://everyday/photo/$id"),
            displayName = "2026-03-27_084500.jpg",
            dateKey = "2026-03-27",
            capturedAt = Instant.parse("2026-03-27T08:45:00Z"),
            width = 1200,
            height = 1600,
        )
    }
}
