package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StudyFullAnswerHeightAdaptationTest {
    private val commonTraits =
        StudyVisualContentTraits(
            hasImage = true,
            hasPronunciation = true,
            hasPartOfSpeech = true,
            hasExamples = true
        )

    @Test
    fun `1200 viewport is comfortable while 1080 compresses image and preserves bilingual example`() {
        val comfortable = StudyVisualLayoutResolver.resolve(1920, 1200, commonTraits)
        val compact = StudyVisualLayoutResolver.resolve(1920, 1080, commonTraits)

        assertEquals(FullAnswerDensityClass.COMFORTABLE, comfortable.fullAnswerDensityClass)
        assertEquals(FullAnswerDensityClass.COMPACT, compact.fullAnswerDensityClass)
        assertTrue(compact.fullAnswerSectionGapDp < comfortable.fullAnswerSectionGapDp)
        assertTrue(compact.fullAnswerCardVerticalPaddingDp < comfortable.fullAnswerCardVerticalPaddingDp)
        assertTrue(compact.fullAnswerImageMaxHeightDp < comfortable.fullAnswerImageMaxHeightDp)
        assertTrue(comfortable.fullAnswerExampleBudgetDp >= 120)
        assertTrue(compact.fullAnswerExampleBudgetDp >= 120)
        assertTrue(comfortable.commonAnswerFitsWithoutScroll)
        assertTrue(compact.commonAnswerFitsWithoutScroll)
    }

    @Test
    fun `height alone recomputes full answer image budget`() {
        val taller = StudyVisualLayoutResolver.resolve(1280, 1200, commonTraits)
        val shorter = StudyVisualLayoutResolver.resolve(1280, 1080, commonTraits)

        assertEquals(taller.imageMaxWidthDp, shorter.imageMaxWidthDp)
        assertNotEquals(taller.fullAnswerImageMaxHeightDp, shorter.fullAnswerImageMaxHeightDp)
        assertTrue(taller.fullAnswerImageMaxHeightDp > shorter.fullAnswerImageMaxHeightDp)
    }

    @Test
    fun `density and font scale participate in full answer calculation`() {
        val normal =
            StudyVisualLayoutResolver.resolve(
                StudyDisplayEnvironment(1280, 1080, density = 1f, fontScale = 1f),
                commonTraits
            )
        val denser =
            StudyVisualLayoutResolver.resolve(
                StudyDisplayEnvironment(1280, 1080, density = 1.3f, fontScale = 1f),
                commonTraits
            )
        val scaled =
            StudyVisualLayoutResolver.resolve(
                StudyDisplayEnvironment(1280, 1080, density = 1f, fontScale = 1.25f),
                commonTraits
            )

        assertNotEquals(normal.fullAnswerImageMaxHeightDp, denser.fullAnswerImageMaxHeightDp)
        assertTrue(scaled.fullAnswerImageMaxHeightDp < normal.fullAnswerImageMaxHeightDp)
    }

    @Test
    fun `extremely low viewport keeps readable image minimum and enables scroll fallback`() {
        val minimum = StudyVisualLayoutResolver.resolve(800, 500, commonTraits)
        val belowSupported =
            StudyVisualLayoutResolver.resolve(
                800,
                StudyVisualLayoutResolver.MINIMUM_SUPPORTED_FULL_ANSWER_HEIGHT_DP - 1,
                commonTraits
            )

        assertEquals(FullAnswerDensityClass.MINIMUM, minimum.fullAnswerDensityClass)
        assertTrue(minimum.fullAnswerImageMaxHeightDp >= 96)
        assertFalse(minimum.commonAnswerFitsWithoutScroll)
        assertFalse(belowSupported.commonAnswerFitsWithoutScroll)
    }

    @Test
    fun `full answer wrapper and image share adaptive bound while pre answer keeps original authority`() {
        val answer = studySource("FocusedAnswerSurface.kt")
        val renderer = studySource("LearningSceneRenderer.kt")
        val screen = studySource("StudyScreen.kt")

        assertTrue(answer.contains("imageMaxHeightDp = resolvedLayout.fullAnswerImageMaxHeightDp"))
        assertTrue(answer.contains("val maxH = imageMaxHeightDp.dp"))
        assertTrue(answer.contains(".heightIn(max = maxH)"))
        assertTrue(answer.contains("contentScale = ContentScale.Fit"))
        assertFalse(renderer.contains("fullAnswerImageMaxHeightDp"))
        assertTrue(screen.contains("remember(displayEnvironment, visualTraits)"))
        assertTrue(screen.contains(".verticalScroll(rememberScrollState())"))
    }

    @Test
    fun `rating dock remains fixed outside scroll and resolver has no monitor resolution branch`() {
        val screen = studySource("StudyScreen.kt")
        val resolver = studySource("StudyVisualLayout.kt")
        val scrollPosition = screen.indexOf(".verticalScroll(rememberScrollState())")
        val dockPosition = screen.indexOf("ActionDock(")

        assertTrue(scrollPosition >= 0)
        assertTrue(dockPosition > scrollPosition)
        assertFalse(resolver.contains("1080"))
        assertFalse(resolver.contains("1200"))
        assertFalse(resolver.contains("monitor"))
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
