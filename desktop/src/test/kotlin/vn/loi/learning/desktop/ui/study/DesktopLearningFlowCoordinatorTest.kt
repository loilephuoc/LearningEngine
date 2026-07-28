package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.application.learningcontent.LearningContentSection
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.domain.content.model.ContentTextFormat
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.desktop.shortcut.DesktopKeyChord
import vn.loi.learning.desktop.shortcut.DesktopShortcutKey

class DesktopLearningFlowCoordinatorTest {
    @Test
    fun `new item initializes stable rotated flow and recomputation retains stage`() {
        val coordinator = DesktopLearningFlowCoordinator()
        val initial = coordinator.synchronize(question(ordinal = 1))
        val repeated = coordinator.synchronize(initial)

        assertEquals(
            LearningExperienceKind.LISTENING_RECALL,
            initial.learningFlowSelection?.selectedKind
        )
        assertEquals(initial.learningFlowDefinition, repeated.learningFlowDefinition)
        assertEquals(initial.learningFlowState, repeated.learningFlowState)
    }

    @Test
    fun `continue advances to typing then requests reveal exactly once`() {
        val coordinator = DesktopLearningFlowCoordinator()
        val initial = coordinator.synchronize(question())

        val (typing, firstReveal) = coordinator.completeCurrent(initial)
        assertFalse(firstReveal)
        assertEquals(
            LearningExperienceKind.TYPING_RECALL,
            typing.learningFlowSelection?.selectedKind
        )

        val (pending, reveal) = coordinator.completeCurrent(typing)
        assertTrue(reveal)
        assertIs<LearningFlowStage.AnswerReveal>(pending.learningFlowCurrentStage)

        val (unchanged, duplicateReveal) = coordinator.completeCurrent(pending)
        assertFalse(duplicateReveal)
        assertEquals(pending.learningFlowState, unchanged.learningFlowState)
    }

    @Test
    fun `empty typing does not touch coordinator and authoritative reveal reaches rating ready`() {
        val coordinator = DesktopLearningFlowCoordinator()
        val initial = coordinator.synchronize(question())
        val typing = coordinator.completeCurrent(initial).first
        val empty =
            requireNotNull(
                TypingRecallInteraction.submit(
                    TypingRecallUiState(itemId = "item"),
                    requireNotNull(typing.learningExperiencePlan?.typingPrompt),
                    vn.loi.learning.application.learningexperience.TypingAnswerEvaluator()
                )
            )

        assertFalse(empty.shouldRevealAnswer)
        assertEquals(typing.learningFlowState, coordinator.synchronize(typing).learningFlowState)

        val pending = coordinator.completeCurrent(typing).first
        val ready =
            coordinator.synchronize(
                pending.copy(
                    canRevealAnswer = false,
                    canReview = true,
                    workspaceState = ReviewWorkspaceState.AnswerRevealed
                )
            )
        assertTrue(ready.learningFlowProgress?.isRatingReady == true)
        assertIs<LearningFlowStage.RatingReady>(ready.learningFlowCurrentStage)
    }

    @Test
    fun `item and session identity reset flow without cross-item leakage`() {
        val coordinator = DesktopLearningFlowCoordinator()
        val first = coordinator.synchronize(question())
        val advanced = coordinator.completeCurrent(first).first
        val next =
            coordinator.synchronize(
                question(
                    session = "session-2",
                    item = "item-2",
                    ordinal = 0
                )
            )

        assertTrue(advanced.learningFlowState?.completedStageIds?.isNotEmpty() == true)
        assertTrue(next.learningFlowState?.completedStageIds?.isEmpty() == true)
        assertEquals("item-2", next.experienceRotationContext?.learningItemId?.value)
    }

    @Test
    fun `already revealed recovery reconstructs safe rating-ready state`() {
        val ready =
            DesktopLearningFlowCoordinator().synchronize(
                question().copy(
                    canRevealAnswer = false,
                    canReview = true,
                    workspaceState = ReviewWorkspaceState.AnswerRevealed
                )
            )

        assertTrue(ready.learningFlowProgress?.isRatingReady == true)
    }

    @Test
    fun `authoritative direct reveal on the current flow initializes normal rating phase`() {
        val coordinator = DesktopLearningFlowCoordinator()
        val introductionFront = coordinator.synchronize(question())
        val answer =
            coordinator.synchronize(
                introductionFront.copy(
                    canRevealAnswer = false,
                    canReview = true,
                    contentIntroductionState = ContentIntroductionState.COMPLETED,
                    workspaceState = ReviewWorkspaceState.AnswerRevealed
                )
            )

        assertTrue(answer.canReview)
        assertIs<ReviewWorkspaceState.AnswerRevealed>(answer.workspaceState)
        assertIs<LearningFlowStage.RatingReady>(answer.learningFlowCurrentStage)
        assertTrue(answer.learningFlowProgress?.isRatingReady == true)
        assertEquals(StudyActionDockMode.ANSWER_ACTIONS, resolveStudyActionDockMode(answer))
        assertEquals(4, studyRatingOrder.size)
        assertEquals(
            StudyKeyboardAction.REVIEW_GOOD,
            resolveStudyKeyboardAction(
                answer,
                StudyKeyboardInput(DesktopKeyChord(DesktopShortcutKey.THREE))
            )
        )
    }

    @Test
    fun `direct reveal rating phase is stable across refresh and action in progress`() {
        val coordinator = DesktopLearningFlowCoordinator()
        val front = coordinator.synchronize(question())
        val answer = coordinator.synchronize(
            front.copy(
                canRevealAnswer = false,
                canReview = true,
                actionInProgress = true,
                workspaceState = ReviewWorkspaceState.AnswerRevealed
            )
        )
        val refreshed = coordinator.synchronize(answer)

        assertTrue(answer.learningFlowProgress?.isRatingReady == true)
        assertEquals(StudyActionDockMode.ANSWER_ACTIONS, resolveStudyActionDockMode(answer))
        assertEquals(answer.learningFlowState, refreshed.learningFlowState)
        assertTrue(refreshed.actionInProgress)
    }

    private fun question(
        session: String = "session",
        item: String = "item",
        ordinal: Long = 0
    ) =
        StudyUiState(
            hasActiveSession = true,
            canRevealAnswer = true,
            currentLearningItemId = item,
            experienceRotationContext =
                ExperienceRotationContext(
                    SessionId(session),
                    LearningItemId(item),
                    ordinal
                ),
            learningContent =
                LearningContent(
                    question =
                        LearningContentSection(
                            listOf(
                                LearningContentBlock.Text(
                                    "question",
                                    ContentTextFormat.PLAIN_TEXT,
                                    vn.loi.learning.application.learningcontent.LearningTextRole.PRIMARY_ENGLISH
                                ),
                                LearningContentBlock.Image(
                                    requireNotNull(
                                        vn.loi.learning.application.learningcontent
                                            .LocalLearningAssetReference.from("image.png")
                                    )
                                ),
                                LearningContentBlock.Audio(
                                    requireNotNull(
                                        vn.loi.learning.application.learningcontent
                                            .LocalLearningAssetReference.from("audio.mp3")
                                    )
                                )
                            )
                        ),
                    answer =
                        LearningContentSection(
                            listOf(
                                LearningContentBlock.Text(
                                    "answer",
                                    ContentTextFormat.PLAIN_TEXT,
                                    vn.loi.learning.application.learningcontent.LearningTextRole.VIETNAMESE_MEANING
                                )
                            )
                        )
                ),
            workspaceState = ReviewWorkspaceState.Question
        )
}
