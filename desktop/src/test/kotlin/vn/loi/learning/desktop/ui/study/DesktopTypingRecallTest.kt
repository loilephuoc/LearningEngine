package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.ExperienceSelectionReason
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.LearningExperienceCapabilities
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperienceOptions
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.session.model.SessionId

class DesktopTypingRecallTest {
    @Test
    fun `chooser is available only for an eligible plan`() {
        assertTrue(DesktopExperienceSelection.isTypingAvailable(plan(typing = true)))
        assertFalse(DesktopExperienceSelection.isTypingAvailable(plan(typing = false)))
        assertFalse(DesktopExperienceSelection.isTypingAvailable(null))
    }

    @Test
    fun `default stays ordinal-zero baseline and explicit typing uses engine result`() {
        val plan = plan(typing = true)

        val default = requireNotNull(
            DesktopExperienceSelection.select(plan, DesktopExperienceMode.DEFAULT, rotation(0))
        )
        val typing = requireNotNull(
            DesktopExperienceSelection.select(plan, DesktopExperienceMode.TYPING, rotation(0))
        )

        assertEquals(LearningExperienceKind.IMAGE_RECALL, default.selectedKind)
        assertEquals(ExperienceSelectionReason.ROUND_ROBIN, default.reason)
        assertEquals(LearningExperienceKind.TYPING_RECALL, typing.selectedKind)
        assertEquals(ExperienceSelectionReason.USER_CHOICE, typing.reason)
        assertEquals(plan.options.orderedKinds, typing.availableKinds)
    }

    @Test
    fun `typing request safely falls back to default when unavailable`() {
        val result = requireNotNull(
            DesktopExperienceSelection.select(
                plan(typing = false),
                DesktopExperienceMode.TYPING,
                rotation(0)
            )
        )

        assertEquals(LearningExperienceKind.IMAGE_RECALL, result.selectedKind)
        assertEquals(ExperienceSelectionReason.ROUND_ROBIN, result.reason)
        assertNull(
            DesktopExperienceSelection.select(
                null,
                DesktopExperienceMode.DEFAULT,
                rotation(0)
            )
        )
    }

    @Test
    fun `default rotates passive experiences and never selects typing`() {
        val plan = plan(typing = true)

        assertEquals(
            listOf(
                LearningExperienceKind.IMAGE_RECALL,
                LearningExperienceKind.LISTENING_RECALL,
                LearningExperienceKind.PROMPT_RECALL,
                LearningExperienceKind.IMAGE_RECALL
            ),
            (0L..3L).map { ordinal ->
                requireNotNull(
                    DesktopExperienceSelection.select(
                        plan,
                        DesktopExperienceMode.DEFAULT,
                        rotation(ordinal)
                    )
                ).selectedKind
            }
        )
    }

    @Test
    fun `typing then default restores current item automatic selection`() {
        val plan = plan(typing = true)
        val context = rotation(2)

        val typing =
            requireNotNull(
                DesktopExperienceSelection.select(
                    plan,
                    DesktopExperienceMode.TYPING,
                    context
                )
            )
        val restored =
            requireNotNull(
                DesktopExperienceSelection.select(
                    plan,
                    DesktopExperienceMode.DEFAULT,
                    context
                )
            )

        assertEquals(LearningExperienceKind.TYPING_RECALL, typing.selectedKind)
        assertEquals(LearningExperienceKind.PROMPT_RECALL, restored.selectedKind)
        assertEquals(ExperienceSelectionReason.ROUND_ROBIN, restored.reason)
    }

    @Test
    fun `interaction resets on real item identity and isolates switched-away input`() {
        val first = TypingRecallUiState(itemId = "item-1", input = "draft")

        assertEquals(
            TypingRecallUiState(itemId = "item-2"),
            TypingRecallInteraction.initial("item-2")
        )
        assertEquals(
            TypingRecallUiState(itemId = "item-1"),
            TypingRecallInteraction.initial(first.itemId)
        )
    }

