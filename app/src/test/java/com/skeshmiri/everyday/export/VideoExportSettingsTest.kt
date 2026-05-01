package com.skeshmiri.everyday.export

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoExportSettingsTest {
    @Test
    fun pickTargetFrameSizeUsesLargestSourceResolution() {
        val frameSize = VideoExportSettings.pickTargetFrameSize(
            listOf(
                VideoExportSettings.SourceImageSize(width = 1536, height = 2048),
                VideoExportSettings.SourceImageSize(width = 3024, height = 4032),
                VideoExportSettings.SourceImageSize(width = 1944, height = 2592),
            ),
        )

        assertEquals(3024, frameSize.width)
        assertEquals(4032, frameSize.height)
    }

    @Test
    fun pickTargetFrameSizeRoundsOddDimensionsDownToEvenValues() {
        val frameSize = VideoExportSettings.pickTargetFrameSize(
            listOf(
                VideoExportSettings.SourceImageSize(width = 3025, height = 4033),
            ),
        )

        assertEquals(3024, frameSize.width)
        assertEquals(4032, frameSize.height)
    }

    @Test
    fun calculateBitrateScalesWithResolutionAndFps() {
        val low = VideoExportSettings.calculateBitrate(
            width = 1536,
            height = 2048,
            fps = 3,
        )
        val high = VideoExportSettings.calculateBitrate(
            width = 3024,
            height = 4032,
            fps = 8,
        )

        assertEquals(9_437_184, low)
        assertEquals(VideoExportSettings.DEFAULT_MAX_VIDEO_BITRATE, high)
    }

    @Test
    fun calculateBitrateHonorsEncoderMaximum() {
        val bitrate = VideoExportSettings.calculateBitrate(
            width = 3024,
            height = 4032,
            fps = 5,
            maxBitrate = 18_000_000,
        )

        assertEquals(18_000_000, bitrate)
    }
}
