package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignatureStudyPresentationTest {
    @Test
    fun `width and height resolve the four signature viewport classes deterministically`() {
        assertEquals(SignatureStudyViewport.EXPANDED, SignatureStudyPresentationResolver.resolve(1400, 1000).viewport)
        assertEquals(SignatureStudyViewport.STANDARD, SignatureStudyPresentationResolver.resolve(900, 820).viewport)
        assertEquals(SignatureStudyViewport.COMPACT, SignatureStudyPresentationResolver.resolve(650, 700).viewport)
        assertEquals(SignatureStudyViewport.COMPRESSED, SignatureStudyPresentationResolver.resolve(520, 620).viewport)
    }

    @Test
    fun `compression protects hero and actions while reducing secondary content in order`() {
        val expanded = SignatureStudyPresentationResolver.resolve(1400, 1000)
        val compressed = SignatureStudyPresentationResolver.resolve(520, 620)

        assertEquals(
            listOf(
                SignatureCompressionStep.SPACING,
                SignatureCompressionStep.IMAGE,
                SignatureCompressionStep.SCHEDULER,
                SignatureCompressionStep.EXAMPLES,
                SignatureCompressionStep.METADATA
            ),
            compressed.compressionOrder
        )
        assertTrue(compressed.sectionSpacingDp < expanded.sectionSpacingDp)
        assertTrue(compressed.imageHeightFraction < expanded.imageHeightFraction)
        assertTrue(compressed.maximumVisibleExamples < expanded.maximumVisibleExamples)
        assertTrue(compressed.compactScheduler)
        assertTrue(compressed.compactMetadata)
        assertTrue(compressed.allowAnswerContentScroll)
        assertFalse(expanded.allowAnswerContentScroll)
    }
}
