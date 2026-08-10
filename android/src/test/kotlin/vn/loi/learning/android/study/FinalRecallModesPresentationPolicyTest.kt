package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.*
import vn.loi.learning.android.study.design.*
import vn.loi.learning.domain.study.recall.RecallOutcome

class FinalRecallModesPresentationPolicyTest {
    @Test fun `image recall is hero before feedback and shrinks after feedback or IME`() {
        assertEquals(StudyMediaRole.HERO, imageRecallMediaRole(false, false))
        assertEquals(StudyMediaRole.STANDARD, imageRecallMediaRole(true, false))
        assertEquals(StudyMediaRole.SUPPORTING, imageRecallMediaRole(false, true))
        assertEquals(StudyContentDensity.DENSE, resolveImageRecallDensity(StudyContentDensity.RELAXED, true, 10, false))
        assertEquals(StudyContentDensity.STANDARD, resolveImageRecallDensity(StudyContentDensity.RELAXED, false, 10, true))
    }

    @Test fun `image recall maps semantic feedback and reduced media motion`() {
        assertEquals(StudyFeedbackVisualState.NEUTRAL, imageRecallFeedback(null))
        assertEquals(StudyFeedbackVisualState.CORRECT, imageRecallFeedback(RecallOutcome.CORRECT))
        assertEquals(StudyFeedbackVisualState.INCORRECT, imageRecallFeedback(RecallOutcome.INCORRECT))
        assertEquals(180, imageRecallMediaMotionMillis(false))
        assertEquals(0, imageRecallMediaMotionMillis(true))
    }

    @Test fun `cloze preserves exact prefix answer and suffix`() {
        val result = resolveClozePresentation("I wrote a ", "letter", " to my friend.", null, null)
        assertEquals("I wrote a letter to my friend.", result.completedSentence)
        assertNull(result.clozeTranslation)
    }

    @Test fun `equivalent completed sentence suppresses duplicate example and keeps translation nearby`() {
        val result = resolveClozePresentation(
            "I wrote a ", "letter", " to my friend.",
            "  i WROTE   a letter to my friend. ", "Tôi đã viết thư cho bạn."
        )
        assertNull(result.supportingExample)
        assertNull(result.supportingExampleTranslation)
        assertEquals("Tôi đã viết thư cho bạn.", result.clozeTranslation)
    }

    @Test fun `nonduplicate support example remains available`() {
        val result = resolveClozePresentation(
            "I wrote a ", "letter", ".", "She sent a letter.", "Cô ấy đã gửi một lá thư."
        )
        assertNull(result.clozeTranslation)
        assertEquals("She sent a letter.", result.supportingExample)
        assertEquals("Cô ấy đã gửi một lá thư.", result.supportingExampleTranslation)
    }

    @Test fun `cloze density and feedback follow published state`() {
        assertEquals(StudyContentDensity.DENSE, resolveExampleCompletionDensity(StudyContentDensity.RELAXED, true, 20, 0))
        assertEquals(StudyContentDensity.DENSE, resolveExampleCompletionDensity(StudyContentDensity.STANDARD, false, 150, 40))
        assertEquals(StudyContentDensity.RELAXED, resolveExampleCompletionDensity(StudyContentDensity.RELAXED, false, 50, 0))
        assertEquals(StudyFeedbackVisualState.CORRECT, exampleCompletionFeedback(RecallOutcome.CORRECT))
        assertEquals(StudyFeedbackVisualState.INCORRECT, exampleCompletionFeedback(RecallOutcome.INCORRECT))
        assertEquals(160, clozeMotionDurationMillis(false))
        assertEquals(0, clozeMotionDurationMillis(true))
    }
}
