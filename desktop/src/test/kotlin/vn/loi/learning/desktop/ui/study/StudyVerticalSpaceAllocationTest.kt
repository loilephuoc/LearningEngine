package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudyVerticalSpaceAllocationTest {
    @Test
    fun `all aspect classes receive the same real surplus without arbitrary portrait shrink`() {
        val portrait = allocation(StudyImageAspectClass.PORTRAIT, 760)
        val extreme = allocation(StudyImageAspectClass.EXTREME, 760)
        val landscape = allocation(StudyImageAspectClass.STANDARD_LANDSCAPE, 760)

        assertEquals(96, portrait.typingMinHeightDp)
        assertEquals(portrait.imageMaxHeightDp, extreme.imageMaxHeightDp)
        assertEquals(portrait.imageMaxHeightDp, landscape.imageMaxHeightDp)
        assertEquals(portrait.imageMaxWidthDp, landscape.imageMaxWidthDp)
    }

    @Test
    fun `compacted short viewport gives reclaimed timer space back to image`() {
        val short = allocation(StudyImageAspectClass.PORTRAIT, 200)
        val fitting = allocation(StudyImageAspectClass.PORTRAIT, 900)

        assertTrue(short.imageMaxHeightDp > 0)
        assertFalse(short.requiresBoundedScroll)
        assertTrue(fitting.imageMaxHeightDp > 0)
        assertFalse(fitting.requiresBoundedScroll)
    }

    @Test
    fun `inventory and expanded examples consume surplus image budget`() {
        val base = allocation(StudyImageAspectClass.SQUARE, 1_000)
        val constrained = StudyVerticalSpaceAllocationResolver.resolve(
            input(StudyImageAspectClass.SQUARE, 1_000).copy(
                externalReservedHeightDp = 76,
                examplesExpanded = true
            )
        )
        assertTrue(constrained.imageMaxHeightDp < base.imageMaxHeightDp)
    }

    @Test
    fun `realistic typing heights preserve input and reduce image monotonically`() {
        val short = allocation(StudyImageAspectClass.STANDARD_LANDSCAPE, 520)
        val medium = allocation(StudyImageAspectClass.STANDARD_LANDSCAPE, 680)
        val tall = allocation(StudyImageAspectClass.STANDARD_LANDSCAPE, 900)

        assertEquals(96, short.typingMinHeightDp)
        assertEquals(short.typingMinHeightDp, medium.typingMinHeightDp)
        assertEquals(medium.typingMinHeightDp, tall.typingMinHeightDp)
        assertTrue(short.imageMaxHeightDp > 0)
        assertTrue(short.imageMaxHeightDp < medium.imageMaxHeightDp)
        assertTrue(medium.imageMaxHeightDp < tall.imageMaxHeightDp)
        assertFalse(short.requiresBoundedScroll)
        assertFalse(medium.requiresBoundedScroll)
        assertFalse(tall.requiresBoundedScroll)
    }

    private fun allocation(aspect: StudyImageAspectClass, height: Int) =
        StudyVerticalSpaceAllocationResolver.resolve(input(aspect, height))

    private fun input(aspect: StudyImageAspectClass, height: Int) = StudyVerticalSpaceInput(
        viewportWidthDp = 900,
        viewportHeightDp = height,
        imageAspectClass = aspect,
        typingRequired = true,
        externalReservedHeightDp = 0,
        examplesExpanded = false
    )
}
