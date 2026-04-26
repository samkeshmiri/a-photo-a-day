package com.skeshmiri.everyday.ui.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryVideoExportDefaultsTest {
    @Test
    fun `estimatedDurationSeconds rounds to one decimal place`() {
        assertEquals(7.2, GalleryVideoExportDefaults.estimatedDurationSeconds(photoCount = 36, fps = 5), 0.0)
        assertEquals(0.1, GalleryVideoExportDefaults.estimatedDurationSeconds(photoCount = 1, fps = 8), 0.0)
    }
}
