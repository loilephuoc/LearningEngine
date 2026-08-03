package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.ExperienceSelectionReason
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceCapabilities
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperienceOptions
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

class DesktopMultipleChoiceRuntimeTest {
    @Test
    fun `router selects multiple choice renderer only from RecallPlan mode`() {
        assertEquals(DesktopRecallRenderer.MULTIPLE_CHOICE, DesktopRecallModeRouter.route(plan(choices(2))))
        assertEquals(DesktopRecallRenderer.TYPING, DesktopRecallModeRouter.route(typingPlan()))
        assertEquals(DesktopRecallRenderer.UNSUPPORTED, DesktopRecallModeRouter.route(null))
    }

    @Test
    fun `resolver preserves two option identities labels and order`() {
        val source = choices(2)
        val presentation = requireNotNull(MultipleChoicePresentationResolver.resolve(plan(source)))

        assertEquals(source.map { it.id }, presentation.options.map { it.optionId })
        assertEquals(source.map { it.text }, presentation.options.map { it.text })
        assertEquals(listOf(1, 2), presentation.options.map { it.position })
    }

    @Test
    fun `resolver supports four options without reordering`() {
        val source = choices(4)
        assertEquals(
            source.map { it.id },
            requireNotNull(MultipleChoicePresentationResolver.resolve(plan(source))).options.map { it.optionId }
        )
    }

    @Test
    fun `number keys resolve only existing options`() {
        val options = requireNotNull(MultipleChoicePresentationResolver.resolve(plan(choices(2)))).options

        assertEquals("option-1", multipleChoiceOptionForNumberKey(1, options)?.optionId)
        assertEquals("option-2", multipleChoiceOptionForNumberKey(2, options)?.optionId)
        assertNull(multipleChoiceOptionForNumberKey(3, options))
        assertNull(multipleChoiceOptionForNumberKey(0, options))
    }

    @Test
    fun `submission gate accepts one typed option identity`() {
        val gate = MultipleChoiceSubmissionGate()

        assertTrue(gate.accept("option-2", setOf("option-1", "option-2")))
        assertEquals("option-2", gate.selectedOptionId)
    }

    @Test
    fun `submission gate rejects unknown identity without locking attempt`() {
        val gate = MultipleChoiceSubmissionGate()

        assertFalse(gate.accept("missing", setOf("option-1")))
        assertTrue(gate.accept("option-1", setOf("option-1")))
    }

    @Test
    fun `submission gate blocks double click and keyboard click race`() {
        val gate = MultipleChoiceSubmissionGate()
        val available = setOf("option-1", "option-2")

        assertTrue(gate.accept("option-1", available))
        assertFalse(gate.accept("option-1", available))
        assertFalse(gate.accept("option-2", available))
    }

    @Test
    fun `scene projector routes multiple choice plan without typing fallback`() {
        val experience = experiencePlan()
        val scene = DesktopLearningSceneProjector().project(
            experience,
            selection(experience),
            LearningContentPresentation(
                listOf(
                    PresentedLearningSection(
                        LearningSectionKind.QUESTION,
                        listOf(PresentedLearningBlock.Text(SafeMarkdownDocument.plain("question"), PresentedTextRole.PRIMARY_ENGLISH))
                    )
                )
            ),
            plan(choices(4))
        )

        assertIs<MultipleChoiceScene>(scene)
        assertEquals(SceneType.MULTIPLE_CHOICE, scene.type)
    }

    private fun choices(count: Int) = (1..count).map { index ->
        RecallChoice("option-$index", "Option $index long text can wrap", index == 1)
    }

    private fun plan(choices: List<RecallChoice>) = basePlan(
        mode = RecallMode.MULTIPLE_CHOICE,
        prompt = RecallPrompt.MultipleChoice("Choose the translation", choices),
        answerKind = RecallAnswerKind.CHOICE,
        requirements = RecallPlatformRequirements(requiresChoiceSelection = true)
    )

    private fun typingPlan() = basePlan(
        mode = RecallMode.TYPING,
        prompt = RecallPrompt.Typing("question"),
        answerKind = RecallAnswerKind.TEXT,
        requirements = RecallPlatformRequirements(requiresTextInput = true)
    )

    private fun basePlan(
        mode: RecallMode,
        prompt: RecallPrompt,
        answerKind: RecallAnswerKind,
        requirements: RecallPlatformRequirements
    ) = RecallPlan(
        planId = RecallPlanId("plan-${mode.wireId}"),
        learnerId = LearnerId("learner"),
        contentId = ContentId("content"),
        learningItemId = LearningItemId("item"),
        sessionId = SessionId("session"),
        mode = mode,
        direction = RecallDirection.SOURCE_TO_TARGET,
        prompt = prompt,
        answerContract = RecallAnswerContract(
            "Option 1 long text can wrap", normalizationPolicy = RecallNormalizationPolicyId("typing-v1"),
            caseSensitivity = CaseSensitivity.INSENSITIVE, punctuationPolicy = PunctuationPolicy.IGNORE,
            whitespacePolicy = WhitespacePolicy.NORMALIZE, expectedLanguage = RecallLanguageTag("vi"), kind = answerKind
        ),
        availableAssistance = emptySet(),
        evidenceClass = RecallEvidenceEligibility.STANDARD,
        deterministicSeed = RecallDeterministicSeed(1),
        generatedAt = Moment(1),
        provenance = RecallProvenance.EVALUATIVE,
        platformRequirements = requirements,
        contentCapabilities = RecallContentCapabilities(
            ContentId("content"), setOf(RecallCapability.SOURCE_TEXT, RecallCapability.TARGET_TRANSLATION)
        )
    )

    private fun experiencePlan() = LearningExperiencePlan(
        options = LearningExperienceOptions.from(listOf(LearningExperienceKind.PROMPT_RECALL)),
        capabilities = LearningExperienceCapabilities(true, false, false, true, false, false, false),
        context = LearningExperienceContext(false),
        visibleSupportingRoles = emptySet()
    )

    private fun selection(plan: LearningExperiencePlan) = ExperienceSelectionResult(
        LearningExperienceKind.PROMPT_RECALL,
        plan.options.orderedKinds,
        0,
        ExperienceSelectionReason.ROUND_ROBIN
    )
}
