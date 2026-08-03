package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.*
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

class DesktopExampleCompletionRuntimeTest {
    @Test
    fun `router selects Example Completion only from RecallPlan mode`() {
        assertEquals(
            DesktopRecallRenderer.EXAMPLE_COMPLETION,
            DesktopRecallModeRouter.route(plan("A ____ appears.", 2, 6))
        )
    }

    @Test
    fun `presentation uses exact plan span for target at start middle and end`() {
        assertSegments("____ appears.", 0, 4, "", "____", " appears.")
        assertSegments("A ____ appears.", 2, 6, "A ", "____", " appears.")
        assertSegments("Choose ____", 7, 11, "Choose ", "____", "")
    }

    @Test
    fun `presentation preserves multi word punctuation Unicode whitespace and selected occurrence`() {
        assertSegments("Use _________, now.", 4, 13, "Use ", "_________", ", now.")
        assertSegments("Tôi thấy ____!", 9, 13, "Tôi thấy ", "____", "!")
        assertSegments("____ then word then ____.", 20, 24, "____ then word then ", "____", ".")
        assertSegments("Line one\n  ____ remains.", 11, 15, "Line one\n  ", "____", " remains.")
    }

    @Test
    fun `invalid and answer bearing spans are typed unavailable without fake blank`() {
        assertEquals(
            ExampleCompletionUnavailableReason.INVALID_TARGET_SPAN,
            assertIs<ExampleCompletionPresentationResult.Unavailable>(
                ExampleCompletionPresentationResolver.resolve(plan("Short", 2, 12))
            ).reason
        )
        assertEquals(
            ExampleCompletionUnavailableReason.UNSAFE_BLANK,
            assertIs<ExampleCompletionPresentationResult.Unavailable>(
                ExampleCompletionPresentationResolver.resolve(plan("A word appears.", 2, 6))
            ).reason
        )
    }

    @Test
    fun `plan bound scene exposes no canonical target translation IPA POS or support`() {
        val experience = experiencePlan()
        val scene = assertIs<ExampleCompletionScene>(
            DesktopLearningSceneProjector().project(
                experience,
                selection(experience),
                LearningContentPresentation(
                    listOf(
                        PresentedLearningSection(
                            LearningSectionKind.QUESTION,
                            listOf(
                                PresentedLearningBlock.Text(
                                    SafeMarkdownDocument.plain("canonical target IPA POS"),
                                    PresentedTextRole.PRIMARY_ENGLISH
                                )
                            )
                        ),
                        PresentedLearningSection(
                            LearningSectionKind.ANSWER,
                            listOf(
                                PresentedLearningBlock.Text(
                                    SafeMarkdownDocument.plain("translation"),
                                    PresentedTextRole.VIETNAMESE_MEANING
                                )
                            )
                        )
                    )
                ),
                plan("A ____ appears.", 2, 6)
            )
        )

        assertTrue(scene.blocks.isEmpty())
        assertTrue(scene.supportingScenes.isEmpty())
        assertIs<ExampleCompletionPresentationResult.Ready>(scene.presentation)
    }

    @Test
    fun `resolver never searches replaces or reads canonical answer`() {
        val source = source("DesktopExampleCompletionRuntime.kt")
            .substringAfter("object ExampleCompletionPresentationResolver")
            .substringBefore("class ExampleCompletionSubmissionGate")

        assertFalse(source.contains("replace("))
        assertFalse(source.contains("indexOf("))
        assertFalse(source.contains("contains("))
        assertFalse(source.contains("canonicalAnswer"))
        assertFalse(source.contains("answerContract"))
    }

    @Test
    fun `submission gate rejects unavailable and blank then accepts only one raw attempt`() {
        val gate = ExampleCompletionSubmissionGate()

        assertFalse(gate.accept("answer", promptAvailable = false))
        assertFalse(gate.accept("  ", promptAvailable = true))
        assertTrue(gate.accept("  Raw Café  ", promptAvailable = true))
        assertFalse(gate.accept("second", promptAvailable = true))
    }

    @Test
    fun `Desktop submission uses raw TypedText and delegates execution and learning`() {
        val source = source("StudyFacade.kt")
            .substringAfter("fun submitExampleCompletion(")
            .substringBefore("private fun recallStrategyContext")

        assertTrue(source.contains("RecallSubmission.TypedText("))
        assertTrue(source.contains("text = rawInput"))
        assertTrue(source.contains("engine.executeRecall("))
        assertTrue(source.contains("engine.executeRecallLearning("))
        assertFalse(source.contains("isCorrect"))
        assertFalse(source.contains("normalize"))
        assertFalse(source.contains("ReviewRating."))
        assertFalse(source.contains("targetSpan"))
    }

