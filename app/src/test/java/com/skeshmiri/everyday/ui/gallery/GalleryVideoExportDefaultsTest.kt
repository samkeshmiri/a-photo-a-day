package com.skeshmiri.everyday.ui.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryVideoExportDefaultsTest {
    @Test
    fun `estimatedDurationSeconds rounds to one decimal place`() {
        assertEquals(1.5, GalleryVideoExportDefaults.estimatedDurationSeconds(photoCount = 36, fps = 24), 0.0)
        assertEquals(0.0, GalleryVideoExportDefaults.estimatedDurationSeconds(photoCount = 1, fps = 30), 0.0)
    }
}
