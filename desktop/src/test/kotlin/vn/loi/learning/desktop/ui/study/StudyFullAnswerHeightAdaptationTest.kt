package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudyFullAnswerHeightAdaptationTest {
    private val commonBlocks =
        FullAnswerMeasuredBlocks(
            identityHeight = 104,
            meaningHeight = 92,
            requiredExampleHeight = 164
        )

    @Test
    fun `measured common geometry fits both desktop body heights without overlap`() {
        val taller = geometry(availableHeight = 820)
        val shorter = geometry(availableHeight = 700)

        assertActualOrderAndFit(taller, 820)
        assertActualOrderAndFit(shorter, 700)
        assertTrue(taller.imageHeight > shorter.imageHeight)
    }

    @Test
    fun `height-only resize recomputes measured image remainder`() {
        val first = geometry(availableHeight = 760)
        val resized = geometry(availableHeight = 680)

        assertEquals(first.identityBottom, resized.identityBottom)
        assertTrue(first.imageHeight > resized.imageHeight)
        assertEquals(80, first.imageHeight - resized.imageHeight)
    }

    @Test
    fun `scheduler feedback is continuation and returns its budget to image`() {
        val legacyRequiredFeedbackImageHeight =
            780 - (
                6 * 2 +
                    commonBlocks.identityHeight +
                    commonBlocks.meaningHeight +
                    commonBlocks.requiredExampleHeight +
                    72 +
                    8 * 4
                )
        val result =
            geometry(
                availableHeight = 780,
                blocks = commonBlocks.copy(schedulerFeedbackHeight = 72)
            )

        assertTrue(result.fitsWithoutScroll)
        assertEquals(304, legacyRequiredFeedbackImageHeight)
        assertEquals(384, result.imageHeight)
        assertEquals(80, result.imageHeight - legacyRequiredFeedbackImageHeight)
        assertTrue(requireNotNull(result.schedulerFeedbackTop) >= result.requiredExampleBottom)
        assertTrue(result.requiredExampleBottom <= 780)
        assertTrue(requireNotNull(result.schedulerFeedbackBottom) > 780)
    }

    @Test
    fun `long bilingual example and low viewport use scroll fallback without overlap`() {
        val longExample =
            geometry(
                availableHeight = 540,
                blocks = commonBlocks.copy(requiredExampleHeight = 300)
            )

        assertFalse(longExample.fitsWithoutScroll)
        assertTrue(longExample.imageHeight >= 96)
        assertTrue(longExample.requiredExampleBottom > 540)
        assertTrue(longExample.meaningBottom <= longExample.requiredExampleTop)
    }

    @Test
    fun `additional examples are continuation and do not steal first pair image budget`() {
        val withoutContinuation = geometry(availableHeight = 700)
        val withContinuation =
            geometry(
                availableHeight = 700,
                blocks = commonBlocks.copy(continuationHeight = 220)
            )

        assertEquals(withoutContinuation.imageHeight, withContinuation.imageHeight)
        assertEquals(withoutContinuation.requiredExampleBottom, withContinuation.requiredExampleBottom)
        assertTrue(withContinuation.fitsWithoutScroll)
        assertTrue(withContinuation.totalHeight > 700)
        assertTrue(requireNotNull(withContinuation.continuationTop) >= withContinuation.requiredExampleBottom)
    }

    @Test
    fun `production uses actual body constraints and two-pass measured blocks`() {
        val screen = studySource("StudyScreen.kt")
        val fit = studySource("FullAnswerFitLayout.kt")
        val answer = studySource("FocusedAnswerSurface.kt")
        val renderer = studySource("LearningSceneRenderer.kt")

        assertTrue(screen.contains("fullAnswerAvailableBodyHeightDp"))
        assertTrue(screen.contains("maxHeight - LESpacing.sm * 2 - LETheme.spacing.space5 * 2"))
        assertTrue(fit.contains("SubcomposeLayout"))
        assertTrue(fit.contains("FullAnswerMeasuredBlocks("))
        assertTrue(fit.contains("maxHeight = geometry.imageHeight"))
        assertFalse(answer.contains("FullAnswerFitLayout("))
        assertTrue(answer.contains("spacePresentation.imageMaximumHeightDp"))
        assertTrue(answer.contains("ResponsiveAnswerSupportingRegion("))
        assertTrue(answer.contains("if (spacePresentation.showAllExampleContent) disclosure.examples"))
        assertTrue(answer.contains("FullAnswerResponsivePolicyResolver.resolve(availableContentWidthDp)"))
        assertTrue(answer.contains("contentScale = ContentScale.Fit"))
        assertFalse(renderer.contains("FullAnswerFitLayout"))
    }

    @Test
    fun `rating dock remains outside measured scroll body`() {
        val screen = studySource("StudyScreen.kt")
        val measuredBody = screen.indexOf("fullAnswerAvailableBodyHeightDp")
        val dock = screen.indexOf("ActionDock(")
        val status = screen.indexOf("StatusStrip(")

        assertTrue(measuredBody >= 0)
        assertTrue(dock > measuredBody)
        assertTrue(status > dock)
    }

    private fun geometry(
        availableHeight: Int,
        blocks: FullAnswerMeasuredBlocks = commonBlocks
    ): FullAnswerFitGeometry =
        resolveFullAnswerFitGeometry(
            availableHeight = availableHeight,
            verticalPadding = 6,
            sectionGap = 8,
            minimumImageHeight = 96,
            blocks = blocks
        )

    private fun assertActualOrderAndFit(
        geometry: FullAnswerFitGeometry,
        availableHeight: Int
    ) {
        assertTrue(geometry.identityBottom < geometry.imageTop)
        assertTrue(geometry.imageBottom < geometry.meaningTop)
        assertTrue(geometry.meaningBottom < geometry.requiredExampleTop)
        assertTrue(geometry.requiredExampleBottom <= availableHeight)
        assertTrue(geometry.fitsWithoutScroll)
    }

    private fun studySource(name: String): String = studySourceDirectory().resolve(name).readText()

    private fun studySourceDirectory(): File {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study")
        return if (fromRoot.isDirectory) {
            fromRoot
        } else {
            File("src/main/kotlin/vn/loi/learning/desktop/ui/study")
        }
    }
}
