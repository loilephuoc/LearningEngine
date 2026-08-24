package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.android.study.design.*
import vn.loi.learning.domain.study.recall.RecallChoice
import vn.loi.learning.domain.study.recall.RecallOutcome

class MultipleChoicePresentationPolicyTest {
    private val choices = listOf(
        RecallChoice("a", "Alpha", false),
        RecallChoice("b", "Beta", true),
        RecallChoice("c", "Gamma", false)
    )

    @Test
    fun `idle and pending selection remain neutral about correctness`() {
        val idle = resolveChoicePresentations(choices, null, false, null)
        assertTrue(idle.all { it.visualState == StudyChoiceVisualState.IDLE && it.enabled })

        val selected = resolveChoicePresentations(choices, "a", false, null)
        assertEquals(StudyChoiceVisualState.SELECTED, selected[0].visualState)
        assertTrue(selected.drop(1).all { it.visualState == StudyChoiceVisualState.IDLE })
        assertTrue(selected.none { it.enabled })
    }

    @Test
    fun `completed correct selection identifies correct tile`() {
        val result = resolveChoicePresentations(choices, "b", true, RecallOutcome.CORRECT)
        assertEquals(StudyChoiceVisualState.CORRECT, result[1].visualState)
        assertTrue(result.filterIndexed { index, _ -> index != 1 }.all {
            it.visualState == StudyChoiceVisualState.DISABLED
        })
        assertTrue(result.none { it.enabled })
    }

    @Test
    fun `incorrect selection identifies both wrong and expected choice`() {
        val result = resolveChoicePresentations(choices, "a", true, RecallOutcome.INCORRECT)
        assertEquals(StudyChoiceVisualState.INCORRECT, result[0].visualState)
        assertEquals(StudyChoiceVisualState.CORRECT, result[1].visualState)
        assertEquals(StudyChoiceVisualState.DISABLED, result[2].visualState)
    }

    @Test
    fun `choice count and content drive density while media remains supporting`() {
        assertEquals(
            StudyContentDensity.RELAXED,
            resolveMultipleChoiceDensity(StudyContentDensity.RELAXED, 3, 60, false)
        )
        assertEquals(
            StudyContentDensity.DENSE,
            resolveMultipleChoiceDensity(StudyContentDensity.RELAXED, 5, 60, false)
        )
        assertEquals(
            StudyContentDensity.DENSE,
            resolveMultipleChoiceDensity(StudyContentDensity.STANDARD, 4, 190, true)
        )
        assertEquals(StudyMediaRole.SUPPORTING, multipleChoiceMediaRole())
        assertEquals(120, multipleChoiceMotionDurationMillis(StudyChoiceVisualState.IDLE, false))
        assertEquals(140, multipleChoiceMotionDurationMillis(StudyChoiceVisualState.SELECTED, false))
        assertEquals(160, multipleChoiceMotionDurationMillis(StudyChoiceVisualState.CORRECT, false))
        assertEquals(160, multipleChoiceMotionDurationMillis(StudyChoiceVisualState.INCORRECT, false))
        assertEquals(0, multipleChoiceMotionDurationMillis(StudyChoiceVisualState.SELECTED, true))
    }

    @Test
    fun `choice anchors preserve stable order beyond four items`() {
        val many = (0..26).map { RecallChoice("id-$it", "Choice $it", it == 0) }
        val result = resolveChoicePresentations(many, null, false, null)
        assertEquals("A", result.first().anchor)
        assertEquals("Z", result[25].anchor)
        assertEquals("27", result[26].anchor)
        assertFalse(result.any { it.text.isBlank() })
    }

    @Test
    fun `completion policy separates correct fast path from wrong full answer`() {
        assertEquals(
            MultipleChoiceCompletionPath.QUESTION,
            multipleChoiceCompletionPath(false, null, wrongRevealReady = false)
        )
        assertEquals(
            MultipleChoiceCompletionPath.CORRECT_FAST_ADVANCE,
            multipleChoiceCompletionPath(true, RecallOutcome.CORRECT, wrongRevealReady = false)
        )
        assertEquals(
            MultipleChoiceCompletionPath.WRONG_OPTION_FEEDBACK,
            multipleChoiceCompletionPath(true, RecallOutcome.INCORRECT, wrongRevealReady = false)
        )
        assertEquals(
            MultipleChoiceCompletionPath.WRONG_FULL_ANSWER,
            multipleChoiceCompletionPath(true, RecallOutcome.INCORRECT, wrongRevealReady = true)
        )
        assertFalse(multipleChoiceAllowsManualRating())
        assertTrue(MULTIPLE_CHOICE_MINIMUM_FEEDBACK_MILLIS in 250L..500L)
        assertTrue(shouldAutoplayMultipleChoiceQuestion(completed = false, audioPath = "question.mp3"))
        assertFalse(shouldAutoplayMultipleChoiceQuestion(completed = false, audioPath = null))
        assertFalse(shouldAutoplayMultipleChoiceQuestion(completed = false, audioPath = ""))
        assertFalse(shouldAutoplayMultipleChoiceQuestion(completed = true, audioPath = "question.mp3"))
    }
}
