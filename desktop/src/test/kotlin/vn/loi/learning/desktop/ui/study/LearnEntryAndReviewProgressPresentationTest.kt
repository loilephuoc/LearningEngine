package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.packageprogress.StudyHeaderStatistics
import vn.loi.learning.application.packageprogress.StudyPackageLearningStatistics
import vn.loi.learning.application.packageprogress.StudySessionProgressStatistics
import vn.loi.learning.application.session.LearnEntryReviewAvailability
import vn.loi.learning.application.session.LearnedItemsReviewAvailability
import vn.loi.learning.application.session.LatestCompletedSessionAvailability
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId

class LearnEntryAndReviewProgressPresentationTest {
    @Test
    fun `idle chooser projects four application-derived actions`() {
        val presentation = resolveStudyIdlePresentation(
            StudyUiState(
                learnEntryReviewAvailability = LearnEntryReviewAvailability(
                    LatestCompletedSessionAvailability.Available(SessionId("session"), 3),
                    LearnedItemsReviewAvailability.Available(12, 5)
                )
            )
        )!!

        assertEquals("Bạn muốn học gì?", presentation.title)
        assertEquals(
            listOf(
                StudyLearningAction.CONTINUE,
                StudyLearningAction.REPLAY_LATEST,
                StudyLearningAction.REVIEW_ALL_LEARNED,
                StudyLearningAction.BACK_TO_LIBRARY
            ),
            presentation.actions.map { it.action }
        )
        assertTrue(presentation.actions.all { it.enabled })
        assertTrue(presentation.actions[1].description.contains("3"))
        assertTrue(presentation.actions[2].description.contains("5"))
    }

    @Test
    fun `unavailable replay and learned review remain visible and disabled`() {
        val presentation = resolveStudyIdlePresentation(
            StudyUiState(
                learnEntryReviewAvailability = LearnEntryReviewAvailability(
                    LatestCompletedSessionAvailability.Unavailable,
                    LearnedItemsReviewAvailability.Unavailable
                )
            )
        )!!

        assertFalse(presentation.actions[1].enabled)
        assertFalse(presentation.actions[2].enabled)
        assertTrue(presentation.actions[2].description.contains("Chưa có item đã học"))
    }

    @Test
    fun `Review header renders completed over configured target`() {
        val statistics = StudyHeaderStatistics(
            StudySessionProgressStatistics(
                "session",
                newCompleted = 0,
                newConfiguredTarget = 5,
                newEffectiveWorkload = 5,
                reviewCompleted = 1,
                reviewConfiguredTarget = 5,
                reviewEffectiveWorkload = 5
            ),
            StudyPackageLearningStatistics(
                "scope", Moment(1), 0, 0, 0, 0, 0, 0, null
            )
        )
        val review = resolveStudyHeaderStatisticsPresentation(
            StudyHeaderStatisticsState.Available(statistics),
            StudyStatisticsStrings.ENGLISH
        )!!.metrics.single { it.type == StudyHeaderMetricType.REVIEW }

        assertEquals("1", review.primaryValue)
        assertEquals("/ 5", review.secondaryValue)
    }

    @Test
    fun `Good uses centered supporting line without dot or underline authority`() {
        val action = resolveStudyActionAccessibility(StudyActionControl.REVIEW_GOOD)
        assertEquals("[3]  Good", ratingButtonLabel(StudyActionControl.REVIEW_GOOD, action))
        assertTrue(action.contentDescription.contains("3 or Space"))

        val screen = source("study/StudyScreen.kt")
        val button = source("designsystem/components/base/LEButton.kt")
        assertTrue(screen.contains("supportingLabel ="))
        assertTrue(screen.contains("\"Space\""))
        assertTrue(screen.contains("control != StudyActionControl.REVIEW_GOOD"))
        assertTrue(button.contains("Column(horizontalAlignment = Alignment.CenterHorizontally)"))
        assertFalse(ratingButtonLabel(StudyActionControl.REVIEW_GOOD, action).contains("·"))
    }

    @Test
    fun `Desktop delegates availability and mutations without repository scans`() {
        val facade = source("study/StudyFacade.kt")
        val screen = source("study/StudyScreen.kt")
        assertTrue(facade.contains("getLearnEntryReviewAvailability("))
        assertTrue(facade.contains("startLearnedItemsReview("))
        assertTrue(screen.contains("continueLearning = onStartStudy"))
        assertTrue(screen.contains("replayLatestCompletedSession = onReplayLatestCompletedStudySession"))
        assertTrue(screen.contains("reviewAllLearned = onStartLearnedItemsReview"))
        assertTrue(screen.contains("backToLibrary = { onBackToLibrary?.invoke() }"))
        assertFalse(facade.contains("studySessionRepository.findAll("))
        assertFalse(facade.contains("reviewEventRepository.findAll("))
        assertFalse(facade.contains("memoryStateRepository.findAll("))
    }

    private fun source(relative: String): String =
        Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/$relative")
        )
}
