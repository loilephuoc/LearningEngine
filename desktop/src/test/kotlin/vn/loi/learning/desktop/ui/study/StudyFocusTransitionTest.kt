package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals

class StudyFocusTransitionTest {
    @Test
    fun `idle state resolves to idle focus phase`() {
        val key =
            resolveStudyFocusTransitionKey(
                StudyUiState()
            )

        assertEquals(StudyFocusPhase.IDLE, key.phase)
        assertEquals(0, key.reviewedCount)
        assertEquals(0, key.currentItemPosition)
    }

    @Test
    fun `question and revealed answer use distinct focus phases`() {
        val question =
            resolveStudyFocusTransitionKey(
                StudyUiState(
                    hasActiveSession = true,
                    canRevealAnswer = true,
                    currentItemPosition = 2
                )
            )
        val answer =
            resolveStudyFocusTransitionKey(
                StudyUiState(
                    hasActiveSession = true,
                    canReview = true,
                    currentItemPosition = 2
                )
            )

        assertEquals(StudyFocusPhase.QUESTION, question.phase)
        assertEquals(StudyFocusPhase.ANSWER_REVEALED, answer.phase)
    }

    @Test
    fun `grading the next item changes the transition key`() {
        val beforeGrade =
            resolveStudyFocusTransitionKey(
                StudyUiState(
                    hasActiveSession = true,
                    canReview = true,
                    reviewedCount = 0,
                    currentItemPosition = 1
                )
            )
        val nextQuestion =
            resolveStudyFocusTransitionKey(
                StudyUiState(
                    hasActiveSession = true,
                    canRevealAnswer = true,
                    reviewedCount = 1,
                    currentItemPosition = 2
                )
            )

        assertEquals(StudyFocusPhase.ANSWER_REVEALED, beforeGrade.phase)
        assertEquals(StudyFocusPhase.QUESTION, nextQuestion.phase)
        assertEquals(1, nextQuestion.reviewedCount)
        assertEquals(2, nextQuestion.currentItemPosition)
    }

    @Test
    fun `recoverable error has focus priority and normalized identity`() {
        val key =
            resolveStudyFocusTransitionKey(
                StudyUiState(
                    hasActiveSession = true,
                    canReview = true,
                    reviewedCount = 3,
                    loadError = "  Persisted queue is invalid.  "
                )
            )

        assertEquals(StudyFocusPhase.ERROR, key.phase)
        assertEquals("Persisted queue is invalid.", key.loadError)
    }

    @Test
    fun `completion resolves to completed focus phase`() {
        val key =
            resolveStudyFocusTransitionKey(
                StudyUiState(
                    sessionCompleted = true,
                    reviewedCount = 4,
                    currentItemPosition = 4
                )
            )

        assertEquals(StudyFocusPhase.COMPLETED, key.phase)
    }
}
