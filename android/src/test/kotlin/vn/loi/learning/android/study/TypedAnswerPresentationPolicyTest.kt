package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.android.study.design.*
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.study.recall.RecallOutcome

class TypedAnswerPresentationPolicyTest {
    @Test fun `input visual maps idle focused correct incorrect and disabled`() {
        assertEquals(StudyInputVisualState.IDLE, resolveStudyInputVisualState(true, false))
        assertEquals(StudyInputVisualState.FOCUSED, resolveStudyInputVisualState(true, true))
        assertEquals(StudyInputVisualState.CORRECT, resolveStudyInputVisualState(false, false, TypingAnswerEvaluationStatus.CORRECT))
        assertEquals(StudyInputVisualState.INCORRECT, resolveStudyInputVisualState(true, false, TypingAnswerEvaluationStatus.INCORRECT))
        assertEquals(StudyInputVisualState.DISABLED, resolveStudyInputVisualState(false, false))
        assertEquals(StudyInputVisualState.CORRECT, resolveStudyInputVisualState(false, false, outcome = RecallOutcome.CORRECT))
        assertEquals(StudyInputVisualState.INCORRECT, resolveStudyInputVisualState(false, false, outcome = RecallOutcome.INCORRECT))
    }

    @Test fun `typed density prioritizes IME and typing media uses adaptive standard bounds`() {
        assertEquals(StudyContentDensity.DENSE, resolveTypedModeDensity(StudyContentDensity.RELAXED, true, true, 20, false))
        assertEquals(StudyContentDensity.RELAXED, resolveTypedModeDensity(StudyContentDensity.RELAXED, false, false, 30, false))
        assertEquals(StudyContentDensity.DENSE, resolveTypedModeDensity(StudyContentDensity.STANDARD, false, true, 220, true))
        assertEquals(StudyMediaRole.STANDARD, typedModeMediaRole(listening = false, feedbackVisible = false))
        assertEquals(StudyMediaRole.COMPACT, typedModeMediaRole(listening = true, feedbackVisible = false))
        assertEquals(StudyMediaRole.STANDARD, typedModeMediaRole(listening = false, feedbackVisible = true))
        assertEquals(
            StudyMediaRole.SUPPORTING,
            typedModeMediaRole(listening = false, feedbackVisible = false, imeVisible = true)
        )
    }

    @Test fun `feedback and motion share foundation semantics`() {
        assertEquals(StudyFeedbackVisualState.CORRECT, StudyInputVisualState.CORRECT.feedbackVisual())
        assertEquals(StudyFeedbackVisualState.INCORRECT, StudyInputVisualState.INCORRECT.feedbackVisual())
        assertEquals(StudyFeedbackVisualState.NEUTRAL, StudyInputVisualState.FOCUSED.feedbackVisual())
        assertEquals(StudyFeedbackVisualState.NEUTRAL, StudyInputVisualState.DISABLED.feedbackVisual())
        assertEquals(160, studyMotionDurationMillis(StudyMotionRole.SELECTION, false))
        assertEquals(0, studyMotionDurationMillis(StudyMotionRole.AUDIO_PULSE, true))
    }

    @Test fun `Typing success requires concurrent audio and visual gates`() {
        assertEquals(450L, AndroidTypingSuccessPresentationPolicy.minimumDwellMillis)
        assertFalse(typingSuccessReady(true, audioCompleted = true, dwellCompleted = false))
        assertFalse(typingSuccessReady(true, audioCompleted = false, dwellCompleted = true))
        assertTrue(typingSuccessReady(true, audioCompleted = true, dwellCompleted = true))
        assertFalse(typingSuccessReady(false, audioCompleted = true, dwellCompleted = true))
    }
}
