package vn.loi.learning.desktop.ui.study

import java.io.File
import java.nio.file.Path
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

class DesktopListeningRecallRuntimeTest {
    @Test
    fun `router selects Listening only from RecallPlan mode`() {
        assertEquals(DesktopRecallRenderer.LISTENING, DesktopRecallModeRouter.route(plan()))
    }

    @Test
    fun `Listening presentation preserves resource identity and exposes only primary audio`() {
        val audio = PresentedLearningBlock.Audio(
            Path.of("audio.wav"), "Word audio", "Pronunciation", PresentedAudioRole.PRIMARY_WORD
        )
        val canonical = PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("canonical answer"), PresentedTextRole.PRIMARY_ENGLISH
        )
        val presentation = requireNotNull(
            ListeningRecallPresentationResolver.resolve(plan(), listOf(canonical, audio))
        )

        assertEquals("asset:word-audio", presentation.audioResourceId)
        assertTrue(presentation.audioAvailable)
        assertEquals(listOf(audio), presentation.blocks)
        assertFalse(presentation.blocks.any { it is PresentedLearningBlock.Text })
    }

    @Test
    fun `Listening scene never projects canonical answer or supporting meaning before submission`() {
        val experience = experiencePlan()
        val scene = assertIs<ListeningScene>(
            DesktopLearningSceneProjector().project(
                experience,
                selection(experience),
                LearningContentPresentation(
                    listOf(
                        PresentedLearningSection(
                            LearningSectionKind.QUESTION,
                            listOf(
                                PresentedLearningBlock.Text(
                                    SafeMarkdownDocument.plain("canonical answer"),
                                    PresentedTextRole.PRIMARY_ENGLISH
                                ),
                                PresentedLearningBlock.Audio(
                                    Path.of("audio.wav"), "Word audio", "Pronunciation",
                                    PresentedAudioRole.PRIMARY_WORD
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
                plan()
            )
        )

        assertTrue(scene.blocks.all { it is PresentedLearningBlock.Audio })
        assertTrue(scene.supportingScenes.isEmpty())
        assertEquals("asset:word-audio", scene.recallPresentation?.audioResourceId)
    }

    @Test
    fun `audio unavailable remains explicit without fabricating a block`() {
        val unavailable = PresentedLearningBlock.Unavailable("Audio unavailable")
        val presentation = requireNotNull(
            ListeningRecallPresentationResolver.resolve(plan(), listOf(unavailable))
        )

        assertEquals(listOf(unavailable), presentation.blocks)
        assertFalse(presentation.audioAvailable)
    }

    @Test
    fun `Listening submission gate rejects blank and accepts exactly one raw response`() {
        val gate = ListeningSubmissionGate()

        assertFalse(gate.accept("  "))
        assertTrue(gate.accept("  Raw Café  "))
        assertFalse(gate.accept("second"))
    }

    @Test
    fun `Listening unavailable audio blocks submission without consuming the attempt`() {
        val gate = ListeningSubmissionGate()

        assertFalse(gate.accept("answer", audioAvailable = false))
        assertTrue(gate.accept("answer", audioAvailable = true))
        assertFalse(gate.accept("second", audioAvailable = true))
    }

    @Test
    fun `Desktop submission sends raw TypedText and delegates all evaluation`() {
        val source = source("StudyFacade.kt")
            .substringAfter("fun submitListening(")
            .substringBefore("private fun recallStrategyContext")

        assertTrue(source.contains("RecallSubmission.TypedText("))
        assertTrue(source.contains("text = rawInput"))
        assertTrue(source.contains("engine.executeRecall("))
        assertTrue(source.contains("engine.executeRecallLearning("))
        assertFalse(source.contains("isCorrect"))
        assertFalse(source.contains("normalize"))
        assertFalse(source.contains("ReviewRating."))
    }

    @Test
    fun `Listening localization and responsive input use shared theme tokens`() {
        val strings = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/localization/DesktopStrings.kt")
            .takeIf(File::isFile)
            ?: File("src/main/kotlin/vn/loi/learning/desktop/ui/localization/DesktopStrings.kt")
        val screen = source("StudyScreen.kt")

        assertTrue(strings.readText().contains("listeningInputLabel = \"Gõ lại nội dung bạn nghe được\""))
        assertTrue(screen.contains("ListeningRecallPanel("))
        assertTrue(screen.contains(".fillMaxWidth()"))
        assertTrue(screen.contains("LETheme.spacing.space4"))
    }

    private fun plan() = RecallPlan(
        planId = RecallPlanId("plan-listening"),
        learnerId = LearnerId("learner"),
        contentId = ContentId("content"),
        learningItemId = LearningItemId("item"),
        sessionId = SessionId("session"),
        mode = RecallMode.LISTENING,
        direction = RecallDirection.AUDIO_TO_TEXT,
        prompt = RecallPrompt.Listening(RecallResourceId("asset:word-audio")),
        answerContract = RecallAnswerContract(
            canonicalAnswer = "canonical answer",
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
        platformRequirements = RecallPlatformRequirements(requiresTextInput = true, requiresAudioPlayback = true),
        contentCapabilities = RecallContentCapabilities(
            ContentId("content"),
            setOf(RecallCapability.SOURCE_TEXT, RecallCapability.WORD_AUDIO),
            wordAudio = RecallResourceId("asset:word-audio")
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

    private fun source(name: String): String {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
        return (if (fromRoot.isFile) fromRoot else File("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"))
            .readText()
    }
}
