package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.learningexperience.ExperienceSelectionReason
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.application.learningflow.LearningFlowStageId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

class StudyReviewMemoryDockTest {
    @Test
    fun `image listening prompt and typing review modes share memory footer semantics`() {
        LearningExperienceKind.entries.forEach { kind ->
            val context =
                CurrentStudyItemReviewContext(
                    origin = SessionItemOrigin.REVIEW,
                    previousRating = ReviewRating.GOOD
                )
            val state =
                StudyUiState(
                    hasActiveSession = true,
                    canRevealAnswer = true,
                    currentItemReviewContext = context,
                    learningFlowCurrentStage =
                        LearningFlowStage.Experience(
                            id = LearningFlowStageId("experience-$kind"),
                            selection =
                                ExperienceSelectionResult(
                                    selectedKind = kind,
                                    availableKinds = listOf(kind),
                                    selectedIndex = 0,
                                    reason = ExperienceSelectionReason.ROUND_ROBIN
                                )
                        )
                )

            val expected =
                if (kind == LearningExperienceKind.TYPING_RECALL) {
                    StudyActionDockMode.REVIEW_CONTEXT
                } else {
                    StudyActionDockMode.FRONT_CONTEXT
                }
            assertEquals(expected, resolveStudyActionDockMode(state))
            val segments = resolveRatingDockPresentation(RatingDockMode.QUESTION_CONTEXT, context)
            assertEquals(4, segments.size)
            assertTrue(segments.single { it.control == StudyActionControl.REVIEW_GOOD }.isPreviousRating)
        }
    }

    @Test
    fun `review pre-answer memory does not depend on reveal action availability`() {
        val state =
            StudyUiState(
                hasActiveSession = true,
                canRevealAnswer = false,
                currentItemReviewContext =
                    CurrentStudyItemReviewContext(
                        origin = SessionItemOrigin.REVIEW,
                        previousRating = ReviewRating.HARD
                    )
            )

        assertEquals(StudyActionDockMode.REVIEW_CONTEXT, resolveStudyActionDockMode(state))
    }

    @Test
    fun `new pre-answer without a dock action renders no rating context`() {
        val state =
            StudyUiState(
                hasActiveSession = true,
                canRevealAnswer = true,
                currentItemReviewContext =
                    CurrentStudyItemReviewContext(
                        origin = SessionItemOrigin.NEW,
                        previousRating = null
                    )
            )

        assertEquals(StudyActionDockMode.HIDDEN, resolveStudyActionDockMode(state))
    }
}
