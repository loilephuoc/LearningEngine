package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.*
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidTypingRevealAudioLoopTest {

    private fun setupTestSession(withExampleAudio: Boolean = true): Pair<AndroidStudyFacade, String> {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val contentId = ContentId("content-test-typing")
        val itemId = LearningItemId("item-test-typing")
        val content = Content(
            id = contentId,
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "apple",
                translatedText = "quả táo",
                exampleText = "I eat an apple",
                exampleTranslation = "Tôi ăn một quả táo"
            ),
            media = ContentMedia(
                primaryAudio = "apple.mp3",
                translatedAudio = "qua_tao.mp3",
                exampleAudio = if (withExampleAudio) "example_apple.mp3" else null,
                exampleTranslatedAudio = if (withExampleAudio) "example_qua_tao.mp3" else null
            )
        )
        context.contentRepository!!.save(content)
        context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECALL))
        context.engine.review(ReviewCommand(ReviewEventId("seed-test"), learner, itemId, ReviewRating.GOOD, Moment(1_000)))
        val sessionId = SessionId("session-test-typing")
        val session = StudySession.start(
            sessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 0, reviewItemLimit = 1), setOf(contentId),
            studyMode = StudyMode.TYPING
        )
        context.studySessionRepository!!.save(session)
        context.studyQueue.create(
            sessionId, Moment(1_000), listOf(itemId), mapOf(itemId to SessionItemOrigin.REVIEW), mapOf(itemId to contentId),
            configuredReviewTarget = 1, effectiveReviewWorkload = 1
        )

        val facade = AndroidStudyFacade(context, learner, { 2_000 }, resolveMedia = { "/media/$it" })
        return facade to sessionId.value
    }

    @Test
    fun `shouldStartTypingRevealAnswerAutoplay triggers only on reveal entry for wrong answer`() {
        // When not revealed: false
        assertFalse(shouldStartTypingRevealAnswerAutoplay(revealed = false, completionPending = false, alreadyStarted = false))
        // When revealed and not started: true
        assertTrue(shouldStartTypingRevealAnswerAutoplay(revealed = true, completionPending = false, alreadyStarted = false))
        // When already started: false (prevents double replay)
        assertFalse(shouldStartTypingRevealAnswerAutoplay(revealed = true, completionPending = false, alreadyStarted = true))
        // When completion pending (correct auto-rating): false
        assertFalse(shouldStartTypingRevealAnswerAutoplay(revealed = true, completionPending = true, alreadyStarted = false))
    }

    @Test
    fun `nextIntroductionPlaybackFocus toggles WORD to EXAMPLE and back to WORD`() {
        val focus1 = nextIntroductionPlaybackFocus(
            current = IntroductionPlaybackFocus.WORD,
            hasWordAudio = true,
            hasExampleAudio = true
        )
        assertEquals(IntroductionPlaybackFocus.EXAMPLE, focus1)

        val focus2 = nextIntroductionPlaybackFocus(
            current = IntroductionPlaybackFocus.EXAMPLE,
            hasWordAudio = true,
            hasExampleAudio = true
        )
        assertEquals(IntroductionPlaybackFocus.WORD, focus2)
    }

    @Test
    fun `nextIntroductionPlaybackFocus safely stays on WORD when example audio is missing`() {
        val focus = nextIntroductionPlaybackFocus(
            current = IntroductionPlaybackFocus.WORD,
            hasWordAudio = true,
            hasExampleAudio = false
        )
        // If example audio is unavailable, remains safely on WORD
        assertEquals(IntroductionPlaybackFocus.WORD, focus)
    }

    @Test
    fun `nextIntroductionPlaybackFocus uses EXAMPLE when word audio is missing`() {
        val focus = nextIntroductionPlaybackFocus(
            current = IntroductionPlaybackFocus.WORD,
            hasWordAudio = false,
            hasExampleAudio = true
        )
        assertEquals(IntroductionPlaybackFocus.EXAMPLE, focus)
    }

    @Test
    fun `nextIntroductionPlaybackFocus returns null when both audio sources are missing`() {
        val focus = nextIntroductionPlaybackFocus(
            current = IntroductionPlaybackFocus.WORD,
            hasWordAudio = false,
            hasExampleAudio = false
        )
        assertNull(focus)
    }

    @Test
    fun `revealing incorrect typing answer commits AGAIN and produces revealed state`() {
        val (facade, sessionId) = setupTestSession(withExampleAudio = true)
        val initial = assertIs<AndroidStudyState.Typing>(facade.load(sessionId))
        assertFalse(initial.revealed)
        assertFalse(initial.completed)

        // Type incorrect answer and reveal
        val typed = facade.updateAnswer(initial, "wrong_answer")
        val revealed = assertIs<AndroidStudyState.Typing>(facade.reveal(typed))

        assertTrue(revealed.revealed)
        assertTrue(revealed.completed)
        assertFalse(revealed.completionPending)
        assertEquals(RecallOutcome.REVEALED, revealed.outcome)
        assertEquals("/media/apple.mp3", revealed.resolvedExpectedAnswerAudio)
        assertEquals("/media/example_apple.mp3", revealed.resolvedExampleEnglishAudio)
    }

    @Test
    fun `Continue action and swipe-up in revealed wrong typing advance queue without extra ratings or FSRS mutations`() {
        val (facade, sessionId) = setupTestSession(withExampleAudio = true)
        val initial = assertIs<AndroidStudyState.Typing>(facade.load(sessionId))

        // Submit wrong answer via Reveal
        val revealed = assertIs<AndroidStudyState.Typing>(facade.reveal(initial, "wrong_text"))
        assertTrue(revealed.revealed)
        assertTrue(revealed.completed)

        // Calling facade.next(revealed) simulates Continue button / swipe-up
        val afterContinue = facade.next(revealed)

        // Session was 1 item, so after continue it transitions to Completion
        assertIs<AndroidStudyState.Completion>(afterContinue)
    }

    @Test
    fun `interleaving card taps and word taps follows exactly one shared loop toggle state`() {
        var focus: IntroductionPlaybackFocus = IntroductionPlaybackFocus.WORD
        val hasWordAudio = true
        val hasExampleAudio = true

        // Initial: WORD is looping
        assertEquals(IntroductionPlaybackFocus.WORD, focus)

        // 1. Tap CARD -> switch to EXAMPLE
        focus = assertNotNull(nextIntroductionPlaybackFocus(focus, hasWordAudio, hasExampleAudio))
        assertEquals(IntroductionPlaybackFocus.EXAMPLE, focus)

        // 2. Tap WORD -> switch to WORD
        focus = assertNotNull(nextIntroductionPlaybackFocus(focus, hasWordAudio, hasExampleAudio))
        assertEquals(IntroductionPlaybackFocus.WORD, focus)

        // 3. Tap WORD -> switch to EXAMPLE
        focus = assertNotNull(nextIntroductionPlaybackFocus(focus, hasWordAudio, hasExampleAudio))
        assertEquals(IntroductionPlaybackFocus.EXAMPLE, focus)

        // 4. Tap CARD -> switch to WORD
        focus = assertNotNull(nextIntroductionPlaybackFocus(focus, hasWordAudio, hasExampleAudio))
        assertEquals(IntroductionPlaybackFocus.WORD, focus)

        // 5. If example is missing, tap card or word safely stays on WORD
        val missingExampleFocus = nextIntroductionPlaybackFocus(
            current = IntroductionPlaybackFocus.WORD,
            hasWordAudio = true,
            hasExampleAudio = false
        )
        assertEquals(IntroductionPlaybackFocus.WORD, missingExampleFocus)
    }

    @Test
    fun `Typing front card visual hierarchy places translation and POS on the same top row and omits READY`() {
        val modesSource = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/study/modes/TypedAnswerStages.kt")
        )
        val screenSource = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
        )

        val typing = modesSource.substringAfter("internal fun TypingStudyStage(")
            .substringBefore("private fun formatTypingSeconds")
        val frontTyping = typing.substringAfter("if (!feedbackVisible)")
            .substringBefore("if (!state.revealed)")

        // Translation text and POS badge are in the same top presentation row
        assertTrue(frontTyping.contains("state.prompt"))
        assertTrue(frontTyping.contains("PartOfSpeechBadge(pos)"))
        assertTrue(frontTyping.contains("Arrangement.SpaceBetween"))

        // READY label is deleted
        assertFalse(typing.contains("\"READY\""))

        // Word audio tap in revealed typing routes to onTypingStageTap
        val feedbackContent = screenSource.substringAfter("private fun StudyRevealAndFeedbackContent(")
            .substringBefore("private fun Completion(")
        assertTrue(feedbackContent.contains("onAnswerAudio = if (forcedTypingReveal) onTypingStageTap else"))
        assertTrue(feedbackContent.contains("onEnglishExampleAudio = if (forcedTypingReveal) onTypingStageTap else"))
    }
}
