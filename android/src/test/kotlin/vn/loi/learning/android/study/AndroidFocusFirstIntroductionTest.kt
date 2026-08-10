package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidFocusFirstIntroductionTest {
    @Test
    fun `reveal alone records no review and revealed rating advances exactly once`() {
        val fixture = fixture("front-good", itemCount = 2)
        val first = assertIs<AndroidStudyState.Introduction>(fixture.facade.load(fixture.sessionId.value))
        val revealed = assertIs<AndroidStudyState.Introduction>(fixture.facade.revealIntroduction(first))

        assertTrue(revealed.revealed)
        assertEquals(first.learningItemId, revealed.learningItemId)
        assertEquals(0, fixture.context.engine.getSession(fixture.sessionId)!!.newItemsReviewed)
        assertTrue(fixture.context.reviewEventRepository!!.findAll(fixture.learner).isEmpty())
        val next = assertIs<AndroidStudyState.Introduction>(
            fixture.facade.rateIntroduction(revealed, ReviewRating.GOOD)
        )

        assertTrue(next.learningItemId != first.learningItemId)
        val session = fixture.context.engine.getSession(fixture.sessionId)!!
        assertEquals(1, session.newItemsReviewed)
        assertEquals(1, session.reviewedContentIds.size)
        assertEquals(1, session.introducedContentIds.size)
        assertEquals(1, fixture.context.reviewEventRepository!!.findAll(fixture.learner).size)

        assertEquals(revealed, fixture.facade.rateIntroduction(revealed, ReviewRating.GOOD))
        assertEquals(1, fixture.context.engine.getSession(fixture.sessionId)!!.newItemsReviewed)
        assertEquals(1, fixture.context.reviewEventRepository!!.findAll(fixture.learner).size)
    }

    @Test
    fun `all revealed ratings use the same canonical review path`() {
        ReviewRating.entries.forEach { rating ->
            val fixture = fixture("front-${rating.name.lowercase()}", itemCount = 1)
            val intro = assertIs<AndroidStudyState.Introduction>(fixture.facade.load(fixture.sessionId.value))

            val revealed = assertIs<AndroidStudyState.Introduction>(fixture.facade.revealIntroduction(intro))
            assertIs<AndroidStudyState.Completion>(fixture.facade.rateIntroduction(revealed, rating))

            val event = fixture.context.reviewEventRepository!!.findAll(fixture.learner).single()
            assertEquals(rating, event.rating)
            assertEquals(1, fixture.context.engine.getSession(fixture.sessionId)!!.newItemsReviewed)
        }
    }

    @Test
    fun `front ratings introduce review and advance exactly once without visual reveal`() {
        ReviewRating.entries.forEach { rating ->
            val fixture = fixture("direct-${rating.name.lowercase()}", itemCount = 2)
            val front = assertIs<AndroidStudyState.Introduction>(fixture.facade.load(fixture.sessionId.value))
            assertFalse(front.revealed)

            val next = assertIs<AndroidStudyState.Introduction>(fixture.facade.rateIntroduction(front, rating))
            assertTrue(next.learningItemId != front.learningItemId)
            assertEquals(rating, fixture.context.reviewEventRepository!!.findAll(fixture.learner).single().rating)
            assertEquals(1, fixture.context.engine.getSession(fixture.sessionId)!!.newItemsReviewed)

            assertEquals(front, fixture.facade.rateIntroduction(front, rating))
            assertEquals(1, fixture.context.reviewEventRepository!!.findAll(fixture.learner).size)
        }
    }

    @Test
    fun `Introduction POS resolves canonical custom field instead of tag-only fallback`() {
        val content = Content(
            id = ContentId("pos-custom-field"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "uncle", translatedText = "chú"),
            customFields = ContentCustomFields(
                setOf(ContentCustomField(ContentFieldId("partOfSpeech"), "noun"))
            )
        )

        assertEquals("noun", resolveIntroductionPartOfSpeech(content))
    }

    @Test
    fun `part of speech aliases normalize to stable canonical labels and colors`() {
        listOf("n", "noun", "NOUN").forEach {
            assertEquals(PartOfSpeechPresentation("NOUN", 0), partOfSpeechPresentation(it))
        }
        listOf("v", "verb", "VERB").forEach {
            assertEquals(PartOfSpeechPresentation("VERB", 1), partOfSpeechPresentation(it))
        }
        assertEquals("ADJECTIVE", canonicalPartOfSpeech("adj"))
        assertEquals("PHRASAL VERB", canonicalPartOfSpeech("verb phrase"))
        assertNull(partOfSpeechPresentation(" "))

        val first = partOfSpeechPresentation("custom lexical role")
        val reopened = partOfSpeechPresentation(" CUSTOM_LEXICAL_ROLE ")
        assertEquals(first, reopened)
        assertTrue(first!!.paletteIndex in 0..8)
    }

    @Test
    fun `example audio routes remain language specific and missing Vietnamese never falls back`() {
        val english = introductionExampleAudioRoute(false, "/audio/en.mp3", "/audio/vi.mp3")
        val vietnamese = introductionExampleAudioRoute(true, "/audio/en.mp3", "/audio/vi.mp3")
        assertEquals(AudioRole.EXAMPLE_ENGLISH, english.role)
        assertEquals("/audio/en.mp3", english.path)
        assertTrue(english.isLooping)
        assertEquals(AudioRole.EXAMPLE_VIETNAMESE, vietnamese.role)
        assertEquals("/audio/vi.mp3", vietnamese.path)
        assertFalse(vietnamese.isLooping)

        val missingVietnamese = introductionExampleAudioRoute(true, "/audio/en.mp3", null)
        assertEquals(AudioRole.EXAMPLE_VIETNAMESE, missingVietnamese.role)
        assertNull(missingVietnamese.path)
    }

    @Test
    fun `legacy POS prefix is removed from pronunciation without damaging IPA`() {
        assertEquals("/gruːm/", normalizedIntroductionPronunciation("n", "/(n) /gruːm//"))
        assertEquals("/gruːm/", normalizedIntroductionPronunciation("noun", "(noun) /gruːm/"))
        assertEquals("/vɜːb/", normalizedIntroductionPronunciation("verb", "/vɜːb/"))
        assertEquals("/wɜːd/", normalizedIntroductionPronunciation(null, "wɜːd"))
        assertNull(normalizedIntroductionPronunciation("noun", " "))
    }

    @Test
    fun `gesture resolver accepts one dominant upward swipe only without scrolling`() {
        assertEquals(
            IntroductionStageGesture.SWIPE_GOOD,
            gesture(deltaX = 18f, deltaY = -90f)
        )
        assertEquals(IntroductionStageGesture.NONE, gesture(deltaX = 18f, deltaY = -90f, scrollRequired = true))
        assertEquals(IntroductionStageGesture.NONE, gesture(deltaX = 5f, deltaY = -30f))
        assertEquals(IntroductionStageGesture.NONE, gesture(deltaX = 90f, deltaY = -80f))
        assertEquals(IntroductionStageGesture.NONE, gesture(deltaX = 18f, deltaY = -90f, alreadySubmitted = true))
        assertEquals(IntroductionStageGesture.NONE, gesture(deltaX = 18f, deltaY = -90f, childConsumed = true))
        assertEquals(
            IntroductionStageGesture.NONE,
            gesture(deltaX = 18f, deltaY = -90f, ratingEnabled = false)
        )
    }

    @Test
    fun `gesture resolver preserves generic tap without converting drag`() {
        assertEquals(IntroductionStageGesture.TAP, gesture(deltaX = 3f, deltaY = 4f))
        assertEquals(IntroductionStageGesture.NONE, gesture(deltaX = 20f, deltaY = 15f))
    }

    @Test
    fun `gesture resolver directionally locks horizontal traversal away from vertical Good`() {
        assertEquals(IntroductionStageGesture.PREVIOUS, gesture(deltaX = 100f, deltaY = 12f))
        assertEquals(IntroductionStageGesture.NEXT, gesture(deltaX = -100f, deltaY = -12f))
        assertEquals(IntroductionStageGesture.SWIPE_GOOD, gesture(deltaX = 12f, deltaY = -100f))
    }

    @Test
    fun `revealed generic playback cycles word and example with safe fallbacks`() {
        assertEquals(
            IntroductionPlaybackFocus.EXAMPLE,
            nextIntroductionPlaybackFocus(IntroductionPlaybackFocus.WORD, true, true)
        )
        assertEquals(
            IntroductionPlaybackFocus.WORD,
            nextIntroductionPlaybackFocus(IntroductionPlaybackFocus.EXAMPLE, true, true)
        )
        assertEquals(
            IntroductionPlaybackFocus.WORD,
            nextIntroductionPlaybackFocus(IntroductionPlaybackFocus.WORD, true, false)
        )
        assertEquals(
            IntroductionPlaybackFocus.EXAMPLE,
            nextIntroductionPlaybackFocus(IntroductionPlaybackFocus.WORD, false, true)
        )
        assertEquals(null, nextIntroductionPlaybackFocus(IntroductionPlaybackFocus.WORD, false, false))
    }

    @Test
    fun `Vietnamese one shot has no English auto resume policy`() {
        val policy = source("vn/loi/learning/android/study/StudyPresentationPolicy.kt")
        assertFalse(policy.contains("loopRoleAfterTemporaryAudio"))
        val screen = source("vn/loi/learning/android/study/StudyScreen.kt")
        assertFalse(screen.contains("resumeLoopAfterTemporary"))
        assertFalse(screen.contains("resumableLoopFocus"))
    }

    @Test
    fun `rating feedback always replays expected answer regardless of resumable focus`() {
        assertEquals(
            RatingFeedbackAudio(AudioRole.EXPECTED_ANSWER, "/audio/answer.mp3"),
            resolveRatingFeedbackAudio(
                IntroductionPlaybackFocus.EXAMPLE,
                "/audio/answer.mp3",
                "/audio/example.mp3"
            )
        )
        assertEquals(
            RatingFeedbackAudio(AudioRole.EXPECTED_ANSWER, "/audio/answer.mp3"),
            resolveRatingFeedbackAudio(
                IntroductionPlaybackFocus.EXAMPLE,
                "/audio/answer.mp3",
                null
            )
        )
        assertTrue(StudyRatingFeedbackPolicy.timeoutMillis in 10_000L..15_000L)
        assertTrue(StudyRatingFeedbackPolicy.pulseMillis in 180..250)
    }

    @Test
    fun `package position is distinct from session progress and follows package content order`() {
        assertEquals(PackageStudyPosition(3, 4), resolvePackageStudyPosition(listOf("a", "b", "c", "d"), "c"))
        assertEquals(PackageStudyPosition(2, 3), resolvePackageStudyPosition(listOf("a", "b", "b", "c"), "b"))
        assertNull(resolvePackageStudyPosition(listOf("a", "b"), "missing"))
    }

    @Test
    fun `composition exposes direct rating and swipe Good on both Introduction sides`() {
        val screen = source("vn/loi/learning/android/study/StudyScreen.kt")
        val bottomBar = screen.substringAfter("bottomBar = {").substringBefore("}")
        assertFalse(bottomBar.contains("state is AndroidStudyState.Introduction"))
        assertFalse(bottomBar.contains("state.revealed"))
        assertFalse(screen.contains("item(\"introduction-rating\")"))
        assertTrue(screen.contains("StudyRatingBar("))
        assertTrue(screen.contains("onDragOffset = { swipeOffsetTarget = it }"))
        assertTrue(screen.contains("pass = PointerEventPass.Initial"))
        assertTrue(screen.contains("var childConsumed = down.isConsumed"))
        assertTrue(screen.contains("submitIntroductionRating(ReviewRating.GOOD)"))
        assertTrue(screen.contains("onRating = submitIntroductionRating"))
        assertTrue(screen.contains("if (state is AndroidStudyState.Introduction && !swipeRatingSubmitted)"))
        assertTrue(screen.contains("ratingEnabled = true"))
        assertFalse(screen.contains("ratingEnabled = state.revealed"))
        assertFalse(screen.contains("state.revealed && !swipeRatingSubmitted"))
        assertTrue(screen.contains("LaunchedEffect(itemKey, (state as? AndroidStudyState.Introduction)?.revealed)"))
        assertTrue(screen.contains("onOpenFullscreenSecondary = if (state.revealed) onOpenFullscreenImage else null"))
    }

    @Test
    fun `hero height bounds respond to viewport without a fixed reveal ratio`() {
        val compact = resolveIntroductionImageBounds(640)
        val tall = resolveIntroductionImageBounds(900)
        val short = resolveIntroductionImageBounds(420)

        assertEquals(345, compact.frontMaxHeightDp)
        assertEquals(230, compact.revealMaxHeightDp)
        assertEquals(460, tall.frontMaxHeightDp)
        assertEquals(324, tall.revealMaxHeightDp)
        assertEquals(226, short.frontMaxHeightDp)
        assertEquals(170, short.revealMaxHeightDp)
    }

    @Test
    fun `Vietnamese clue typography reduces emphasis as content grows`() {
        assertEquals(32, introductionClueTextSizeSp(24))
        assertEquals(28, introductionClueTextSizeSp(60))
        assertEquals(24, introductionClueTextSizeSp(120))
    }

    @Test
    fun `corrected canvas removes persistent reveal instruction and full width answer row`() {
        val screen = source("vn/loi/learning/android/study/StudyScreen.kt")
        val introduction = screen.substringAfter("private fun IntroductionLearningStage(")
            .substringBefore("fun StudyAudioButton(")

        assertFalse(introduction.contains("Text(\"Tap to reveal\""))
        assertTrue(introduction.contains("StudyAudioTextTarget("))
        assertFalse(introduction.contains("LearningEngineAudioIndicator("))
        assertTrue(introduction.contains("StudyAnswerSection("))
        assertTrue(introduction.contains("MaterialTheme.colorScheme.surface"))
        assertFalse(introduction.contains("color = MaterialTheme.colorScheme.surfaceContainerLow"))
        assertTrue(introduction.contains("onDragOffset"))
        assertTrue(introduction.contains("swipeOffsetTarget"))
        assertFalse(introduction.contains("translationY = swipeOffset\n"))
        assertTrue(introduction.contains("translationY = swipeOffset.coerceIn(-maximumContentOffsetPx, 0f)"))
        assertTrue(introduction.contains("0.994f"))
        listOf("\"Expected Answer\"", "\"Meaning\"", "\"Example\"", "\"Translation\"", "\"Answer revealed\"").forEach {
            assertFalse(introduction.contains(it), it)
        }
    }

    @Test
    fun `Android Study projects canonical and legacy example fields through one safe boundary`() {
        val facade = source("vn/loi/learning/android/study/AndroidStudyFacade.kt")
        assertTrue(facade.contains("LegacyExampleTranslationProjection.project("))
        assertTrue(facade.contains("example = projectedExample.exampleText"))
        assertTrue(facade.contains("translation = projectedExample.exampleTranslation"))
    }

    private fun gesture(
        deltaX: Float,
        deltaY: Float,
        scrollRequired: Boolean = false,
        childConsumed: Boolean = false,
        alreadySubmitted: Boolean = false,
        ratingEnabled: Boolean = true
    ) = resolveIntroductionStageGesture(
        deltaX = deltaX,
        deltaY = deltaY,
        swipeThresholdPx = 72f,
        tapSlopPx = 12f,
        scrollRequired = scrollRequired,
        childConsumed = childConsumed,
        alreadySubmitted = alreadySubmitted,
        ratingEnabled = ratingEnabled
    )

    private fun fixture(prefix: String, itemCount: Int): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentIds = (1..itemCount).map { index ->
            val contentId = ContentId("$prefix-content-$index")
            val itemId = LearningItemId("$prefix-item-$index")
            context.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("word-$index", "meaning-$index")))
            context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
            contentId
        }
        val sessionId = SessionId("$prefix-session")
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId,
                learner,
                Moment(1_000),
                SessionPolicy(newItemLimit = itemCount, reviewItemLimit = 0),
                contentIds.toSet()
            )
        )
        return Fixture(context, learner, sessionId, AndroidStudyFacade(context, learner, now = { 2_000 }))
    }

    private fun source(relative: String): String = Files.readString(Path.of("src/main/kotlin").resolve(relative))

    private data class Fixture(
        val context: LearningApplicationContext,
        val learner: LearnerId,
        val sessionId: SessionId,
        val facade: AndroidStudyFacade
    )
}
