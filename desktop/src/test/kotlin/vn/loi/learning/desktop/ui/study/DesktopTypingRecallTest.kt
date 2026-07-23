package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.ExperienceSelectionReason
import vn.loi.learning.application.learningexperience.LearningExperienceCapabilities
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperienceOptions
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt

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
            DesktopExperienceSelection.select(plan, DesktopExperienceMode.DEFAULT)
        )
        val typing = requireNotNull(
            DesktopExperienceSelection.select(plan, DesktopExperienceMode.TYPING)
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
                DesktopExperienceMode.TYPING
            )
        )

        assertEquals(LearningExperienceKind.IMAGE_RECALL, result.selectedKind)
        assertEquals(ExperienceSelectionReason.ROUND_ROBIN, result.reason)
        assertNull(DesktopExperienceSelection.select(null, DesktopExperienceMode.DEFAULT))
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
    fun `submit evaluates empty correct and incorrect input deterministically`() {
        val evaluator = TypingAnswerEvaluator()
        val prompt = TypingRecallPrompt("Answer")
        val empty = TypingRecallInteraction.submit(
            TypingRecallUiState(itemId = "item"),
            prompt,
            evaluator
        )
        val correct = TypingRecallInteraction.submit(
            TypingRecallInteraction.updateInput(empty, " answer "),
            prompt,
            evaluator
        )
        val incorrect = TypingRecallInteraction.submit(
            TypingRecallInteraction.updateInput(correct, "different"),
            prompt,
            evaluator
        )

        assertEquals(TypingAnswerEvaluationStatus.EMPTY, empty.evaluation?.status)
        assertEquals(TypingAnswerEvaluationStatus.CORRECT, correct.evaluation?.status)
        assertEquals(TypingAnswerEvaluationStatus.INCORRECT, incorrect.evaluation?.status)
        assertEquals(
            incorrect,
            TypingRecallInteraction.submit(incorrect, prompt, evaluator)
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
}
