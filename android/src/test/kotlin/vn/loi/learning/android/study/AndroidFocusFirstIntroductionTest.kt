package vn.loi.learning.android.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
    fun `part of speech front label is compact normalized and optional`() {
        assertEquals("(noun)", introductionPartOfSpeechLabel("noun"))
        assertEquals("(phrasal verb)", introductionPartOfSpeechLabel("PHRASAL_VERB"))
        assertEquals(null, introductionPartOfSpeechLabel("  "))
        assertEquals(null, introductionPartOfSpeechLabel(null))
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
    fun `composition exposes direct rating on both states and swipe only after reveal`() {
        val screen = source("vn/loi/learning/android/study/StudyScreen.kt")
        val bottomBar = screen.substringAfter("bottomBar = {").substringBefore("}")
        assertFalse(bottomBar.contains("state is AndroidStudyState.Introduction"))
        assertFalse(bottomBar.contains("state.revealed"))
        assertTrue(screen.contains("item(\"introduction-rating\")"))
        assertTrue(screen.contains("LearningEngineRatingRow("))
        assertTrue(screen.contains("onDragOffset = { swipeOffsetTarget = it }"))
        assertTrue(screen.contains("pass = PointerEventPass.Initial"))
        assertTrue(screen.contains("var childConsumed = down.isConsumed"))
        assertTrue(screen.contains("submitIntroductionRating(ReviewRating.GOOD)"))
        assertTrue(screen.contains("onRating = submitIntroductionRating"))
        assertTrue(screen.contains("if (state is AndroidStudyState.Introduction && !swipeRatingSubmitted)"))
        assertTrue(screen.contains("ratingEnabled = state.revealed"))
        assertFalse(screen.contains("state.revealed && !swipeRatingSubmitted"))
        assertTrue(screen.contains("LaunchedEffect(itemKey, (state as? AndroidStudyState.Introduction)?.revealed)"))
        assertTrue(screen.contains("onOpenFullscreenSecondary = if (state.revealed) onOpenFullscreenImage else null"))
    }

    @Test
    fun `hero height bounds respond to viewport without a fixed reveal ratio`() {
        val compact = resolveIntroductionImageBounds(640)
        val tall = resolveIntroductionImageBounds(900)
        val short = resolveIntroductionImageBounds(420)

        assertEquals(371, compact.frontMaxHeightDp)
        assertEquals(268, compact.revealMaxHeightDp)
        assertEquals(440, tall.frontMaxHeightDp)
        assertEquals(360, tall.revealMaxHeightDp)
        assertEquals(243, short.frontMaxHeightDp)
        assertEquals(176, short.revealMaxHeightDp)
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
            .substringBefore("private fun StudyPromptHeader(")

        assertFalse(introduction.contains("Text(\"Tap to reveal\""))
        assertTrue(introduction.contains("IntroductionAudioTextTarget("))
        assertFalse(introduction.contains("LearningEngineAudioIndicator("))
        assertTrue(introduction.contains("accessibilityLabel = \"English example\""))
        assertTrue(introduction.contains("accessibilityLabel = \"Vietnamese example\""))
        assertTrue(introduction.contains("MaterialTheme.colorScheme.surface"))
        assertFalse(introduction.contains("color = MaterialTheme.colorScheme.surfaceContainerLow"))
        assertTrue(introduction.contains("onDragOffset"))
        assertTrue(introduction.contains("swipeOffsetTarget"))
        assertTrue(introduction.contains("wrapContentWidth()"))
        assertTrue(introduction.contains("tertiaryContainer.copy(alpha = 0.34f)"))
        assertTrue(introduction.contains("breathingScale"))
        assertTrue(introduction.contains("if (strongEmphasis && isPlaying && isLooping && !reducedMotion)"))
        listOf("\"Expected Answer\"", "\"Meaning\"", "\"Example\"", "\"Translation\"", "\"Answer revealed\"").forEach {
            assertFalse(introduction.contains(it), it)
        }
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
