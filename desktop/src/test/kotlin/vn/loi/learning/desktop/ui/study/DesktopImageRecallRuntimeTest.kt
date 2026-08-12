package vn.loi.learning.desktop.ui.study

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.*
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

class DesktopImageRecallRuntimeTest {
    @Test
    fun `router selects Image renderer only from RecallPlan mode`() {
        assertEquals(DesktopRecallRenderer.IMAGE_RECALL, DesktopRecallModeRouter.route(plan()))
    }

    @Test
    fun `presentation preserves opaque identity and exposes only safe image prompt`() {
        val image = PresentedLearningBlock.Image(Path.of("image.png"), "Learning content image")
        val answer = PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain("canonical answer"), PresentedTextRole.PRIMARY_ENGLISH
        )
        val presentation = requireNotNull(ImageRecallPresentationResolver.resolve(plan(), listOf(answer, image)))

        assertEquals("asset:image", presentation.imageResourceId)
        assertEquals(image, presentation.image)
        assertEquals("Learning content image", presentation.safeDescription)
    }

    @Test
    fun `Image scene excludes canonical answer translation IPA POS and support before submission`() {
        val experience = experiencePlan()
        val scene = assertIs<ImageScene>(
            DesktopLearningSceneProjector().project(
                experience,
                selection(experience),
                LearningContentPresentation(
                    listOf(
                        PresentedLearningSection(
                            LearningSectionKind.QUESTION,
                            listOf(
                                PresentedLearningBlock.Text(
                                    SafeMarkdownDocument.plain("canonical answer / IPA POS"),
                                    PresentedTextRole.PRIMARY_ENGLISH
                                ),
                                PresentedLearningBlock.Image(Path.of("image.png"), "Generic image prompt")
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

        assertEquals(listOf(PresentedLearningBlock.Image(Path.of("image.png"), "Generic image prompt")), scene.blocks)
        assertTrue(scene.supportingScenes.isEmpty())
        assertEquals("asset:image", scene.recallPresentation?.imageResourceId)
    }

    @Test
    fun `missing image remains typed unavailable and cannot submit`() {
        val unavailable = PresentedLearningBlock.Unavailable("Image unavailable")
        val presentation = requireNotNull(ImageRecallPresentationResolver.resolve(plan(), listOf(unavailable)))
        val gate = ImageRecallSubmissionGate()

        assertNull(presentation.image)
        assertEquals(unavailable, presentation.unavailable)
        assertFalse(gate.accept(1, correct = true, ImageRecallMediaState.UNAVAILABLE))
    }

    @Test
    fun `media probe distinguishes missing decode failure and renderable image`() {
        val directory = Files.createTempDirectory("image-recall-test")
        val invalid = Files.write(directory.resolve("invalid.png"), byteArrayOf(1, 2, 3))
        val valid = Files.write(
            directory.resolve("valid.png"),
            Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
            )
        )

        assertEquals(ImageRecallMediaState.UNAVAILABLE, DesktopImageRecallMediaProbe.inspect(directory.resolve("missing.png")))
        assertEquals(ImageRecallMediaState.DECODE_FAILED, DesktopImageRecallMediaProbe.inspect(invalid))
        assertEquals(ImageRecallMediaState.READY, DesktopImageRecallMediaProbe.inspect(valid))
    }

    @Test
    fun `landscape portrait square and large image metrics preserve fit within layout budget`() {
        val cases = listOf(1600 to 900, 900 to 1600, 1000 to 1000, 8000 to 5000)
        val metrics = cases.map { (width, height) ->
            AdaptiveStudyImagePresentationResolver.resolve(width, height, 720, 480)
        }

        assertEquals(StudyImageAspectClass.WIDE_LANDSCAPE, metrics[0].aspectClass)
        assertEquals(StudyImageAspectClass.PORTRAIT, metrics[1].aspectClass)
        assertEquals(StudyImageAspectClass.SQUARE, metrics[2].aspectClass)
        assertTrue(metrics.all { it.contentScale == androidx.compose.ui.layout.ContentScale.Fit })
        assertTrue(metrics.all { it.renderedWidthDp <= 720 && it.renderedHeightDp <= 480 })
    }

    @Test
    fun `submission gate accepts one correct revision and rejects incomplete or duplicate delivery`() {
        val gate = ImageRecallSubmissionGate()

        assertFalse(gate.accept(1, correct = false, ImageRecallMediaState.READY))
        assertFalse(gate.accept(2, correct = true, ImageRecallMediaState.LOADING))
        assertTrue(gate.accept(3, correct = true, ImageRecallMediaState.READY))
        assertFalse(gate.accept(3, correct = true, ImageRecallMediaState.READY))
    }

    @Test
    fun `exact and normalized correct request automatic success while prefix stays neutral`() {
        val initial = ImageRecallInputState()
        val prefix = ImageRecallInputInteraction.update(initial, "canonical", plan().answerContract)
        val exact = ImageRecallInputInteraction.update(prefix, "canonical answer", plan().answerContract)
        val normalized = ImageRecallInputInteraction.update(initial, "  CANONICAL, ANSWER  ", plan().answerContract)

        assertFalse(prefix.automaticSuccessRequested)
        assertFalse(prefix.explicitIncorrectFeedback)
        assertTrue(exact.automaticSuccessRequested)
        assertTrue(exact.evaluation?.correct == true)
        assertTrue(normalized.automaticSuccessRequested)
        assertEquals(RecallCorrectness.NORMALIZED, normalized.evaluation?.correctness)
    }

    @Test
    fun `incorrect Done reports feedback without clearing input then correction completes`() {
        val wrong = ImageRecallInputInteraction.update(ImageRecallInputState(), "wrong", plan().answerContract)
        val submitted = ImageRecallInputInteraction.submitIncorrect(wrong, plan().answerContract)
        val corrected = ImageRecallInputInteraction.update(submitted, "canonical answer", plan().answerContract)

        assertEquals("wrong", submitted.value.text)
        assertTrue(submitted.explicitIncorrectFeedback)
        assertFalse(submitted.automaticSuccessRequested)
        assertFalse(corrected.explicitIncorrectFeedback)
        assertTrue(corrected.automaticSuccessRequested)
    }

    @Test
    fun `image recall live diff marks only positional mismatches and clears after correction`() {
        val contract = plan().answerContract
        val prefix = recallContractTypingEvaluation(contract, "canonical")
        val wrong = recallContractTypingEvaluation(contract, "canXnical answer")
        val multiple = recallContractTypingEvaluation(contract, "xanXnical answer")
        val corrected = recallContractTypingEvaluation(contract, "canonical answer")
        val wrongSpans = requireNotNull(resolvePositionalTypingLiveDiff("canXnical answer", wrong)).mismatchSpans

        assertNull(resolvePositionalTypingLiveDiff("canonical", prefix))
        assertEquals(listOf(TypingLiveMismatchSpan(3, 4, TypingDifferenceKind.REPLACEMENT)), wrongSpans)
        assertEquals(
            listOf(
                TypingLiveMismatchSpan(0, 1, TypingDifferenceKind.REPLACEMENT),
                TypingLiveMismatchSpan(3, 4, TypingDifferenceKind.REPLACEMENT)
            ),
            requireNotNull(resolvePositionalTypingLiveDiff("xanXnical answer", multiple)).mismatchSpans
        )
        assertNull(resolvePositionalTypingLiveDiff("canonical answer", corrected))
    }

    @Test
    fun `image recall live diff is code point safe for unicode`() {
        val unicodeContract = plan().answerContract.copy(canonicalAnswer = "café 😊")
        val evaluation = recallContractTypingEvaluation(unicodeContract, "café 😢")

        assertEquals(
            listOf(TypingLiveMismatchSpan(5, 6, TypingDifferenceKind.REPLACEMENT)),
            requireNotNull(resolvePositionalTypingLiveDiff("café 😢", evaluation)).mismatchSpans
        )
    }

    @Test
    fun `new plan state resets input evaluation feedback and delivery gate`() {
        val previous = ImageRecallInputInteraction.submitIncorrect(
            ImageRecallInputInteraction.update(ImageRecallInputState(), "wrong", plan().answerContract),
            plan().answerContract
        )
        val next = ImageRecallInputState()
        val nextGate = ImageRecallSubmissionGate()

        assertTrue(previous.explicitIncorrectFeedback)
        assertEquals("", next.value.text)
        assertNull(next.evaluation)
        assertFalse(next.explicitIncorrectFeedback)
        assertTrue(nextGate.accept(1, correct = true, ImageRecallMediaState.READY))
    }

    @Test
    fun `Desktop submission uses raw TypedText and delegates evaluation and learning`() {
        val source = source("StudyFacade.kt")
            .substringAfter("fun submitImageRecall(")
            .substringBefore("private fun recallStrategyContext")

        assertTrue(source.contains("RecallSubmission.TypedText("))
        assertTrue(source.contains("text = rawInput"))
        assertTrue(source.contains("engine.executeRecall("))
        assertTrue(source.contains("engine.executeRecallLearning("))
        assertTrue(source.contains("RecallAnswerContractEvaluator.evaluate(plan.answerContract, rawInput).correct"))
        assertFalse(source.contains("normalize"))
        assertFalse(source.contains("ReviewRating."))
        assertFalse(source.contains("schedulerService"))
    }

    @Test
    fun `Image UI localizes safe description and media states with responsive token layout`() {
        val strings = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/localization/DesktopStrings.kt")
            .takeIf(File::isFile)
            ?: File("src/main/kotlin/vn/loi/learning/desktop/ui/localization/DesktopStrings.kt")
        val screen = source("StudyScreen.kt")

        assertTrue(strings.readText().contains("imageRecallInputLabel = \"Gõ từ được gợi nhớ bởi hình ảnh\""))
        assertTrue(screen.contains("ImageRecallMediaState.LOADING"))
        assertTrue(screen.contains("ImageRecallMediaState.DECODE_FAILED"))
        assertTrue(screen.contains("liveRegion = LiveRegionMode.Polite"))
        assertTrue(screen.contains("RecallAnswerInputSurface("))
        assertTrue(screen.contains("visualTransformation = typingLiveDiffVisualTransformation("))
        assertTrue(screen.contains("text = \"Xem đáp án\""))
        assertTrue(screen.contains("singleLine = true"))
        assertTrue(screen.contains("textAlign = TextAlign.Center"))
        assertTrue(screen.contains("fontSize = 32.sp"))
        val imagePanel = screen.substringAfter("private fun ImageRecallInputPanel(")
            .substringBefore("private fun ListeningRecallPanel(")
        assertFalse(Regex("""(?m)^\s*Button\(""").containsMatchIn(imagePanel))
    }

    @Test
    fun `Shared recall contract remains free of Desktop filesystem and bitmap types`() {
        val fromRoot = File("src/main/kotlin/vn/loi/learning/domain/study/recall/RecallTypes.kt")
        val contract = (if (fromRoot.isFile) fromRoot else File("../src/main/kotlin/vn/loi/learning/domain/study/recall/RecallTypes.kt"))
            .readText()

        assertFalse(contract.contains("java.io.File"))
        assertFalse(contract.contains("java.nio.file.Path"))
        assertFalse(contract.contains("ImageBitmap"))
        assertFalse(contract.contains("org.jetbrains.skia"))
    }

    private fun plan() = RecallPlan(
        planId = RecallPlanId("plan-image"),
        learnerId = LearnerId("learner"),
        contentId = ContentId("content"),
        learningItemId = LearningItemId("item"),
        sessionId = SessionId("session"),
        mode = RecallMode.IMAGE_RECALL,
        direction = RecallDirection.IMAGE_TO_TEXT,
        prompt = RecallPrompt.ImageRecall(RecallResourceId("asset:image")),
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
        platformRequirements = RecallPlatformRequirements(requiresTextInput = true, requiresImageRendering = true),
        contentCapabilities = RecallContentCapabilities(
            ContentId("content"),
            setOf(RecallCapability.SOURCE_TEXT, RecallCapability.IMAGE),
            image = RecallResourceId("asset:image")
        )
    )

    private fun experiencePlan() = LearningExperiencePlan(
        options = LearningExperienceOptions.from(listOf(LearningExperienceKind.IMAGE_RECALL)),
        capabilities = LearningExperienceCapabilities(false, true, false, false, false, false, false),
        context = LearningExperienceContext(false),
        visibleSupportingRoles = emptySet()
    )

    private fun selection(plan: LearningExperiencePlan) = ExperienceSelectionResult(
        LearningExperienceKind.IMAGE_RECALL,
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
