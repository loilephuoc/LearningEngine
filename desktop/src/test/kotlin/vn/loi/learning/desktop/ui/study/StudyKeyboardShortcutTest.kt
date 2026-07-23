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
    fun `enter and space retry while a recoverable error is shown`() {
        val state =
            StudyUiState(
                hasActiveSession = true,
                canReview = true,
                loadError = "Repair persisted data and retry."
            )

        assertEquals(
            StudyKeyboardAction.RETRY_LOAD,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardKey.ENTER
            )
        )
        assertEquals(
            StudyKeyboardAction.RETRY_LOAD,
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

    @Test
    fun `keyboard routing follows explicit workspace state over legacy booleans`() {
        val state = StudyUiState(
            hasActiveSession = true,
            canRevealAnswer = true,
            workspaceState = ReviewWorkspaceState.AnswerRevealed
        )

        assertEquals(
            StudyKeyboardAction.REVIEW_GOOD,
            resolveStudyKeyboardAction(state, StudyKeyboardKey.THREE)
        )
        assertNull(resolveStudyKeyboardAction(state, StudyKeyboardKey.SPACE))
    }

    @Test
    fun `control z is available only with an undo checkpoint`() {
        val undoable = StudyUiState(
            hasActiveSession = true,
            canRevealAnswer = true,
            canUndo = true
        )
        assertEquals(
            StudyKeyboardAction.UNDO_LATEST,
            resolveStudyKeyboardAction(
                undoable,
                StudyKeyboardInput(StudyKeyboardKey.Z, controlPressed = true)
            )
        )
        assertNull(
            resolveStudyKeyboardAction(
                undoable.copy(canUndo = false),
                StudyKeyboardInput(StudyKeyboardKey.Z, controlPressed = true)
            )
        )
    }

    @Test
    fun `escape pauses only an active workspace`() {
        assertEquals(
            StudyKeyboardAction.PAUSE_WORKSPACE,
            resolveStudyKeyboardAction(
                StudyUiState(hasActiveSession = true, canReview = true),
                StudyKeyboardInput(StudyKeyboardKey.ESCAPE)
            )
        )
        assertNull(resolveStudyKeyboardAction(StudyUiState(), StudyKeyboardInput(StudyKeyboardKey.ESCAPE)))
    }

    @Test
    fun `r replays primary audio only for an active item that has prompt audio`() {
        val audio = vn.loi.learning.application.learningcontent.LearningContentBlock.Audio(
            requireNotNull(vn.loi.learning.application.learningcontent.LocalLearningAssetReference.from("audio/prompt.mp3"))
        )
        val content = vn.loi.learning.application.learningcontent.LearningContent(
            vn.loi.learning.application.learningcontent.LearningContentSection(
                listOf(
                    vn.loi.learning.application.learningcontent.LearningContentBlock.Text(
                        "Question",
                        vn.loi.learning.domain.content.model.ContentTextFormat.PLAIN_TEXT
                    ),
                    audio
                )
            ),
            vn.loi.learning.application.learningcontent.LearningContentSection(
                listOf(vn.loi.learning.application.learningcontent.LearningContentBlock.UnavailableAnswer)
            )
        )
        val state = StudyUiState(
            hasActiveSession = true,
            canRevealAnswer = true,
            learningContent = content
        )

        assertEquals(
            StudyKeyboardAction.REPLAY_PRIMARY_AUDIO,
            resolveStudyKeyboardAction(state, StudyKeyboardInput(StudyKeyboardKey.R))
        )
        assertNull(
            resolveStudyKeyboardAction(
                state.copy(learningContent = content.copy(question = vn.loi.learning.application.learningcontent.LearningContentSection(listOf(content.question.blocks.first())))),
                StudyKeyboardInput(StudyKeyboardKey.R)
            )
        )
    }

    @Test
    fun `busy repeated and text input shortcuts are suppressed`() {
        val state = StudyUiState(hasActiveSession = true, canReview = true)
        assertNull(resolveStudyKeyboardAction(state.copy(actionInProgress = true), StudyKeyboardInput(StudyKeyboardKey.THREE)))
        assertNull(resolveStudyKeyboardAction(state, StudyKeyboardInput(StudyKeyboardKey.THREE, repeated = true)))
        assertNull(resolveStudyKeyboardAction(state, StudyKeyboardInput(StudyKeyboardKey.THREE, textInputFocused = true)))
        assertNull(resolveStudyKeyboardAction(state, StudyKeyboardInput(StudyKeyboardKey.R, textInputFocused = true)))
        assertEquals(
            StudyKeyboardAction.PAUSE_WORKSPACE,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardInput(StudyKeyboardKey.ESCAPE, textInputFocused = true)
            )
        )
    }
}
