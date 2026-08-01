package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.ReviewRating

class StudySessionContinuityPresentationTest {
    @Test
    fun `successful rating creates one source-bound transition from committed consequence`() {
        val activation = RatingFeedbackTokenGenerator().activate(ReviewRating.HARD)
        val transition = createStudySessionContinuityTransition(
            activation,
            sourceItemId = "item-a",
            committedState = committedNext("item-b", ReviewRating.HARD)
        )

        assertEquals(activation.token, transition.token)
        assertEquals("item-a", transition.sourceItemId)
        assertEquals("item-b", transition.destinationItemId)
        assertEquals(ReviewRating.HARD, transition.finalRating)
        assertEquals(StudySessionTransitionPhase.ACTION_CONFIRMED, transition.phase)
    }

    @Test
    fun `automatic candidate uses final committed scheduler rating`() {
        val candidateEasy = RatingActionFeedback(
            ReviewRating.EASY,
            token = 9L,
            phase = RatingFeedbackPhase.ACTIVATED
        )

        val transition = createStudySessionContinuityTransition(
            candidateEasy,
            "item-a",
            committedNext("item-b", ReviewRating.GOOD)
        )

        assertEquals(ReviewRating.GOOD, transition.finalRating)
    }

    @Test
    fun `consecutive same ratings retain distinct sequence tokens`() {
        val tokens = RatingFeedbackTokenGenerator()
        val first = createStudySessionContinuityTransition(
            tokens.activate(ReviewRating.GOOD), "a", committedNext("b", ReviewRating.GOOD)
        )
        val second = createStudySessionContinuityTransition(
            tokens.activate(ReviewRating.GOOD), "b", committedNext("c", ReviewRating.GOOD)
        )

        assertNotEquals(first.token, second.token)
    }

    @Test
    fun `token-safe phase reducer ignores stale callbacks and consumes only current sequence`() {
        val transition = createStudySessionContinuityTransition(
            RatingActionFeedback(ReviewRating.AGAIN, 7L, RatingFeedbackPhase.ACTIVATED),
            "a",
            committedNext("b", ReviewRating.AGAIN)
        )
        val initial = committedNext("b", ReviewRating.AGAIN)
            .copy(sessionContinuityTransition = transition)

        assertTrue(initial === initial.advanceSessionContinuity(6L))
        val consequence = initial.advanceSessionContinuity(7L)
        assertEquals(
            StudySessionTransitionPhase.CONSEQUENCE_VISIBLE,
            consequence.sessionContinuityTransition?.phase
        )
        val arriving = consequence.advanceSessionContinuity(7L)
        assertEquals(
            StudySessionTransitionPhase.DESTINATION_ARRIVING,
            arriving.sessionContinuityTransition?.phase
        )
        val settled = arriving.advanceSessionContinuity(7L)
        assertNull(settled.sessionContinuityTransition)
        assertNull(settled.schedulerFeedback)
    }

    @Test
    fun `completion has an explicit destination and preserves its committed consequence`() {
        val committed = committedNext(null, ReviewRating.EASY).copy(sessionCompleted = true)
        val transition = createStudySessionContinuityTransition(
            RatingActionFeedback(ReviewRating.EASY, 12L, RatingFeedbackPhase.ACTIVATED),
            "last-item",
            committed
        )
        val settled = committed.copy(sessionContinuityTransition = transition)
            .advanceSessionContinuity(12L)
            .advanceSessionContinuity(12L)
            .advanceSessionContinuity(12L)

        assertEquals(StudySessionTransitionDestination.COMPLETION, transition.destination)
        assertNull(transition.destinationItemId)
        assertEquals(committed.schedulerFeedback, settled.schedulerFeedback)
    }

    @Test
    fun `default recovery state cannot restore transient continuity`() {
        assertNull(StudyUiState().sessionContinuityTransition)
    }

    @Test
    fun `view model creates continuity only on successful rating and clears it on failure or undo`() {
        val source = source("ui/study/StudyViewModel.kt")

        assertTrue(source.contains("createStudySessionContinuityTransition("))
        assertTrue(source.contains("stateToUse.loadError == null"))
        assertTrue(source.contains("sessionContinuityTransition = null"))
        assertFalse(source.substringAfter("onFailure =").substringBefore("actionInProgress = false")
            .contains("createStudySessionContinuityTransition("))
    }

    @Test
    fun `facade commits scheduler consequence before selecting destination`() {
        val source = source("ui/study/StudyFacade.kt")
        val review = source.substringAfter("private fun reviewInternal(")
            .substringBefore("private fun validateTypingMetricsContext")

        assertTrue(review.indexOf("latestSchedulerFeedback =") < review.indexOf("loadNextItem("))
        assertTrue(review.indexOf("latestSchedulerFeedback =") < review.indexOf("sessionCompleted = true"))
    }

    @Test
    fun `screen sequences with motion tokens and keeps established focus scroll audio owners`() {
        val screen = source("ui/study/StudyScreen.kt")

        assertTrue(screen.contains("LETheme.motion.ratingDuration"))
        assertTrue(screen.contains("LETheme.motion.durationNormal"))
        assertTrue(screen.contains("LETheme.motion.durationFast"))
        assertTrue(screen.contains("resolveStudyFocusTransitionKey(uiState)"))
        assertTrue(screen.contains("mainBodyScrollState.scrollTo(0)"))
        assertTrue(screen.contains("synchronizeStudyAudio(audioController, learningScene"))
        assertTrue(screen.contains("StudyMicroInteractionResolver.reveal"))
        assertFalse(screen.contains("Thread.sleep"))
    }

    @Test
    fun `continuity resolver is presentation-only and non-persistent`() {
        val source = source("ui/study/StudySessionContinuityPresentation.kt")
        listOf("application.", "infrastructure.", "repository", "filesystem", "Json", "FSRS")
            .forEach { forbidden -> assertFalse(source.contains(forbidden), forbidden) }
    }

    private fun committedNext(itemId: String?, rating: ReviewRating): StudyUiState =
        StudyUiState(
            hasActiveSession = itemId != null,
            currentLearningItemId = itemId,
            schedulerFeedback = StudySchedulerFeedback(
                rating = rating.name,
                committedRating = rating,
                stageTransition = "REVIEW → REVIEW",
                scheduledInterval = "1 day",
                nextReviewAt = "tomorrow",
                difficultyBefore = "5.0",
                difficultyAfter = "4.9",
                stabilityBefore = "2 days",
                stabilityAfter = "3 days",
                reviewCount = 2,
                lapseCount = 0
            )
        )

    private fun source(relative: String): String =
        Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/$relative"))
}
