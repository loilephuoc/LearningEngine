package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.application.learningcontent.LearningContentSection
import vn.loi.learning.application.learningcontent.LocalLearningAssetReference
import vn.loi.learning.application.learningexperience.ExperienceSelectionEngine
import vn.loi.learning.application.learningexperience.ExperienceSelectionReason
import vn.loi.learning.application.learningexperience.ExperienceSelectionRequest
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceCapabilities
import vn.loi.learning.application.learningexperience.LearningExperienceContext
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.LearningExperienceOptions
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningexperience.LearningExperiencePolicy
import vn.loi.learning.application.learningexperience.LearningExperienceSupportingRole
import vn.loi.learning.application.learningexperience.RoundRobinExperienceStrategy
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.domain.content.model.ContentTextFormat
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

class DesktopLearningSceneProjectorTest {
    private val projector = DesktopLearningSceneProjector()
    private val questionText =
        PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("question"),
            PresentedTextRole.PRIMARY_ENGLISH
        )

    @Test
    fun `selection results map to their Desktop scene types`() {
        val presentation = presentation(questionText)

        assertIs<ImageScene>(
            project(LearningExperienceKind.IMAGE_RECALL, presentation)
        )
        assertIs<ListeningScene>(
            project(LearningExperienceKind.LISTENING_RECALL, presentation)
        )
        assertIs<PromptScene>(
            project(LearningExperienceKind.PROMPT_RECALL, presentation)
        )
        assertIs<TypingScene>(
            project(LearningExperienceKind.TYPING_RECALL, presentation)
        )
    }

    @Test
    fun `projector trusts selection when Desktop blocks contradict selected kind`() {
        val presentation = presentation(
            PresentedLearningBlock.Image(Path.of("image.png"), "image"),
            PresentedLearningBlock.Audio(Path.of("prompt.mp3"), "audio", "Pronunciation")
        )

        assertIs<PromptScene>(
            project(LearningExperienceKind.PROMPT_RECALL, presentation)
        )
    }

    @Test
    fun `image selection survives unavailable resolved media`() {
        val unavailable = PresentedLearningBlock.Unavailable("image unavailable")

        val scene = project(
            LearningExperienceKind.IMAGE_RECALL,
            presentation(unavailable),
            hasImage = true
        )

        assertIs<ImageScene>(scene)
        assertEquals(listOf(unavailable), scene.blocks)
        assertTrue(scene.capabilities.hasImage)
    }

    @Test
    fun `image recall excludes answer audio and identity from its Question blocks`() {
        val image = PresentedLearningBlock.Image(Path.of("image.png"), "image")
        val primary = PresentedLearningBlock.Audio(
            Path.of("word.mp3"), "audio", "renamed", PresentedAudioRole.PRIMARY_WORD
        )
        val meaning = PresentedLearningBlock.Audio(
            Path.of("meaning.mp3"), "audio", "renamed", PresentedAudioRole.MEANING_TRANSLATION
        )

        val scene = project(
            LearningExperienceKind.IMAGE_RECALL,
            presentation(questionText, image, meaning, primary),
            hasImage = true
        )

        assertIs<ImageScene>(scene)
        assertEquals(listOf(image), scene.blocks)
    }

    @Test
    fun `listening question keeps only primary audio in its Question blocks before reveal`() {
        val primaryAudio = PresentedLearningBlock.Audio(
            Path.of("word.mp3"),
            "audio",
            "English",
            PresentedAudioRole.PRIMARY_WORD
        )
        val meaningText = PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("nghĩa"),
            PresentedTextRole.VIETNAMESE_MEANING
        )
        val meaningAudio = PresentedLearningBlock.Audio(
            Path.of("meaning.mp3"),
            "audio",
            "Vietnamese",
            PresentedAudioRole.MEANING_TRANSLATION
        )
        val presentation = LearningContentPresentation(
            listOf(
                PresentedLearningSection(
                    LearningSectionKind.QUESTION,
                    listOf(questionText, primaryAudio)
                ),
                PresentedLearningSection(
                    LearningSectionKind.ANSWER,
                    listOf(meaningText, meaningAudio)
                )
            )
        )

        val scene = project(LearningExperienceKind.LISTENING_RECALL, presentation)

        assertIs<ListeningScene>(scene)
        assertEquals(
            listOf(primaryAudio),
            scene.blocks
        )
        assertEquals(
            listOf(meaningText, meaningAudio),
            scene.supportingScenes.single().blocks
        )
    }

    @Test
    fun `image question keeps one typed meaning as optional support without duplication`() {
        val image = PresentedLearningBlock.Image(Path.of("image.png"), "image")
        val meaning = PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("nghĩa"),
            PresentedTextRole.VIETNAMESE_MEANING
        )
        val presentation = LearningContentPresentation(
            listOf(
                PresentedLearningSection(
                    LearningSectionKind.QUESTION,
                    listOf(questionText, image)
                ),
                PresentedLearningSection(
                    LearningSectionKind.ANSWER,
                    listOf(meaning)
                )
            )
        )

        val scene = project(
            LearningExperienceKind.IMAGE_RECALL,
            presentation,
            hasImage = true
        )

        assertEquals(listOf(image), scene.blocks)
        assertEquals(listOf(meaning), scene.supportingScenes.single().blocks)
    }

    @Test
    fun `revealed plan projects meaning and example in policy order`() {
        val presentation = LearningContentPresentation(
            listOf(
                PresentedLearningSection(LearningSectionKind.QUESTION, listOf(questionText)),
                PresentedLearningSection(
                    LearningSectionKind.ANSWER,
                    listOf(
                        PresentedLearningBlock.Text(
                            SafeMarkdownDocument.plain("meaning"),
                            PresentedTextRole.VIETNAMESE_MEANING
                        )
                    )
                ),
                PresentedLearningSection(
                    LearningSectionKind.EXAMPLE,
                    listOf(
                        PresentedLearningBlock.Text(
                            SafeMarkdownDocument.plain("example"),
                            PresentedTextRole.ENGLISH_EXAMPLE
                        )
                    )
                )
            )
        )
        val plan = plan(
            kind = LearningExperienceKind.PROMPT_RECALL,
            revealed = true,
            supporting = setOf(
                LearningExperienceSupportingRole.MEANING,
                LearningExperienceSupportingRole.EXAMPLE
            )
        )

        val scene = requireNotNull(
            projector.project(plan, selection(plan, 0), presentation)
        )

        assertEquals(listOf(SceneType.MEANING, SceneType.EXAMPLE), scene.supportingScenes.map { it.type })
        assertIs<MeaningScene>(scene.supportingScenes[0])
        assertIs<ExampleScene>(scene.supportingScenes[1])
        assertTrue(scene.context.answerRevealed)
    }

    @Test
    fun `ordinal zero preserves baseline image first selection`() {
        val content = LearningContent(
            question = LearningContentSection(
                listOf(
                    text("question"),
                    image("prompt.png"),
                    audio("prompt.mp3")
                )
            ),
            answer = LearningContentSection(listOf(text("meaning")))
        )
        val plan = requireNotNull(
            LearningExperiencePolicy().plan(
                content,
                LearningExperienceContext(answerRevealed = false)
            )
        )
        val result = ExperienceSelectionEngine(RoundRobinExperienceStrategy())
            .select(ExperienceSelectionRequest(plan.options, ordinal = 0))

        assertEquals(LearningExperienceKind.IMAGE_RECALL, result.selectedKind)
        assertIs<ImageScene>(
            projector.project(plan, result, presentation(questionText))
        )
    }

    @Test
    fun `typing selection creates an interactive scene from authoritative prompt`() {
        val scene = project(
            LearningExperienceKind.TYPING_RECALL,
            presentation(questionText)
        )

        assertIs<TypingScene>(scene)
        assertEquals(TypingRecallPrompt("question"), scene.prompt)
        assertTrue(scene.capabilities.acceptsTyping)
    }

    @Test
    fun `typing projection rejects split brain direction without enabling input`() {
        val plan = plan(LearningExperienceKind.TYPING_RECALL)
        val mismatched = recallPlan().copy(direction = RecallDirection.SOURCE_TO_TARGET)

        val scene = requireNotNull(projector.project(
            plan,
            selection(plan, 0),
            typingPresentation(presentation(questionText)),
            mismatched
        ))

        assertIs<UnsupportedRecallScene>(scene)
        assertFalse(scene.capabilities.acceptsTyping)
    }

    @Test
    fun `absent inputs have no scene`() {
        val plan = plan(LearningExperienceKind.PROMPT_RECALL)

        assertNull(projector.project(null, null, presentation(questionText)))
        assertNull(projector.project(plan, selection(plan, 0), LearningContentPresentation(emptyList())))
    }

    private fun project(
        kind: LearningExperienceKind,
        presentation: LearningContentPresentation,
        hasImage: Boolean = false
    ): LearningScene {
        val plan = plan(kind, hasImage = hasImage)
        return requireNotNull(
            projector.project(
                plan,
                selection(plan, 0),
                if (kind == LearningExperienceKind.TYPING_RECALL) typingPresentation(presentation) else presentation,
                if (kind == LearningExperienceKind.TYPING_RECALL) recallPlan() else null
            )
        )
    }

    private fun recallPlan() = RecallPlan(
        planId = RecallPlanId("plan-desktop-test"),
        learnerId = LearnerId("learner-test"),
        contentId = ContentId("content-test"),
        learningItemId = LearningItemId("item-test"),
        sessionId = SessionId("session-test"),
        mode = RecallMode.TYPING,
        direction = RecallDirection.TARGET_TO_SOURCE,
        prompt = RecallPrompt.Typing("meaning"),
        answerContract = RecallAnswerContract(
            "question", emptyList(), RecallNormalizationPolicyId("typing-v1"),
            CaseSensitivity.INSENSITIVE, PunctuationPolicy.EXACT, WhitespacePolicy.NORMALIZE,
            RecallLanguageTag("en"), RecallAnswerKind.TEXT
        ),
        availableAssistance = setOf(RecallAssistance.ANSWER_REVEALED),
        evidenceClass = RecallEvidenceEligibility.STANDARD,
        deterministicSeed = RecallDeterministicSeed(1L),
        generatedAt = Moment(1L),
        provenance = RecallProvenance.EVALUATIVE,
        platformRequirements = RecallPlatformRequirements(requiresTextInput = true),
        contentCapabilities = RecallContentCapabilities(
            ContentId("content-test"),
            setOf(RecallCapability.SOURCE_TEXT, RecallCapability.TARGET_TRANSLATION)
        )
    )

    private fun typingPresentation(base: LearningContentPresentation) = LearningContentPresentation(
        base.sections + PresentedLearningSection(
            LearningSectionKind.ANSWER,
            listOf(PresentedLearningBlock.Text(SafeMarkdownDocument.plain("meaning"), PresentedTextRole.VIETNAMESE_MEANING))
        )
    )

    private fun plan(
        kind: LearningExperienceKind,
        hasImage: Boolean = false,
        revealed: Boolean = false,
        supporting: Set<LearningExperienceSupportingRole> = emptySet()
    ) = LearningExperiencePlan(
        options = LearningExperienceOptions.from(listOf(kind)),
        capabilities = LearningExperienceCapabilities(
            hasPromptText = true,
            hasPromptImage = hasImage,
            hasPromptAudio = kind == LearningExperienceKind.LISTENING_RECALL,
            hasMeaning = revealed,
            hasExample = LearningExperienceSupportingRole.EXAMPLE in supporting,
            hasAnswerAudio = false,
            hasExampleAudio = false
        ),
        context = LearningExperienceContext(revealed),
        visibleSupportingRoles = supporting,
        typingPrompt =
            if (kind == LearningExperienceKind.TYPING_RECALL) {
                TypingRecallPrompt("expected answer")
            } else {
                null
            }
    )

    private fun selection(plan: LearningExperiencePlan, index: Int) =
        ExperienceSelectionResult(
            selectedKind = plan.options.orderedKinds[index],
            availableKinds = plan.options.orderedKinds,
            selectedIndex = index,
            reason = ExperienceSelectionReason.ROUND_ROBIN
        )

    private fun presentation(
        vararg blocks: PresentedLearningBlock
    ) = LearningContentPresentation(
        listOf(PresentedLearningSection(LearningSectionKind.QUESTION, blocks.toList()))
    )

    private fun text(value: String) =
        LearningContentBlock.Text(
            value,
            ContentTextFormat.PLAIN_TEXT,
            vn.loi.learning.application.learningcontent.LearningTextRole.PRIMARY_ENGLISH
        )

    private fun image(value: String) =
        LearningContentBlock.Image(requireNotNull(LocalLearningAssetReference.from(value)))

    private fun audio(value: String) =
        LearningContentBlock.Audio(requireNotNull(LocalLearningAssetReference.from(value)))
}
