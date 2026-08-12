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
    fun `practice advancement can reuse transition without scheduler consequence`() {
        val destination = StudyUiState(
            hasActiveSession = true,
            currentLearningItemId = "practice-b",
            practiceProgress = vn.loi.learning.application.session.PracticeProgress(
                round = 1,
                position = 2,
                membershipSize = 2
            )
        )
        val transition = createStudySessionContinuityTransition(
            RatingActionFeedback(ReviewRating.GOOD, 41L, RatingFeedbackPhase.ACTIVATED),
            "practice-a",
            destination
        )

        assertEquals(ReviewRating.GOOD, transition.finalRating)
        assertNull(transition.schedulerFeedback)
        assertEquals("practice-b", transition.destinationItemId)
    }

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
        assertEquals(StudySessionTransitionPhase.RESULT_SHOWN, transition.phase)
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
    fun `typed phases sequence result then exit then entering`() {
        val transition = createStudySessionContinuityTransition(
            RatingActionFeedback(ReviewRating.AGAIN, 7L, RatingFeedbackPhase.ACTIVATED),
            "a",
            committedNext("b", ReviewRating.AGAIN)
        )
        assertEquals(
            StudySessionTransitionPhase.EXITING_CURRENT,
            nextStudySessionTransitionPhase(transition.phase)
        )
        assertEquals(
            StudySessionTransitionPhase.ENTERING_NEXT,
            nextStudySessionTransitionPhase(StudySessionTransitionPhase.EXITING_CURRENT)
        )
        assertNull(nextStudySessionTransitionPhase(StudySessionTransitionPhase.ENTERING_NEXT))
    }

    @Test
    fun `next item consequence is disposed before destination becomes visible`() {
        val transition = createStudySessionContinuityTransition(
            RatingActionFeedback(ReviewRating.GOOD, 8L, RatingFeedbackPhase.ACTIVATED),
            "a",
            committedNext("b", ReviewRating.GOOD)
        )
        val consequence =
            resolveStudySessionContinuityPresentation(
                transition.copy(phase = StudySessionTransitionPhase.RESULT_SHOWN)
            )
        val arriving =
            resolveStudySessionContinuityPresentation(
                transition.copy(phase = StudySessionTransitionPhase.ENTERING_NEXT)
            )

        assertTrue(consequence.consequenceVisible)
        assertTrue(consequence.destinationVisible)
        assertFalse(arriving.consequenceVisible)
        assertFalse(arriving.overlayVisible)
        assertFalse(arriving.retainOverlayDuringExit)
        assertTrue(arriving.destinationVisible)
        assertTrue(arriving.destinationArriving)
    }

    @Test
    fun `completion has an explicit destination and preserves its committed consequence`() {
        val committed = committedNext(null, ReviewRating.EASY).copy(sessionCompleted = true)
        val transition = createStudySessionContinuityTransition(
            RatingActionFeedback(ReviewRating.EASY, 12L, RatingFeedbackPhase.ACTIVATED),
            "last-item",
            committed
        )
        assertEquals(StudySessionTransitionDestination.COMPLETION, transition.destination)
        assertNull(transition.destinationItemId)

        val consequence =
            resolveStudySessionContinuityPresentation(
                transition.copy(phase = StudySessionTransitionPhase.RESULT_SHOWN)
            )
        assertTrue(consequence.consequenceVisible)
        assertTrue(consequence.destinationVisible)
        assertFalse(consequence.destinationArriving)
        assertTrue(consequence.retainOverlayDuringExit)
    }

    @Test
    fun `stale transition cannot replay a disposed consequence`() {
        val transition = createStudySessionContinuityTransition(
            RatingActionFeedback(ReviewRating.HARD, 21L, RatingFeedbackPhase.ACTIVATED),
            "a",
            committedNext("b", ReviewRating.HARD)
        )
        val arrivingState = committedNext("b", ReviewRating.HARD).copy(
            sessionContinuityTransition =
                transition.copy(phase = StudySessionTransitionPhase.ENTERING_NEXT)
        )

        val presentation = resolveStudySessionContinuityPresentation(arrivingState.sessionContinuityTransition)
        assertFalse(presentation.consequenceVisible)
        assertTrue(presentation.destinationVisible)
        assertTrue(presentation.destinationArriving)
    }

    @Test
    fun `consecutive ratings do not reuse prior consequence presentation`() {
        val tokens = RatingFeedbackTokenGenerator()
        val first = createStudySessionContinuityTransition(
            tokens.activate(ReviewRating.GOOD), "a", committedNext("b", ReviewRating.GOOD)
        )
        val second = createStudySessionContinuityTransition(
            tokens.activate(ReviewRating.HARD), "b", committedNext("c", ReviewRating.HARD)
        )

        assertNotEquals(first.token, second.token)
        assertEquals(ReviewRating.HARD, second.schedulerFeedback?.committedRating)
        assertTrue(
            resolveStudySessionContinuityPresentation(second).consequenceVisible
        )
    }

    @Test
    fun `recomposition keeps arriving next item consequence disposed`() {
        val transition = createStudySessionContinuityTransition(
            RatingActionFeedback(ReviewRating.EASY, 33L, RatingFeedbackPhase.ACTIVATED),
            "a",
            committedNext("b", ReviewRating.EASY)
        ).copy(phase = StudySessionTransitionPhase.ENTERING_NEXT)

        val first = resolveStudySessionContinuityPresentation(transition)
        val recomposed = resolveStudySessionContinuityPresentation(transition.copy())

        assertEquals(first, recomposed)
        assertFalse(recomposed.consequenceVisible)
        assertTrue(recomposed.destinationVisible)
        assertTrue(recomposed.destinationArriving)
    }

    @Test
    fun `default recovery state cannot restore transient continuity`() {
        assertNull(StudyUiState().sessionContinuityTransition)
    }

    @Test
    fun `view model creates continuity only on successful rating and clears it on failure or undo`() {
        val source = source("ui/study/StudyViewModel.kt")

        assertTrue(source.contains("createStudySessionContinuityTransition("))
        assertTrue(source.contains("pendingContinuityDestination = committedState"))
        assertTrue(source.contains("StudySessionTransitionPhase.EXITING_CURRENT"))
        assertTrue(source.contains("StudySessionTransitionPhase.ENTERING_NEXT"))
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

        assertTrue(screen.contains("LETheme.motion.easingStandard"))
        assertTrue(screen.contains("continuityPhaseDuration.coerceAtLeast(1)"))
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
