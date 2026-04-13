package com.skeshmiri.everyday.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.provider.MediaStore
import com.skeshmiri.everyday.model.DailyPhoto
import com.skeshmiri.everyday.ui.gallery.GalleryVideoExportDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.io.IOException
import java.time.Clock
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MediaStoreGalleryVideoExporter(
    context: Context,
    private val clock: Clock,
) : GalleryVideoExporter {
    private val contentResolver = context.contentResolver
    private val collectionUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val relativePath = "${Environment.DIRECTORY_MOVIES}/Everyday/"

    override suspend fun export(photos: List<DailyPhoto>, fps: Int): ExportedGalleryVideo = withContext(Dispatchers.IO) {
        require(photos.isNotEmpty()) { "At least one photo is required to export a video." }
        require(fps > 0) { "Frames per second must be greater than zero." }

        val orderedPhotos = GalleryVideoExportDefaults.sortPhotosForExport(photos)
        val displayName = buildDisplayName(orderedPhotos, fps)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, MIME_TYPE_MP4)
            put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Video.Media.DATE_TAKEN, clock.instant().toEpochMilli())
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val outputUri = contentResolver.insert(collectionUri, values)
            ?: throw IOException("Failed to create a MediaStore record for the exported video.")

        try {
            contentResolver.openFileDescriptor(outputUri, "w")?.use { descriptor ->
                encodeVideo(
                    outputFd = descriptor.fileDescriptor,
                    photos = orderedPhotos,
                    fps = fps,
                )
            } ?: throw IOException("Failed to open an output file for the exported video.")

            contentResolver.update(
                outputUri,
                ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                },
                null,
                null,
            )

            ExportedGalleryVideo(
                uri = outputUri,
                displayName = displayName,
                relativePath = relativePath,
                fps = fps,
                frameCount = orderedPhotos.size,
                durationSeconds = GalleryVideoExportDefaults.estimatedDurationSeconds(orderedPhotos.size, fps),
            )
        } catch (error: Throwable) {
            contentResolver.delete(outputUri, null, null)
            throw error
        }
    }

    private fun encodeVideo(
        outputFd: FileDescriptor,
        photos: List<DailyPhoto>,
        fps: Int,
    ) {
        val frameSize = determineFrameSize(photos.first())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        val frameBitmap = Bitmap.createBitmap(frameSize.width, frameSize.height, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)
        val argbPixels = IntArray(frameSize.width * frameSize.height)
        val yuvBytes = ByteArray(frameSize.frameByteCount)
        val bufferInfo = BufferInfo()
        val frameIntervalUs = 1_000_000L / fps.toLong()
        val bitrate = calculateBitrate(frameSize.width, frameSize.height)

        val codec = MediaCodec.createEncoderByType(MIME_TYPE_AVC)
        val muxer = MediaMuxer(outputFd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxerState = MuxerState()
        var firstBitmap: Bitmap? = null

        try {
            val colorFormat = selectColorFormat(codec.codecInfo)
            val videoFormat = MediaFormat.createVideoFormat(MIME_TYPE_AVC, frameSize.width, frameSize.height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat.codecConstant)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            codec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            photos.forEachIndexed { index, photo ->
                val sourceBitmap = if (index == 0) {
                    decodeBitmap(
                        uri = photo.uri,
                        requestedWidth = frameSize.width,
                        requestedHeight = frameSize.height,
                    ).also { firstBitmap = it }
                } else {
                    decodeBitmap(
                        uri = photo.uri,
                        requestedWidth = frameSize.width,
                        requestedHeight = frameSize.height,
                    )
                }

                renderFrame(
                    source = sourceBitmap,
                    target = frameBitmap,
                    canvas = frameCanvas,
                    paint = paint,
                )
                writeBitmapToYuv(
                    bitmap = frameBitmap,
                    colorFormat = colorFormat,
                    argbPixels = argbPixels,
                    output = yuvBytes,
                )
                queueFrame(
                    codec = codec,
                    frameData = yuvBytes,
                    presentationTimeUs = frameIntervalUs * index.toLong(),
                    bufferInfo = bufferInfo,
                    muxer = muxer,
                    muxerState = muxerState,
                )
                drainEncoder(
                    codec = codec,
                    bufferInfo = bufferInfo,
                    muxer = muxer,
                    muxerState = muxerState,
                    endOfStream = false,
                )

                if (sourceBitmap !== firstBitmap) {
                    sourceBitmap.recycle()
                }
            }

            firstBitmap?.recycle()
            queueEndOfStream(
                codec = codec,
                presentationTimeUs = frameIntervalUs * photos.size.toLong(),
                bufferInfo = bufferInfo,
                muxer = muxer,
                muxerState = muxerState,
            )
            drainEncoder(
                codec = codec,
                bufferInfo = bufferInfo,
                muxer = muxer,
                muxerState = muxerState,
                endOfStream = true,
            )
        } finally {
            frameBitmap.recycle()
            codec.stopSafely()
            codec.release()
            muxer.stopSafely(muxerState.started)
            muxer.release()
        }
    }

    private fun determineFrameSize(photo: DailyPhoto): VideoFrameSize {
        val sourceWidth = photo.width
        val sourceHeight = photo.height
        if (sourceWidth > 0 && sourceHeight > 0) {
            return VideoFrameSize.from(sourceWidth, sourceHeight)
        }

        val probeBitmap = decodeBitmap(
            uri = photo.uri,
            requestedWidth = MAX_VIDEO_DIMENSION,
            requestedHeight = MAX_VIDEO_DIMENSION,
        )
        return try {
            VideoFrameSize.from(probeBitmap.width, probeBitmap.height)
        } finally {
            probeBitmap.recycle()
        }
    }

    private fun decodeBitmap(
        uri: android.net.Uri,
        requestedWidth: Int,
        requestedHeight: Int,
    ): Bitmap {
        val source = ImageDecoder.createSource(contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val widthSample = max(1, info.size.width / requestedWidth)
            val heightSample = max(1, info.size.height / requestedHeight)
            val sampleSize = max(1, max(widthSample, heightSample))
            if (sampleSize > 1) {
                decoder.setTargetSampleSize(sampleSize)
            }
        }
    }

    private fun renderFrame(
        source: Bitmap,
        target: Bitmap,
        canvas: Canvas,
        paint: Paint,
    ) {
        canvas.drawColor(Color.BLACK)

        val scale = min(
            target.width.toFloat() / source.width.toFloat(),
            target.height.toFloat() / source.height.toFloat(),
        )
        val destWidth = max(2, (source.width * scale).roundToInt())
        val destHeight = max(2, (source.height * scale).roundToInt())
        val left = (target.width - destWidth) / 2
        val top = (target.height - destHeight) / 2
        val destination = Rect(
            left,
            top,
            left + destWidth,
            top + destHeight,
        )
        canvas.drawBitmap(source, null, destination, paint)
    }

    private fun queueFrame(
        codec: MediaCodec,
        frameData: ByteArray,
        presentationTimeUs: Long,
        bufferInfo: BufferInfo,
        muxer: MediaMuxer,
        muxerState: MuxerState,
    ) {
        while (true) {
            val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
            if (inputIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputIndex)
                    ?: throw IOException("Failed to access the video encoder input buffer.")
                inputBuffer.clear()
                if (inputBuffer.capacity() < frameData.size) {
                    throw IOException("The video encoder input buffer is smaller than the frame data.")
                }
                inputBuffer.put(frameData)
                codec.queueInputBuffer(inputIndex, 0, frameData.size, presentationTimeUs, 0)
                return
            }

            drainEncoder(
                codec = codec,
                bufferInfo = bufferInfo,
                muxer = muxer,
                muxerState = muxerState,
                endOfStream = false,
            )
        }
    }

    private fun queueEndOfStream(
        codec: MediaCodec,
        presentationTimeUs: Long,
        bufferInfo: BufferInfo,
        muxer: MediaMuxer,
        muxerState: MuxerState,
    ) {
        while (true) {
            val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
            if (inputIndex >= 0) {
                codec.queueInputBuffer(
                    inputIndex,
                    0,
                    0,
                    presentationTimeUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                )
                return
            }

            drainEncoder(
                codec = codec,
                bufferInfo = bufferInfo,
                muxer = muxer,
                muxerState = muxerState,
                endOfStream = false,
            )
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        bufferInfo: BufferInfo,
        muxer: MediaMuxer,
        muxerState: MuxerState,
        endOfStream: Boolean,
    ) {
        while (true) {
            when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) {
                        return
                    }
                }

                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (muxerState.started) {
                        throw IOException("The video encoder changed output format more than once.")
                    }
                    muxerState.trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerState.started = true
                }

                else -> {
                    if (outputIndex < 0) {
                        continue
                    }

                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                        ?: throw IOException("Failed to access the video encoder output buffer.")
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size > 0) {
                        if (!muxerState.started || muxerState.trackIndex < 0) {
                            throw IOException("The video muxer was not ready before encoded data arrived.")
                        }

                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(muxerState.trackIndex, outputBuffer, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        return
                    }
                }
            }
        }
    }

    private fun writeBitmapToYuv(
        bitmap: Bitmap,
        colorFormat: EncoderColorFormat,
        argbPixels: IntArray,
        output: ByteArray,
    ) {
        val width = bitmap.width
        val height = bitmap.height
        val frameSize = width * height
        bitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)

        var uIndex = frameSize
        var vIndex = frameSize + (frameSize / 4)
        var uvIndex = frameSize

        for (row in 0 until height step 2) {
            for (column in 0 until width step 2) {
                var redTotal = 0
                var greenTotal = 0
                var blueTotal = 0
                var sampleCount = 0

                for (rowOffset in 0..1) {
                    for (columnOffset in 0..1) {
                        val x = column + columnOffset
                        val y = row + rowOffset
                        val pixel = argbPixels[(y * width) + x]
                        val red = Color.red(pixel)
                        val green = Color.green(pixel)
                        val blue = Color.blue(pixel)

                        output[(y * width) + x] = rgbToLuma(red, green, blue).toByte()
                        redTotal += red
                        greenTotal += green
                        blueTotal += blue
                        sampleCount += 1
                    }
                }

                val redAverage = redTotal / sampleCount
                val greenAverage = greenTotal / sampleCount
                val blueAverage = blueTotal / sampleCount
                val chromaBlue = rgbToChromaBlue(redAverage, greenAverage, blueAverage).toByte()
                val chromaRed = rgbToChromaRed(redAverage, greenAverage, blueAverage).toByte()

                when (colorFormat.layout) {
                    ChromaLayout.I420 -> {
                        output[uIndex++] = chromaBlue
                        output[vIndex++] = chromaRed
                    }

                    ChromaLayout.NV12 -> {
                        output[uvIndex++] = chromaBlue
                        output[uvIndex++] = chromaRed
                    }
                }
            }
        }
    }

    private fun selectColorFormat(codecInfo: MediaCodecInfo): EncoderColorFormat {
        val colorFormats = codecInfo.getCapabilitiesForType(MIME_TYPE_AVC).colorFormats.toSet()
        return when {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible in colorFormats -> {
                EncoderColorFormat(
                    codecConstant = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,
                    layout = ChromaLayout.I420,
                )
            }

            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar in colorFormats -> {
                EncoderColorFormat(
                    codecConstant = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
                    layout = ChromaLayout.I420,
                )
            }

            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar in colorFormats -> {
                EncoderColorFormat(
                    codecConstant = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                    layout = ChromaLayout.NV12,
                )
            }

            else -> throw IOException("No supported YUV420 encoder color format was found for MP4 export.")
        }
    }

    private fun calculateBitrate(width: Int, height: Int): Int =
        (width.toLong() * height.toLong() * 5L)
            .coerceIn(2_000_000L, 12_000_000L)
            .toInt()

    private fun buildDisplayName(
        photos: List<DailyPhoto>,
        fps: Int,
    ): String {
        val startDate = photos.first().dateKey
        val endDate = photos.last().dateKey
        val range = if (startDate == endDate) startDate else "${startDate}_to_$endDate"
        return "Everyday_${range}_${fps}fps.mp4"
    }

    private fun rgbToLuma(red: Int, green: Int, blue: Int): Int =
        ((66 * red + 129 * green + 25 * blue + 128) shr 8)
            .plus(16)
            .coerceIn(0, 255)

    private fun rgbToChromaBlue(red: Int, green: Int, blue: Int): Int =
        ((-38 * red - 74 * green + 112 * blue + 128) shr 8)
            .plus(128)
            .coerceIn(0, 255)

    private fun rgbToChromaRed(red: Int, green: Int, blue: Int): Int =
        ((112 * red - 94 * green - 18 * blue + 128) shr 8)
            .plus(128)
            .coerceIn(0, 255)

    private enum class ChromaLayout {
        I420,
        NV12,
    }

    private data class EncoderColorFormat(
        val codecConstant: Int,
        val layout: ChromaLayout,
    )

    private data class MuxerState(
        var started: Boolean = false,
        var trackIndex: Int = -1,
    )

    private data class VideoFrameSize(
        val width: Int,
        val height: Int,
    ) {
        val frameByteCount: Int = width * height * 3 / 2

        companion object {
            fun from(sourceWidth: Int, sourceHeight: Int): VideoFrameSize {
                val longestSide = max(sourceWidth, sourceHeight).coerceAtLeast(1)
                val scale = min(1f, MAX_VIDEO_DIMENSION.toFloat() / longestSide.toFloat())
                val scaledWidth = makeEven((sourceWidth * scale).roundToInt().coerceAtLeast(2))
                val scaledHeight = makeEven((sourceHeight * scale).roundToInt().coerceAtLeast(2))
                return VideoFrameSize(
                    width = scaledWidth,
                    height = scaledHeight,
                )
            }

            private fun makeEven(value: Int): Int = if (value % 2 == 0) value else value - 1
        }
    }

    private fun MediaCodec.stopSafely() {
        runCatching { stop() }
    }

    private fun MediaMuxer.stopSafely(started: Boolean) {
        if (!started) return
        runCatching { stop() }
    }

    companion object {
        private const val MIME_TYPE_AVC = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val MIME_TYPE_MP4 = "video/mp4"
        private const val MAX_VIDEO_DIMENSION = 1080
        private const val CODEC_TIMEOUT_US = 10_000L
    }
}
