package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StudyKeyboardShortcutTest {

    @Test
    fun `enter and space start study when no session is active`() {
        val state = StudyUiState()

        assertEquals(
            StudyKeyboardAction.START_STUDY,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.ENTER
            )
        )
        assertEquals(
            StudyKeyboardAction.START_STUDY,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.SPACE
            )
        )
    }

    @Test
    fun `enter and space reveal an active hidden answer`() {
        val state =
            StudyUiState(
                hasActiveSession = true,
                canRevealAnswer = true
            )

        assertEquals(
            StudyKeyboardAction.REVEAL_ANSWER,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.ENTER
            )
        )
        assertEquals(
            StudyKeyboardAction.REVEAL_ANSWER,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.SPACE
            )
        )
        assertNull(
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.THREE
            )
        )
    }

    @Test
    fun `number keys grade only after the answer is revealed`() {
        val state =
            StudyUiState(
                hasActiveSession = true,
                canReview = true
            )

        assertEquals(
            StudyKeyboardAction.REVIEW_AGAIN,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.ONE
            )
        )
        assertEquals(
            StudyKeyboardAction.REVIEW_HARD,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.TWO
            )
        )
        assertEquals(
            StudyKeyboardAction.REVIEW_GOOD,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.THREE
            )
        )
        assertEquals(
            StudyKeyboardAction.REVIEW_EASY,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.FOUR
            )
        )
        assertNull(
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.SPACE
            )
        )
    }

    @Test
    fun `shortcuts do nothing while a recoverable error is shown`() {
        val state =
            StudyUiState(
                hasActiveSession = true,
                canReview = true,
                loadError = "Repair persisted data and retry."
            )

        StudyKeyboardKey.entries.forEach { key ->
            assertNull(
                resolveStudyKeyboardAction(
                    state,
                    key
                )
            )
        }
    }

    @Test
    fun `completed session accepts only start shortcuts`() {
        val state =
            StudyUiState(
                sessionCompleted = true,
                reviewedCount = 1,
                totalItems = 1
            )

        assertEquals(
            StudyKeyboardAction.START_STUDY,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.ENTER
            )
        )
        assertNull(
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.ONE
            )
        )
    }
}
