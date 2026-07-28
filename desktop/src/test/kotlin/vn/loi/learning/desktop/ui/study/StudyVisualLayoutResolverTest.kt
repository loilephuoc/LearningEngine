package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StudyVisualLayoutResolverTest {

    private val defaultTraits = StudyVisualContentTraits(
        hasImage = true,
        hasPronunciation = true,
        hasPartOfSpeech = true,
        hasExamples = true,
        hasSchedulerFeedback = true
    )

    @Test
    fun `1 - rejects negative width`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            StudyVisualLayoutResolver.resolve(-100, 700, defaultTraits)
        }
        assertTrue(ex.message!!.contains("Viewport width must be positive"))
    }

    @Test
    fun `2 - rejects negative height`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            StudyVisualLayoutResolver.resolve(800, -50, defaultTraits)
        }
        assertTrue(ex.message!!.contains("Viewport height must be positive"))
    }

    @Test
    fun `3 - compact lower range`() {
        val layout = StudyVisualLayoutResolver.resolve(320, 500, defaultTraits)
        assertEquals(StudyViewportClass.COMPACT, layout.viewportClass)
        assertEquals(320, layout.contentMaxWidthDp)
        assertEquals(36, layout.identityWordFontSizeSp)
        assertEquals(MetadataArrangement.STACKED, layout.metadataArrangement)
        assertEquals(RatingArrangement.GRID_2X2, layout.ratingArrangement)
    }

    @Test
    fun `4 - exact compact standard boundary`() {
        val compactEdge = StudyVisualLayoutResolver.resolve(599, 800, defaultTraits)
        assertEquals(StudyViewportClass.COMPACT, compactEdge.viewportClass)

        val standardEdge = StudyVisualLayoutResolver.resolve(600, 800, defaultTraits)
        assertEquals(StudyViewportClass.STANDARD, standardEdge.viewportClass)
        assertEquals(680, standardEdge.contentMaxWidthDp)
        assertEquals(46, standardEdge.identityWordFontSizeSp)
        assertEquals(MetadataArrangement.INLINE, standardEdge.metadataArrangement)
        assertEquals(RatingArrangement.HORIZONTAL, standardEdge.ratingArrangement)
    }

    @Test
    fun `5 - standard range`() {
        val layout = StudyVisualLayoutResolver.resolve(800, 900, defaultTraits)
        assertEquals(StudyViewportClass.STANDARD, layout.viewportClass)
        assertEquals(680, layout.contentMaxWidthDp)
        assertEquals(612, layout.imageMaxWidthDp)
        assertEquals(232, layout.imageMaxHeightDp)
        assertEquals(StudyHeightMode.COMFORTABLE, layout.heightMode)
        assertEquals(72, layout.statisticsDashboardReservedHeightDp)
        assertEquals(128, layout.headerReservedHeightDp)
        assertEquals(636, layout.availableAnswerHeightDp)
        assertEquals(46, layout.identityWordFontSizeSp)
        assertEquals(12, layout.sectionSpacingDp)
        assertEquals(88, layout.ratingDockReservedHeightDp)
        assertEquals(64, layout.ratingButtonHeightDp)
        assertEquals(48, layout.frontRatingSegmentHeightDp)
    }

    @Test
    fun `6 - exact standard wide boundary`() {
        val standardEdge = StudyVisualLayoutResolver.resolve(1023, 800, defaultTraits)
        assertEquals(StudyViewportClass.STANDARD, standardEdge.viewportClass)

        val wideEdge = StudyVisualLayoutResolver.resolve(1024, 800, defaultTraits)
        assertEquals(StudyViewportClass.WIDE, wideEdge.viewportClass)
        assertEquals(800, wideEdge.contentMaxWidthDp)
        assertEquals(52, wideEdge.identityWordFontSizeSp)
        assertEquals(8, wideEdge.sectionSpacingDp)
    }

    @Test
    fun `7 - wide range`() {
        val layout = StudyVisualLayoutResolver.resolve(1920, 1080, defaultTraits)
        assertEquals(StudyViewportClass.WIDE, layout.viewportClass)
        assertEquals(800, layout.contentMaxWidthDp)
        assertEquals(720, layout.imageMaxWidthDp)
        assertEquals(396, layout.imageMaxHeightDp)
        assertEquals(52, layout.identityWordFontSizeSp)
        assertEquals(16, layout.sectionSpacingDp)
    }

    @Test
    fun `8 - deterministic repeated resolve`() {
        val first = StudyVisualLayoutResolver.resolve(800, 600, defaultTraits)
        val second = StudyVisualLayoutResolver.resolve(800, 600, defaultTraits)
        assertEquals(first, second)
    }

    @Test
    fun `9 - image present`() {
        val layout = StudyVisualLayoutResolver.resolve(800, 700, defaultTraits.copy(hasImage = true))
        assertTrue(layout.imageMaxWidthDp > 0)
        assertTrue(layout.imageMaxHeightDp > 0)
    }

    @Test
    fun `10 - image absent`() {
        val layout = StudyVisualLayoutResolver.resolve(800, 700, defaultTraits.copy(hasImage = false))
        assertEquals(0, layout.imageMaxWidthDp)
        assertEquals(0, layout.imageMaxHeightDp)
    }

    @Test
    fun `11 - short viewport height bounds image more conservatively`() {
        val normal = StudyVisualLayoutResolver.resolve(800, 1080, defaultTraits)
        val short = StudyVisualLayoutResolver.resolve(800, 500, defaultTraits)
        assertTrue(short.imageMaxHeightDp < normal.imageMaxHeightDp)
        assertEquals(120, short.imageMaxHeightDp)
        assertEquals(412, normal.imageMaxHeightDp)
        assertTrue(short.availableAnswerHeightDp < normal.availableAnswerHeightDp)
    }

    @Test
    fun `12 - partial IPA POS metadata`() {
        val traitsNoIpa = defaultTraits.copy(hasPronunciation = false, hasPartOfSpeech = true)
        val layout = StudyVisualLayoutResolver.resolve(500, 800, traitsNoIpa)
        assertNotNull(layout)
        assertEquals(StudyViewportClass.COMPACT, layout.viewportClass)
    }

    @Test
    fun `13 - no metadata`() {
        val traitsNoMeta = defaultTraits.copy(hasPronunciation = false, hasPartOfSpeech = false)
        val layout = StudyVisualLayoutResolver.resolve(700, 800, traitsNoMeta)
        assertEquals(StudyViewportClass.STANDARD, layout.viewportClass)
    }

    @Test
    fun `14 - examples present`() {
        val layout = StudyVisualLayoutResolver.resolve(800, 700, defaultTraits.copy(hasExamples = true))
        assertNotNull(layout)
    }

    @Test
    fun `15 - examples absent`() {
        val layout = StudyVisualLayoutResolver.resolve(800, 700, defaultTraits.copy(hasExamples = false))
        assertNotNull(layout)
    }

    @Test
    fun `16 - scheduler feedback present`() {
        val layout = StudyVisualLayoutResolver.resolve(800, 700, defaultTraits.copy(hasSchedulerFeedback = true))
        assertNotNull(layout)
    }

    @Test
    fun `17 - content max width bounded at wide viewport`() {
        val layout = StudyVisualLayoutResolver.resolve(2560, 1440, defaultTraits)
        assertEquals(800, layout.contentMaxWidthDp)
    }

    @Test
    fun `18 - rating arrangement remains available in compact`() {
        val compactLayout = StudyVisualLayoutResolver.resolve(360, 640, defaultTraits)
        assertEquals(RatingArrangement.GRID_2X2, compactLayout.ratingArrangement)
        assertEquals(120, compactLayout.ratingDockReservedHeightDp)
        assertTrue(compactLayout.preserveRatingReachability)
    }

    @Test
    fun `19 - long content layout uses wrapping safe arrangement`() {
        val narrowLayout = StudyVisualLayoutResolver.resolve(400, 800, defaultTraits)
        assertEquals(MetadataArrangement.STACKED, narrowLayout.metadataArrangement)
        assertEquals(RatingArrangement.GRID_2X2, narrowLayout.ratingArrangement)
    }

    @Test
    fun `21 - rating arrangement returns GRID_2X2 below narrow threshold`() {
        val narrow = StudyVisualLayoutResolver.resolve(479, 800, defaultTraits)
        assertEquals(RatingArrangement.GRID_2X2, narrow.ratingArrangement)
    }

    @Test
    fun `22 - rating arrangement returns HORIZONTAL at and above narrow threshold`() {
        val exactBoundary = StudyVisualLayoutResolver.resolve(480, 800, defaultTraits)
        assertEquals(RatingArrangement.HORIZONTAL, exactBoundary.ratingArrangement)

        val wide = StudyVisualLayoutResolver.resolve(800, 800, defaultTraits)
        assertEquals(RatingArrangement.HORIZONTAL, wide.ratingArrangement)
    }

    @Test
    fun `23 - exact rating breakpoint boundary`() {
        assertEquals(
            RatingArrangement.GRID_2X2,
            StudyVisualLayoutResolver.resolve(StudyVisualLayoutResolver.RATING_GRID_MAX_WIDTH_DP, 800, defaultTraits).ratingArrangement
        )
        assertEquals(
            RatingArrangement.HORIZONTAL,
            StudyVisualLayoutResolver.resolve(StudyVisualLayoutResolver.RATING_GRID_MAX_WIDTH_DP + 1, 800, defaultTraits).ratingArrangement
        )
    }

    @Test
    fun `24 - header and dock are reserved before answer image budget`() {
        val layout = StudyVisualLayoutResolver.resolve(800, 500, defaultTraits)

        assertEquals(StudyHeightMode.MINIMUM_HEIGHT, layout.heightMode)
        assertEquals(72, layout.statisticsDashboardReservedHeightDp)
        assertEquals(112, layout.headerReservedHeightDp)
        assertEquals(64, layout.ratingDockReservedHeightDp)
        assertEquals(294, layout.availableAnswerHeightDp)
        assertTrue(layout.imageMaxHeightDp <= 200)
        assertTrue(layout.preserveRatingReachability)
    }

    @Test
    fun `density and font scale are explicit display environment inputs`() {
        val normal = StudyVisualLayoutResolver.resolve(
            StudyDisplayEnvironment(800, 900, density = 1f, fontScale = 1f),
            defaultTraits
        )
        val scaled = StudyVisualLayoutResolver.resolve(
            StudyDisplayEnvironment(800, 900, density = 1.5f, fontScale = 2f),
            defaultTraits
        )

        assertEquals(StudyHeightMode.COMFORTABLE, normal.heightMode)
        assertEquals(StudyHeightMode.MINIMUM_HEIGHT, scaled.heightMode)
        assertTrue(scaled.imageMaxHeightDp < normal.imageMaxHeightDp)
    }

    @Test
    fun `height modes and image budgets adapt without monitor hardcodes`() {
        val comfortable = StudyVisualLayoutResolver.resolve(800, 1080, defaultTraits)
        val compact = StudyVisualLayoutResolver.resolve(800, 800, defaultTraits)
        val minimum = StudyVisualLayoutResolver.resolve(800, 640, defaultTraits)

        assertEquals(StudyHeightMode.COMFORTABLE, comfortable.heightMode)
        assertEquals(StudyHeightMode.COMPACT_HEIGHT, compact.heightMode)
        assertEquals(StudyHeightMode.MINIMUM_HEIGHT, minimum.heightMode)
        assertTrue(comfortable.imageMaxHeightDp > compact.imageMaxHeightDp)
        assertTrue(compact.imageMaxHeightDp >= minimum.imageMaxHeightDp)
        assertTrue(minimum.ratingDockReservedHeightDp > 0)
        assertEquals(56, compact.ratingButtonHeightDp)
        assertEquals(52, minimum.ratingButtonHeightDp)
        assertEquals(40, minimum.frontRatingSegmentHeightDp)
        assertEquals(36, minimum.topActionHeightDp)
    }

    @Test
    fun `common answer with both example rows fits supported compact height`() {
        val layout = StudyVisualLayoutResolver.resolve(800, 720, defaultTraits)

        assertEquals(StudyHeightMode.COMPACT_HEIGHT, layout.heightMode)
        assertTrue(layout.commonAnswerFitsWithoutScroll)
        assertTrue(layout.imageMaxHeightDp >= 120)
        assertTrue(layout.availableAnswerHeightDp >
            layout.ratingButtonHeightDp + layout.frontRatingSegmentHeightDp)
    }
}
