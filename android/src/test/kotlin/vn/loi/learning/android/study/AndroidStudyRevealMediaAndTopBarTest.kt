package vn.loi.learning.android.study

import vn.loi.learning.application.review.ReviewCommand
import org.junit.Test
import kotlin.test.*
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioState
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidStudyRevealMediaAndTopBarTest {
    @Test
    fun `all learned Image Recall correct and incorrect each commit once and Continue never rates`() {
        fun execute(answer: String, expectedRating: ReviewRating, expectedOutcome: RecallOutcome) {
            val f = fixtureWithFullMedia(allowRepeat = true)
            assertIs<AndroidStudyState.Runtime>(f.facade.load())
            val initial = assertIs<AndroidStudyState.ImageRecall>(f.facade.present(imageRecallPlan(f)))
            assertTrue(initial.answerAudioLoopEnabled)
            val before = f.context.engine.getReviewHistory(LearnerId("default-learner"), f.itemId).size
            val submitted = f.facade.submitText(initial, answer)
            if (submitted is AndroidStudyState.Failed) error(submitted.message)
            val completed = assertIs<AndroidStudyState.ImageRecall>(submitted)
            assertEquals(expectedOutcome, completed.outcome)
            assertTrue(completed.completed)
            val afterCheck = f.context.engine.getReviewHistory(LearnerId("default-learner"), f.itemId)
            assertEquals(before + 1, afterCheck.size)
            assertEquals(expectedRating, afterCheck.last().rating)
            assertNotNull(f.context.memoryStateRepository!!.find(LearnerId("default-learner"), f.itemId))
            assertEquals(1, completed.hud?.reviewCompleted)
            f.facade.next(completed)
            assertEquals(afterCheck.size, f.context.engine.getReviewHistory(LearnerId("default-learner"), f.itemId).size)
        }

        execute("apple", ReviewRating.GOOD, RecallOutcome.CORRECT)
        execute("wrong", ReviewRating.AGAIN, RecallOutcome.INCORRECT)
    }

    @Test fun `Image Recall removes duplicate visible prompt and owns accessible unlabeled input`() {
        val source = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/study/modes/ImageRecallStage.kt"))
        assertFalse(source.contains("StudyPrompt(\"Name this item\""))
        assertTrue(source.contains("label = \"\""))
        assertTrue(source.contains("accessibilityLabel = \"Nhập từ tiếng Anh được gợi nhớ bởi hình ảnh\""))
        val screen = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt"))
        val autoplay = screen.substringAfter("val imageRecall = state as? AndroidStudyState.ImageRecall")
        assertTrue(autoplay.contains("audioOwnership.claimAutoplay"))
        assertTrue(autoplay.contains("resolvedExpectedAnswerAudio, true"))
    }
    @Test
    fun `all adaptive runtime projections carry canonical POS`() {
        val facadeSource = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/study/AndroidStudyFacade.kt")
        )
        val runtime = facadeSource.substringAfter("sealed interface Runtime").substringBefore("data class Completion")
        listOf("MultipleChoice", "Listening", "ImageRecall", "ExampleCompletion").forEach { mode ->
            val state = runtime.substringAfter("data class $mode").substringBefore(") : Runtime")
            assertTrue(state.contains("override val partOfSpeech"), mode)
        }
        val present = facadeSource.substringAfter("internal fun present(plan:").substringBefore("private fun execute")
        assertTrue(present.contains("val partOfSpeech = content?.let(::resolveIntroductionPartOfSpeech)"))
    }

    @Test
    fun `all five ContentMedia references map independently in runtime state`() {
        val f = fixtureWithFullMedia()
        val state = assertIs<AndroidStudyState.Runtime>(f.facade.load())

        assertEquals("/resolved/media/prompt.mp3", state.resolvedPromptAudio)
        assertEquals("/resolved/media/prompt.mp3", state.resolvedExpectedAnswerAudio)
        assertEquals("/resolved/media/answer.mp3", state.resolvedMeaningAudio)
        assertEquals("/resolved/media/example.mp3", state.resolvedExampleEnglishAudio)
        assertEquals("/resolved/media/example_trans.mp3", state.resolvedExampleVietnameseAudio)
        assertEquals("/resolved/media/image.png", state.resolvedImage)
        assertEquals("/resolved/media/prompt.mp3", state.resolvedAudio)
    }

    @Test
    fun `missing media references map to null`() {
        val f = fixtureWithNoMedia()
        val state = assertIs<AndroidStudyState.Runtime>(f.facade.load())

        assertNull(state.resolvedPromptAudio)
        assertNull(state.resolvedExpectedAnswerAudio)
        assertNull(state.resolvedMeaningAudio)
        assertNull(state.resolvedExampleEnglishAudio)
        assertNull(state.resolvedExampleVietnameseAudio)
        assertNull(state.resolvedImage)
        assertNull(state.resolvedAudio)
    }

    @Test
    fun `audio controller stop releases active playback safely`() {
        val controller = AndroidAudioController(null)
        controller.stop()
        controller.close()
    }

    @Test
    fun `Listening prompt authority uses RecallPrompt audio with primaryAudio fallback`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-listening")
        val itemId = LearningItemId("item-listening")
        val content = Content(
            contentId, ContentType.WORD,
            ContentText("listen", "nghe", "example listen", "ví dụ nghe"),
            ContentMedia(primaryAudio = "primary.mp3")
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.LISTENING_RECOGNITION))

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/resolved/$it" })

        val plan = RecallPlan(
            planId = RecallPlanId("plan-listening"),
            learnerId = learner,
            contentId = contentId,
            learningItemId = itemId,
            sessionId = SessionId("session-listening"),
            mode = RecallMode.LISTENING,
            direction = RecallDirection.AUDIO_TO_TEXT,
            prompt = RecallPrompt.Listening(
                audio = RecallResourceId("listening_specific.mp3")
            ),
            answerContract = RecallAnswerContract(
                canonicalAnswer = "listen",
                normalizationPolicy = RecallNormalizationPolicyId("norm-v1"),
                caseSensitivity = CaseSensitivity.INSENSITIVE,
                punctuationPolicy = PunctuationPolicy.IGNORE,
                whitespacePolicy = WhitespacePolicy.NORMALIZE,
                expectedLanguage = RecallLanguageTag("en"),
                kind = RecallAnswerKind.TEXT
            ),
            availableAssistance = emptySet(),
            evidenceClass = RecallEvidenceEligibility.STRONG,
            deterministicSeed = RecallDeterministicSeed(1L),
            generatedAt = Moment(1_000),
            provenance = RecallProvenance.EVALUATIVE,
            platformRequirements = RecallPlatformRequirements(),
            contentCapabilities = RecallContentCapabilities(
                contentId = contentId,
                available = setOf(RecallCapability.WORD_AUDIO),
                wordAudio = RecallResourceId("listening_specific.mp3")
            )
        )

        val presented = assertIs<AndroidStudyState.Listening>(facade.present(plan))
        assertEquals("/resolved/listening_specific.mp3", presented.resolvedPromptAudio)
        assertEquals("/resolved/listening_specific.mp3", presented.audioPath)
    }

    @Test
    fun `Image Recall prompt authority uses RecallPrompt image with canonical fallback`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-image")
        val itemId = LearningItemId("item-image")
        val content = Content(
            contentId, ContentType.WORD,
            ContentText("cat", "con mèo"),
            ContentMedia(image = "cat_canonical.png")
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/resolved/$it" })

        val plan = RecallPlan(
            planId = RecallPlanId("plan-image"),
            learnerId = learner,
            contentId = contentId,
            learningItemId = itemId,
            sessionId = SessionId("session-image"),
            mode = RecallMode.IMAGE_RECALL,
            direction = RecallDirection.IMAGE_TO_TEXT,
            prompt = RecallPrompt.ImageRecall(
                image = RecallResourceId("cat_specific.png")
            ),
            answerContract = RecallAnswerContract(
                canonicalAnswer = "cat",
                normalizationPolicy = RecallNormalizationPolicyId("norm-v1"),
                caseSensitivity = CaseSensitivity.INSENSITIVE,
                punctuationPolicy = PunctuationPolicy.IGNORE,
                whitespacePolicy = WhitespacePolicy.NORMALIZE,
                expectedLanguage = RecallLanguageTag("en"),
                kind = RecallAnswerKind.TEXT
            ),
            availableAssistance = emptySet(),
            evidenceClass = RecallEvidenceEligibility.STRONG,
            deterministicSeed = RecallDeterministicSeed(1L),
            generatedAt = Moment(1_000),
            provenance = RecallProvenance.EVALUATIVE,
            platformRequirements = RecallPlatformRequirements(),
            contentCapabilities = RecallContentCapabilities(
                contentId = contentId,
                available = setOf(RecallCapability.IMAGE),
                image = RecallResourceId("cat_specific.png")
            )
        )

        val presented = assertIs<AndroidStudyState.ImageRecall>(facade.present(plan))
        assertEquals("/resolved/cat_specific.png", presented.resolvedImage)
        assertEquals("/resolved/cat_specific.png", presented.imagePath)
    }

    @Test
    fun `reveal and workflow preserve exact session identity and content details`() {
        val f = fixtureWithFullMedia()
        val initial = assertIs<AndroidStudyState.Typing>(f.facade.load())

        val revealed = assertIs<AndroidStudyState.Typing>(f.facade.reveal(initial))
        assertTrue(revealed.revealed)
        assertEquals(initial.plan.sessionId, revealed.plan.sessionId)
        assertEquals(initial.plan.contentId, revealed.plan.contentId)
        assertEquals("/resolved/media/prompt.mp3", revealed.resolvedExpectedAnswerAudio)
        assertEquals("/resolved/media/answer.mp3", revealed.resolvedMeaningAudio)
        assertEquals("/resolved/media/example.mp3", revealed.resolvedExampleEnglishAudio)
        assertEquals("/resolved/media/example_trans.mp3", revealed.resolvedExampleVietnameseAudio)
        assertEquals("I eat an apple", revealed.example)
        assertEquals("Tôi ăn một quả táo", revealed.translation)
        assertEquals("/resolved/media/image.png", revealed.resolvedImage)
    }

    private fun fixtureWithFullMedia(allowRepeat: Boolean = false): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-full-media")
        val itemId = LearningItemId("item-full-media")
        val content = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "apple",
                translatedText = "quả táo",
                exampleText = "I eat an apple",
                exampleTranslation = "Tôi ăn một quả táo",
                pronunciation = "/ˈæp.əl/"
            ),
            media = ContentMedia(
                primaryAudio = "media/prompt.mp3",
                translatedAudio = "media/answer.mp3",
                exampleAudio = "media/example.mp3",
                exampleTranslatedAudio = "media/example_trans.mp3",
                image = "media/image.png"
            )
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECALL))
        context.engine.review(ReviewCommand(ReviewEventId("seed-full-media"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))
        val sessionId = SessionId("android-session-full-media")
        val sessionFull = StudySession.start(
            sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1, allowRepeatInSameSession = allowRepeat),
            setOf(contentId), studyMode = StudyMode.TYPING
        )
        context.studySessionRepository!!.save(sessionFull)
        context.studyQueue.create(
            sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1
        )

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/resolved/$it" })
        return Fixture(context, learner, itemId, sessionId, facade)
    }

    private fun imageRecallPlan(f: Fixture) = RecallPlan(
        planId = RecallPlanId("all-learned-image-${System.nanoTime()}"),
        learnerId = LearnerId("default-learner"), contentId = ContentId("content-full-media"),
        learningItemId = f.itemId, sessionId = f.sessionId,
        mode = RecallMode.IMAGE_RECALL, direction = RecallDirection.IMAGE_TO_TEXT,
        prompt = RecallPrompt.ImageRecall(RecallResourceId("media/image.png")),
        answerContract = RecallAnswerContract(
            canonicalAnswer = "apple",
            normalizationPolicy = RecallNormalizationPolicyId("norm-v1"),
            caseSensitivity = CaseSensitivity.INSENSITIVE,
            punctuationPolicy = PunctuationPolicy.IGNORE,
            whitespacePolicy = WhitespacePolicy.NORMALIZE,
            expectedLanguage = RecallLanguageTag("en"),
            kind = RecallAnswerKind.TEXT
        ),
        availableAssistance = emptySet(), evidenceClass = RecallEvidenceEligibility.STRONG,
        deterministicSeed = RecallDeterministicSeed(1L), generatedAt = Moment(1_500),
        provenance = RecallProvenance.EVALUATIVE,
        platformRequirements = RecallPlatformRequirements(requiresTextInput = true, requiresImageRendering = true),
        contentCapabilities = RecallContentCapabilities(
            ContentId("content-full-media"), setOf(RecallCapability.SOURCE_TEXT, RecallCapability.IMAGE, RecallCapability.WORD_AUDIO),
            wordAudio = RecallResourceId("media/prompt.mp3"), image = RecallResourceId("media/image.png")
        )
    )

    private fun fixtureWithNoMedia(): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-no-media")
        val itemId = LearningItemId("item-no-media")
        val content = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "banana",
                translatedText = "quả chuối"
            ),
            media = ContentMedia()
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECALL))
        context.engine.review(ReviewCommand(ReviewEventId("seed-no-media"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))

        val sessionId = SessionId("android-session-no-media")
        val sessionNoMedia = StudySession.start(
            sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1), setOf(contentId)
        )
        context.studySessionRepository!!.save(sessionNoMedia)
        context.studyQueue.create(
            sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contentId), configuredReviewTarget = 1, effectiveReviewWorkload = 1
        )

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { null })
        return Fixture(context, learner, itemId, sessionId, facade)
    }

    private data class Fixture(
        val context: LearningApplicationContext,
        val learner: LearnerId,
        val itemId: LearningItemId,
        val sessionId: SessionId,
        val facade: AndroidStudyFacade
    )
}
