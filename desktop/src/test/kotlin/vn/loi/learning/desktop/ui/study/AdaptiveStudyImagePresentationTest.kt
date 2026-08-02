package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun `frame follows rendered dimensions for landscape square and portrait`() {
        val wide = resolve(1600, 900)
        val square = resolve(900, 900)
        val portrait = resolve(700, 1000)

        assertEquals(498, wide.renderedWidthDp)
        assertEquals(280, wide.renderedHeightDp)
        assertEquals(wide.renderedWidthDp, wide.frameWidthDp)
        assertEquals(wide.renderedHeightDp, wide.frameHeightDp)
        assertEquals(280, square.frameHeightDp)
        assertEquals(square.renderedWidthDp, square.frameWidthDp)
        assertEquals(280, portrait.frameHeightDp)
        assertEquals(portrait.renderedHeightDp, portrait.frameHeightDp)
    }

    @Test
    fun `small intrinsic images have bounded upscale and every frame stays within budget`() {
        val small = resolve(120, 120)
        val extreme = resolve(2000, 300)

        assertEquals(162, small.renderedWidthDp)
        assertEquals(162, small.frameWidthDp)
        assertEquals(162, small.frameHeightDp)
        assertFalse(small.sourceUpscaleAllowed)
        assertEquals(680, extreme.renderedWidthDp)
        assertEquals(102, extreme.renderedHeightDp)
        assertEquals(extreme.renderedHeightDp, extreme.frameHeightDp)
    }

    @Test
    fun `large landscape uses usable width when vertical budget permits`() {
        val landscape = AdaptiveStudyImagePresentationResolver.resolve(1600, 900, 680, 500)

        assertEquals(680, landscape.renderedWidthDp)
        assertEquals(383, landscape.renderedHeightDp)
        assertTrue(landscape.sourceUpscaleAllowed)
        assertEquals(landscape.renderedWidthDp, landscape.frameWidthDp)
        assertEquals(landscape.renderedHeightDp, landscape.frameHeightDp)
    }

    private fun assertClass(width: Int, height: Int, expected: StudyImageAspectClass) {
        assertEquals(expected, resolve(width, height).aspectClass)
    }

    private fun resolve(width: Int, height: Int) =
        AdaptiveStudyImagePresentationResolver.resolve(width, height, 680, 280)
}
