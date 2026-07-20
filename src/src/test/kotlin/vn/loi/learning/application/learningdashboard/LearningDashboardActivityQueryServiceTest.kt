package vn.loi.learning.application.learningdashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.analytics.StudyStatisticsQueryService
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.progress.LearningProgressQuery
import vn.loi.learning.application.progress.LearningProgressQueryService
import vn.loi.learning.application.progress.LearningProgressSnapshot
import vn.loi.learning.application.reviewhistory.ReviewHistoryQueryService
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.analytics.service.StudyStatisticsCalculator
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent

class LearningDashboardActivityQueryServiceTest {

    private val learnerId =
        LearnerId("learner-1")

    private val service =
        LearningDashboardActivityQueryService(
            learningProgressQueryService =
                LearningProgressQueryService(
                    studyStatisticsQueryService =
                        StudyStatisticsQueryService(
                            reviewHistoryQueryService =
                                ReviewHistoryQueryService(
                                    reviewEventRepository =
                                        EmptyReviewEventRepository()
                                ),
                            studyStatisticsCalculator =
                                StudyStatisticsCalculator()
                        )
                )
        )

    @Test
    fun `query returns activity snapshot from progress service`() {
        val result =
            service.query(
                LearningProgressQuery(
                    learnerId = learnerId,
                    activityPeriod =
                        StudyPeriod(
                            startInclusive =
                                Moment(1_000L),
                            endExclusive =
                                Moment(2_000L)
                        ),
                    evaluatedAt =
                        Moment(2_000L),
                    dailyPeriods =
                        emptyList()
                )
            )

        assertEquals(
            expected =
                LearningDashboardActivitySnapshot(
                    progress =
                        LearningProgressSnapshot.EMPTY
                ),
            actual = result
        )
    }

    private class EmptyReviewEventRepository :
        ReviewEventRepository {

        override fun append(
            event: ReviewEvent
        ) {
            error(
                "Append is not expected in this query test."
            )
        }

        override fun findAll(
            learnerId: LearnerId
        ): List<ReviewEvent> =
            emptyList()

        override fun findAll(
            learnerId: LearnerId,
            learningItemId: LearningItemId
        ): List<ReviewEvent> =
            emptyList()
    }
}