    @Test
    fun `context prompt accessibility names blank without speaking target or underscore count`() {
        val screen = source("StudyScreen.kt")

        assertTrue(screen.contains("prompt.prefix + strings.exampleCompletionMissingWord + prompt.suffix"))
        assertTrue(screen.contains("TextDecoration.Underline"))
        assertTrue(screen.contains("background = LETheme.colors.accentSoft"))
        assertTrue(screen.contains("softWrap = true"))
        assertTrue(screen.contains(".fillMaxWidth()"))
        assertFalse(screen.substringAfter("private fun ExampleCompletionRecallPanel(")
            .substringBefore("private fun ImageRecallInputPanel(").contains("canonicalAnswer"))
    }

    @Test
    fun `Example Completion strings are localized in English and Vietnamese`() {
        val strings = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/localization/DesktopStrings.kt")
            .takeIf(File::isFile)
            ?: File("src/main/kotlin/vn/loi/learning/desktop/ui/localization/DesktopStrings.kt")
        val text = strings.readText()

        assertTrue(text.contains("exampleCompletionMissingWord = \"missing word\""))
        assertTrue(text.contains("exampleCompletionMissingWord = \"từ còn thiếu\""))
        assertTrue(text.contains("exampleCompletionInputLabel = \"Gõ từ còn thiếu\""))
    }

    private fun assertSegments(
        example: String,
        start: Int,
        end: Int,
        prefix: String,
        blank: String,
        suffix: String
    ) {
        val ready = assertIs<ExampleCompletionPresentationResult.Ready>(
            ExampleCompletionPresentationResolver.resolve(plan(example, start, end))
        )
        assertEquals(ExampleCompletionPromptPresentation(prefix, blank, suffix), ready.prompt)
    }

    private fun plan(example: String, start: Int, end: Int) = RecallPlan(
        planId = RecallPlanId("plan-example"),
        learnerId = LearnerId("learner"),
        contentId = ContentId("content"),
        learningItemId = LearningItemId("item"),
        sessionId = SessionId("session"),
        mode = RecallMode.EXAMPLE_COMPLETION,
        direction = RecallDirection.CONTEXT_TO_TEXT,
        prompt = RecallPrompt.ExampleCompletion(example, RecallTextSpan(start, end)),
        answerContract = RecallAnswerContract(
            canonicalAnswer = "hidden target",
            normalizationPolicy = RecallNormalizationPolicyId("typing-v1"),
            caseSensitivity = CaseSensitivity.INSENSITIVE,
            punctuationPolicy = PunctuationPolicy.IGNORE,
            whitespacePolicy = WhitespacePolicy.NORMALIZE,
            expectedLanguage = RecallLanguageTag("en"),
            kind = RecallAnswerKind.TEXT
        ),
        availableAssistance = emptySet(),
        evidenceClass = RecallEvidenceEligibility.STANDARD,
        deterministicSeed = RecallDeterministicSeed(1),
        generatedAt = Moment(1),
        provenance = RecallProvenance.EVALUATIVE,
        platformRequirements = RecallPlatformRequirements(requiresTextInput = true, requiresExampleRendering = true),
        contentCapabilities = RecallContentCapabilities(
            ContentId("content"),
            setOf(RecallCapability.SOURCE_TEXT, RecallCapability.EXAMPLE_SOURCE)
        )
    )

    private fun experiencePlan() = LearningExperiencePlan(
        options = LearningExperienceOptions.from(listOf(LearningExperienceKind.PROMPT_RECALL)),
        capabilities = LearningExperienceCapabilities(false, false, false, false, false, true, false),
        context = LearningExperienceContext(false),
        visibleSupportingRoles = emptySet()
    )

    private fun selection(plan: LearningExperiencePlan) = ExperienceSelectionResult(
        LearningExperienceKind.PROMPT_RECALL,
        plan.options.orderedKinds,
        0,
        ExperienceSelectionReason.ROUND_ROBIN
    )

    private fun source(name: String): String {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
        return (if (fromRoot.isFile) fromRoot else File("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"))
            .readText()
    }
}
