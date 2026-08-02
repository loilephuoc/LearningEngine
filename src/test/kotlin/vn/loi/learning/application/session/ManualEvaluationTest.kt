package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.scheduling.SimpleScheduler
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository

class EvaluativeRatingTest {
    @Test
    fun `availability is shared policy and active-item authority`() {
        val evaluative = StudySession.start(
            SessionId("evaluation"), LearnerId("learner"), Moment(1), SessionPolicy()
        )
        assertEquals(
            EvaluativeRatingAvailability.NO_ACTIVE_ITEM,
            EvaluativeRatingAvailabilityResolver.resolve(evaluative)
        )
        assertEquals(
            EvaluativeRatingAvailability.AVAILABLE,
            EvaluativeRatingAvailabilityResolver.resolve(
                evaluative.presentItem(LearningItemId("item"), Moment(2))
            )
        )
        val practice = StudySession.start(
            SessionId("practice"), LearnerId("learner"), Moment(1),
            SessionPolicy(
                evaluationPolicy = SessionEvaluationPolicy.PRACTICE_ONLY,
                practiceLoopPolicy = PracticeLoopPolicy.LOOP_FIXED_MEMBERSHIP_SHUFFLED
            )
        ).presentItem(LearningItemId("item"), Moment(2))
        assertEquals(
            EvaluativeRatingAvailability.NOT_EVALUATIVE,
            EvaluativeRatingAvailabilityResolver.resolve(practice)
        )
    }

    @Test
    fun `direct evaluative rating commits user provenance through normal transaction`() {
        val sessions = InMemoryStudySessionRepository()
        val items = InMemoryLearningItemRepository()
        val memories = InMemoryMemoryStateRepository()
        val events = InMemoryReviewEventRepository()
        val item = LearningItem(
            LearningItemId("manual-item"), ContentId("manual-content"), LearningMode.MEANING_RECALL
        )
        items.save(item)
        val sessionId = SessionId("manual-session")
        sessions.save(
            StudySession.start(sessionId, LearnerId("learner"), Moment(1), SessionPolicy())
                .presentItem(item.id, Moment(2))
        )
        val result = ReviewSessionItemUseCase(
            sessions,
            items,
            ReviewLearningItemUseCase(memories, events, SimpleScheduler()),
            object : TransactionRunner {
                override fun <T> runInTransaction(block: () -> T): T = block()
            }
        ).execute(
            ReviewSessionItemCommand(
                sessionId,
                ReviewEventId("manual-event"),
                item.id,
                ReviewRating.HARD,
                Moment(10),
                ratingSource = RatingSource.MANUAL_USER
            )
        )

        assertEquals(RatingSource.MANUAL_USER, result.reviewResult.reviewEvent.source)
        assertEquals(ReviewRating.HARD, result.reviewResult.reviewEvent.rating)
        assertEquals(1, result.session.totalReviews)
    }
}
