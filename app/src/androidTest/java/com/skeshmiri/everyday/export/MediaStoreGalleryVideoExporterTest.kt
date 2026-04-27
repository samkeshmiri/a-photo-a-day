package com.skeshmiri.everyday.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skeshmiri.everyday.model.DailyPhoto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class MediaStoreGalleryVideoExporterTest {
    private lateinit var context: Context
    private val createdImageUris = mutableListOf<Uri>()
    private val createdVideoUris = mutableListOf<Uri>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        (createdVideoUris + createdImageUris).forEach { uri ->
            context.contentResolver.delete(uri, null, null)
        }
    }

    @Test
    fun exportPreservesSingleFrameColorLayout() = runBlocking {
        val sourcePhoto = insertQuadrantPhoto()
        val exporter = MediaStoreGalleryVideoExporter(
            context = context,
            clock = Clock.fixed(Instant.parse("2026-03-27T08:45:12Z"), ZoneId.of("Europe/London")),
        )

        val exported = exporter.export(listOf(sourcePhoto), fps = 1)
        createdVideoUris += exported.uri

        val frame = decodeFirstFrame(exported.uri)
        try {
            assertMostlyRed(frame.getPixel(frame.width / 4, frame.height / 4), "top-left")
            assertMostlyGreen(frame.getPixel(frame.width * 3 / 4, frame.height / 4), "top-right")
            assertMostlyBlue(frame.getPixel(frame.width / 4, frame.height * 3 / 4), "bottom-left")
            assertMostlyWhite(frame.getPixel(frame.width * 3 / 4, frame.height * 3 / 4), "bottom-right")
        } finally {
            frame.recycle()
        }
    }

    private fun insertQuadrantPhoto(): DailyPhoto {
        val width = 320
        val height = 240
        val displayName = "2026-03-27_export_test_${System.nanoTime()}.jpg"
        val imageUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/EverydayTest/")
            put(MediaStore.Images.Media.DATE_TAKEN, Instant.parse("2026-03-27T08:45:12Z").toEpochMilli())
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(imageUri, values)
            ?: error("Failed to create a MediaStore image for video export testing.")
        createdImageUris += uri

        val bitmap = createQuadrantBitmap(width, height)
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            } ?: error("Failed to open the MediaStore image for writing.")
        } finally {
            bitmap.recycle()
        }

        context.contentResolver.update(
            uri,
            ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            },
            null,
            null,
        )

        return DailyPhoto(
            id = uri.lastPathSegment?.toLongOrNull() ?: 0L,
            uri = uri,
            displayName = displayName,
            dateKey = "2026-03-27",
            capturedAt = Instant.parse("2026-03-27T08:45:12Z"),
            width = width,
            height = height,
        )
    }

    private fun createQuadrantBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        canvas.drawColor(Color.BLACK)
        paint.color = Color.RED
        canvas.drawRect(0f, 0f, width / 2f, height / 2f, paint)
        paint.color = Color.GREEN
        canvas.drawRect(width / 2f, 0f, width.toFloat(), height / 2f, paint)
        paint.color = Color.BLUE
        canvas.drawRect(0f, height / 2f, width / 2f, height.toFloat(), paint)
        paint.color = Color.WHITE
        canvas.drawRect(width / 2f, height / 2f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    private fun decodeFirstFrame(uri: Uri): Bitmap {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: error("Failed to decode the exported video frame.")
        } finally {
            retriever.release()
        }
    }

    private fun assertMostlyRed(color: Int, label: String) {
        assertTrue("$label red channel was too low: ${Color.red(color)}", Color.red(color) > 150)
        assertTrue("$label green channel was too high: ${Color.green(color)}", Color.green(color) < 120)
        assertTrue("$label blue channel was too high: ${Color.blue(color)}", Color.blue(color) < 120)
    }

    private fun assertMostlyGreen(color: Int, label: String) {
        assertTrue("$label red channel was too high: ${Color.red(color)}", Color.red(color) < 120)
        assertTrue("$label green channel was too low: ${Color.green(color)}", Color.green(color) > 150)
        assertTrue("$label blue channel was too high: ${Color.blue(color)}", Color.blue(color) < 120)
    }

    private fun assertMostlyBlue(color: Int, label: String) {
        assertTrue("$label red channel was too high: ${Color.red(color)}", Color.red(color) < 120)
        assertTrue("$label green channel was too high: ${Color.green(color)}", Color.green(color) < 120)
        assertTrue("$label blue channel was too low: ${Color.blue(color)}", Color.blue(color) > 150)
    }

    private fun assertMostlyWhite(color: Int, label: String) {
        assertTrue("$label red channel was too low: ${Color.red(color)}", Color.red(color) > 180)
        assertTrue("$label green channel was too low: ${Color.green(color)}", Color.green(color) > 180)
        assertTrue("$label blue channel was too low: ${Color.blue(color)}", Color.blue(color) > 180)
    }
}