    @Test
    fun `empty and whitespace submissions do not complete or reveal`() {
        val evaluator = TypingAnswerEvaluator()
        val prompt = TypingRecallPrompt("Answer")
        val empty = requireNotNull(
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item"),
                prompt,
                evaluator
            )
        )
        val whitespace = requireNotNull(
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item", input = " \n\t "),
                prompt,
                evaluator
            )
        )

        assertEquals(TypingAnswerEvaluationStatus.EMPTY, empty.state.evaluation?.status)
        assertFalse(empty.shouldRevealAnswer)
        assertEquals(empty, TypingRecallInteraction.submit(empty.state, prompt, evaluator))
        assertEquals(TypingAnswerEvaluationStatus.EMPTY, whitespace.state.evaluation?.status)
        assertFalse(whitespace.shouldRevealAnswer)
    }

    @Test
    fun `typing after empty clears feedback and can complete a correct attempt`() {
        val evaluator = TypingAnswerEvaluator()
        val prompt = TypingRecallPrompt("Answer")
        val empty = requireNotNull(
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item"),
                prompt,
                evaluator
            )
        )

        val updated = TypingRecallInteraction.updateInput(empty.state, " answer ")
        val correct = requireNotNull(
            TypingRecallInteraction.submit(updated, prompt, evaluator)
        )

        assertNull(updated.evaluation)
        assertEquals(TypingAnswerEvaluationStatus.CORRECT, correct.state.evaluation?.status)
        assertTrue(correct.shouldRevealAnswer)
    }

    @Test
    fun `incorrect non-empty submission completes and reveals`() {
        val outcome = requireNotNull(
            TypingRecallInteraction.submit(
                TypingRecallUiState(itemId = "item", input = "different"),
                TypingRecallPrompt("Answer"),
                TypingAnswerEvaluator()
            )
        )

        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, outcome.state.evaluation?.status)
        assertTrue(outcome.shouldRevealAnswer)
    }

    @Test
    fun `completed attempt reveals only once and busy action blocks submission`() {
        val prompt = TypingRecallPrompt("Answer")
        val evaluator = TypingAnswerEvaluator()
        val state = TypingRecallUiState(itemId = "item", input = "Answer")
        var revealCount = 0

        val first = requireNotNull(
            TypingRecallInteraction.submit(state, prompt, evaluator)
        )
        if (first.shouldRevealAnswer) revealCount++
        val repeated = TypingRecallInteraction.submit(first.state, prompt, evaluator)
        if (repeated?.shouldRevealAnswer == true) revealCount++

        assertEquals(1, revealCount)
        assertNull(repeated)
        assertNull(
            TypingRecallInteraction.submit(
                state,
                prompt,
                evaluator,
                actionInProgress = true
            )
        )
    }

    private fun plan(typing: Boolean): LearningExperiencePlan {
        val kinds = buildList {
            add(LearningExperienceKind.IMAGE_RECALL)
            add(LearningExperienceKind.LISTENING_RECALL)
            add(LearningExperienceKind.PROMPT_RECALL)
            if (typing) add(LearningExperienceKind.TYPING_RECALL)
        }
        return LearningExperiencePlan(
            options = LearningExperienceOptions.from(kinds),
            capabilities = LearningExperienceCapabilities(
                hasPromptText = true,
                hasPromptImage = true,
                hasPromptAudio = true,
                hasMeaning = typing,
                hasExample = false,
                hasAnswerAudio = false,
                hasExampleAudio = false
            ),
            context = LearningExperienceContext(answerRevealed = false),
            visibleSupportingRoles = emptySet(),
            typingPrompt = if (typing) TypingRecallPrompt("Answer") else null
        )
    }

    private fun rotation(ordinal: Long) =
        ExperienceRotationContext(
            sessionId = SessionId("session"),
            learningItemId = LearningItemId("item"),
            ordinal = ordinal
        )
}
