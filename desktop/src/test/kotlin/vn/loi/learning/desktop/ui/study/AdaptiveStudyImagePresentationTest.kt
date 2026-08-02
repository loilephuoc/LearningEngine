package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptiveStudyImagePresentationTest {
    @Test
    fun `aspect ratio classes are deterministic at representative boundaries`() {
        assertClass(2400, 800, StudyImageAspectClass.EXTREME)
        assertClass(1600, 900, StudyImageAspectClass.WIDE_LANDSCAPE)
        assertClass(1200, 900, StudyImageAspectClass.STANDARD_LANDSCAPE)
        assertClass(900, 900, StudyImageAspectClass.SQUARE)
        assertClass(700, 1000, StudyImageAspectClass.PORTRAIT)
        assertClass(400, 1200, StudyImageAspectClass.EXTREME)
    }

    @Test
    fun `landscape uses width while portrait and square retain useful height`() {
        val wide = resolve(1600, 900)
        val square = resolve(900, 900)
        val portrait = resolve(700, 1000)

        assertEquals(680, wide.maximumWidthDp)
        assertTrue(wide.frameHeightDp < square.frameHeightDp)
        assertEquals(280, square.frameHeightDp)
        assertEquals(280, portrait.frameHeightDp)
    }

    @Test
    fun `small intrinsic images have bounded upscale and every frame stays within budget`() {
        val small = resolve(120, 120)
        val extreme = resolve(2000, 300)

        assertEquals(162, small.maximumWidthDp)
        assertTrue(small.frameHeightDp in 112..280)
        assertTrue(extreme.frameHeightDp in 112..280)
    }

    private fun assertClass(width: Int, height: Int, expected: StudyImageAspectClass) {
        assertEquals(expected, resolve(width, height).aspectClass)
    }

    private fun resolve(width: Int, height: Int) =
        AdaptiveStudyImagePresentationResolver.resolve(width, height, 680, 280)
}
