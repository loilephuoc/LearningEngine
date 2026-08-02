package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveStudySpacePresentationTest {
    @Test
    fun `collapsed answer gives surplus height to image`() {
        val collapsed = resolve(1440, 900, false)
        val expanded = resolve(1440, 900, true)

        assertTrue(collapsed.imageMaximumHeightDp > expanded.imageMaximumHeightDp)
        assertTrue(collapsed.imageMaximumHeightDp > collapsed.imageMinimumHeightDp)
        assertFalse(collapsed.showAllExampleContent)
        assertTrue(expanded.showAllExampleContent)
    }

    @Test
    fun `wide and compact allocations retain usable image and typing field`() {
        val wide = resolve(1440, 900, true)
        val compact = resolve(520, 680, true)

        assertTrue(wide.typingFieldMinimumHeightDp >= 168)
        assertTrue(compact.typingFieldMinimumHeightDp >= 144)
        assertTrue(wide.imageMaximumHeightDp >= wide.imageMinimumHeightDp)
        assertTrue(compact.imageMaximumHeightDp >= compact.imageMinimumHeightDp)
    }

    @Test
    fun `bounded scrolling is last resort for genuinely constrained answer`() {
        assertFalse(resolve(1440, 1000, true).allowBoundedContentScroll)
        assertTrue(resolve(520, 560, true).allowBoundedContentScroll)
    }

    private fun resolve(width: Int, height: Int, expanded: Boolean) =
        AdaptiveStudySpacePresentationResolver.resolve(
            AdaptiveStudySpaceRequest(
                isAnswer = true,
                examplesExpanded = expanded,
                hasExamples = true,
                viewportWidthDp = width,
                viewportHeightDp = height,
                bottomControlHeightDp = 88,
                intrinsicExampleHeightDp = 220,
                imageAspectClass = StudyImageAspectClass.STANDARD_LANDSCAPE
            )
        )
}
