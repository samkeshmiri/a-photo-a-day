package com.skeshmiri.aphotoaday.export

internal object VideoExportSettings {
    const val DEFAULT_MAX_VIDEO_BITRATE = 30_000_000
    private const val MIN_VIDEO_BITRATE = 4_000_000

    fun pickTargetFrameSize(sourceSizes: List<SourceImageSize>): ResolvedVideoSize {
        require(sourceSizes.isNotEmpty()) { "At least one source image size is required." }

        val source = sourceSizes.maxWith(
            compareBy<SourceImageSize> { it.width.toLong() * it.height.toLong() }
                .thenBy { it.width }
                .thenBy { it.height },
        )

        return ResolvedVideoSize(
            width = makeEven(source.width.coerceAtLeast(2)),
            height = makeEven(source.height.coerceAtLeast(2)),
        )
    }

    fun calculateBitrate(
        width: Int,
        height: Int,
        fps: Int,
        maxBitrate: Int = DEFAULT_MAX_VIDEO_BITRATE,
    ): Int {
        require(width > 0) { "Width must be greater than zero." }
        require(height > 0) { "Height must be greater than zero." }
        require(fps > 0) { "Frames per second must be greater than zero." }
        require(maxBitrate > 0) { "Max bitrate must be greater than zero." }

        val desiredBitrate = width.toLong() * height.toLong() * fps.toLong()
        if (maxBitrate <= MIN_VIDEO_BITRATE) {
            return maxBitrate
        }

        return desiredBitrate
            .coerceIn(MIN_VIDEO_BITRATE.toLong(), maxBitrate.toLong())
            .toInt()
    }

    data class SourceImageSize(
        val width: Int,
        val height: Int,
    )

    data class ResolvedVideoSize(
        val width: Int,
        val height: Int,
    ) {
        val frameByteCount: Int = width * height * 3 / 2
    }

    private fun makeEven(value: Int): Int = if (value % 2 == 0) value else value - 1
}
