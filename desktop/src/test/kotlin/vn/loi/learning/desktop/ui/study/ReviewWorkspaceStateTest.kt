package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.ReviewRating

class ReviewWorkspaceStateTest {

    @Test
    fun `question permits only reveal and reveal permits ratings in domain order`() {
        assertEquals(
            setOf(ReviewWorkspaceAction.ShowAnswer),
            ReviewWorkspaceState.Question.allowedActions
        )
        assertEquals(
            ReviewRating.entries,
            ReviewWorkspaceState.AnswerRevealed.allowedActions
                .map { action -> (action as ReviewWorkspaceAction.Rate).rating }
        )
    }

    @Test
    fun `valid review path is deterministic through internal transition states`() {
        val preparing = ReviewWorkspaceStateMachine.dispatch(
            ReviewWorkspaceState.Idle,
            ReviewWorkspaceAction.Start
        )
        val question = ReviewWorkspaceStateMachine.projectNext(
            preparing,
            hasNextItem = true
        )
        val answer = ReviewWorkspaceStateMachine.dispatch(
            question,
            ReviewWorkspaceAction.ShowAnswer
        )
        val feedback = ReviewWorkspaceStateMachine.dispatch(
            answer,
            ReviewWorkspaceAction.Rate(ReviewRating.GOOD)
        )
        val transitioning = ReviewWorkspaceStateMachine.beginTransition(feedback)

        assertEquals(ReviewWorkspaceState.Preparing, preparing)
        assertEquals(ReviewWorkspaceState.Question, question)
        assertEquals(ReviewWorkspaceState.AnswerRevealed, answer)
        assertEquals(ReviewWorkspaceState.Feedback(ReviewRating.GOOD), feedback)
        assertEquals(ReviewWorkspaceState.Transitioning, transitioning)
        assertEquals(
            ReviewWorkspaceState.Completed,
            ReviewWorkspaceStateMachine.projectNext(transitioning, hasNextItem = false)
        )
    }

    @Test
    fun `forbidden reveal and rating transitions fail without changing state`() {
        assertFailsWith<IllegalArgumentException> {
            ReviewWorkspaceStateMachine.dispatch(
                ReviewWorkspaceState.Question,
                ReviewWorkspaceAction.Rate(ReviewRating.AGAIN)
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ReviewWorkspaceStateMachine.dispatch(
                ReviewWorkspaceState.AnswerRevealed,
                ReviewWorkspaceAction.ShowAnswer
            )
        }
    }

    @Test
    fun `legacy projection resolves contradictions with lifecycle priority`() {
        val recoveredAnswer = ReviewWorkspaceState.projectLegacy(
            hasActiveSession = true,
            canRevealAnswer = true,
            canReview = true,
            sessionCompleted = false,
            hasLoadError = false
        )
        val failed = ReviewWorkspaceState.projectLegacy(
            hasActiveSession = true,
            canRevealAnswer = true,
            canReview = false,
            sessionCompleted = false,
            hasLoadError = true
        )

        assertEquals(ReviewWorkspaceState.AnswerRevealed, recoveredAnswer)
        assertEquals(ReviewWorkspaceState.RecoverableFailure, failed)
        assertTrue(failed.allows(ReviewWorkspaceAction.Retry))
        assertFalse(failed.allows(ReviewWorkspaceAction.ShowAnswer))
    }
}
