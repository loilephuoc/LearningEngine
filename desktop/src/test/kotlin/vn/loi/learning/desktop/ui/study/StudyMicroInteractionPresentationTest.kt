package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudyMicroInteractionPresentationTest {
    @Test
    fun `reveal timeline establishes answer then meaning then scheduler`() {
        val early = StudyMicroInteractionResolver.reveal(0.25f)
        val middle = StudyMicroInteractionResolver.reveal(0.6f)

        assertTrue(early.answerAlpha > early.meaningAlpha)
        assertTrue(early.meaningAlpha > early.schedulerAlpha)
        assertTrue(middle.answerAlpha > middle.meaningAlpha)
        assertTrue(middle.meaningAlpha > middle.schedulerAlpha)
    }

    @Test
    fun `reveal timeline is bounded deterministic and settles without transform`() {
        assertEquals(
            StudyMicroInteractionResolver.reveal(0.5f),
            StudyMicroInteractionResolver.reveal(0.5f)
        )
        assertEquals(StudyMicroInteractionResolver.reveal(0f), StudyMicroInteractionResolver.reveal(-1f))
        assertEquals(StudyMicroInteractionResolver.reveal(1f), StudyMicroInteractionResolver.reveal(2f))
        val settled = StudyMicroInteractionResolver.reveal(1f)
        assertEquals(1f, settled.answerAlpha)
        assertEquals(1f, settled.meaningAlpha)
        assertEquals(1f, settled.schedulerAlpha)
        assertEquals(0f, settled.answerTranslationFraction)
    }

    @Test
    fun `Study consumes theme motion without changing layout or rating feedback contract`() {
        val screen = source("ui/study/StudyScreen.kt")
        val answer = source("ui/study/FocusedAnswerSurface.kt")

        assertTrue(screen.contains("LETheme.motion.revealDuration"))
        assertTrue(screen.contains("LETheme.motion.easingDecelerate"))
        assertTrue(screen.contains("StudyMicroInteractionResolver.reveal"))
        assertTrue(screen.contains("subtleInteractionMotion = true"))
        assertTrue(answer.contains("revealVisual.meaningAlpha"))
        assertTrue(answer.containsCodeIgnoringWhitespace("revealVisual.schedulerAlpha"))
        assertTrue(screen.contains("LETheme.motion.ratingDuration"))
        assertTrue(screen.contains("RatingFeedbackPhase.ACTIVATED -> 0.96f"))
        assertTrue(screen.contains("RatingFeedbackPhase.CONFIRMED -> 1.03f"))
        assertTrue(screen.contains(".height(visualLayout.ratingButtonHeightDp.dp)"))
    }

    @Test
    fun `polished Study buttons keep hover color stable and animate existing focus tokens`() {
        val button = source("ui/designsystem/components/base/LEButton.kt")

        assertTrue(button.contains("hovered && !subtleInteractionMotion"))
        assertTrue(button.contains("pressed && !subtleInteractionMotion"))
        assertTrue(button.contains("LETheme.motion.hoverDuration"))
        assertTrue(button.contains("if (emphasized) LETheme.borders.thick else style.focusWidth"))
        assertTrue(button.contains("LETheme.elevation.elevation1"))
        assertTrue(button.contains("0.98f"))
    }

    @Test
    fun `micro interaction implementation stays presentation-only and token-backed`() {
        val presentation = source("ui/study/StudyMicroInteractionPresentation.kt")
        val screen = source("ui/study/StudyScreen.kt")

        listOf("Scheduler", "FSRS", "StudyViewModel", "ReviewRating", "Repository")
            .forEach { forbidden -> assertTrue(!presentation.contains(forbidden)) }
        assertTrue(!screen.contains("delayMillis ="))
        assertTrue(!presentation.contains("durationMillis"))
    }

    private fun source(relative: String): String =
        Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/$relative"))
}
