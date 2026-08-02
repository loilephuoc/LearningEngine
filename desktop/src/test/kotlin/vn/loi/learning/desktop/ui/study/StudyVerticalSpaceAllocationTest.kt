package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudyVerticalSpaceAllocationTest {
    @Test
    fun `portrait and extreme portrait yield image space before typing or dock`() {
        val portrait = allocation(StudyImageAspectClass.PORTRAIT, 760)
        val extreme = allocation(StudyImageAspectClass.EXTREME, 760)
        val landscape = allocation(StudyImageAspectClass.STANDARD_LANDSCAPE, 760)

        assertEquals(156, portrait.typingMinHeightDp)
        assertTrue(extreme.imageMaxHeightDp <= portrait.imageMaxHeightDp)
        assertTrue(portrait.imageMaxHeightDp < landscape.imageMaxHeightDp)
        assertTrue(portrait.imageMaxWidthDp < landscape.imageMaxWidthDp)
    }

    @Test
    fun `short viewport removes image before requiring bounded scroll`() {
        val short = allocation(StudyImageAspectClass.PORTRAIT, 450)
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
