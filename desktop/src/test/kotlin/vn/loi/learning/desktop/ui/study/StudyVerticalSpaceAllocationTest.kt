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

        assertEquals(116, portrait.typingMinHeightDp)
        assertEquals(portrait.imageMaxHeightDp, extreme.imageMaxHeightDp)
        assertEquals(portrait.imageMaxHeightDp, landscape.imageMaxHeightDp)
        assertEquals(portrait.imageMaxWidthDp, landscape.imageMaxWidthDp)
    }

    @Test
    fun `short viewport removes image before requiring bounded scroll`() {
        val short = allocation(StudyImageAspectClass.PORTRAIT, 360)
        val fitting = allocation(StudyImageAspectClass.PORTRAIT, 900)

        assertEquals(0, short.imageMaxHeightDp)
        assertTrue(short.requiresBoundedScroll)
        assertTrue(fitting.imageMaxHeightDp > 0)
        assertFalse(fitting.requiresBoundedScroll)
    }

    @Test
    fun `inventory and expanded examples consume surplus image budget`() {
        val base = allocation(StudyImageAspectClass.SQUARE, 1_000)
        val constrained = StudyVerticalSpaceAllocationResolver.resolve(
            input(StudyImageAspectClass.SQUARE, 1_000).copy(
                inventoryVisible = true,
                examplesExpanded = true
            )
        )
        assertTrue(constrained.imageMaxHeightDp < base.imageMaxHeightDp)
    }

    private fun allocation(aspect: StudyImageAspectClass, height: Int) =
        StudyVerticalSpaceAllocationResolver.resolve(input(aspect, height))

    private fun input(aspect: StudyImageAspectClass, height: Int) = StudyVerticalSpaceInput(
        viewportWidthDp = 900,
        viewportHeightDp = height,
        imageAspectClass = aspect,
        typingRequired = true,
        decisionDockRequired = true,
        bottomControlsHeightDp = 112,
        inventoryVisible = false,
        examplesExpanded = false
    )
}
