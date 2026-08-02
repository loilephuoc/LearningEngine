package vn.loi.learning.infrastructure.persistence

import java.nio.file.Files
import kotlin.test.*
import org.junit.jupiter.api.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.persistence.json.JsonLearningTrajectoryStore
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedLearningTrajectoryRepository

class LearningTrajectoryPersistenceTest {
    @Test fun `trajectory anchor evidence and closed chains survive restart`() {
        val path = Files.createTempDirectory("trajectory-test").resolve("learning-trajectories.json")
        val learner = LearnerId("learner")
        val content = ContentId("content")
        val first = evidence("again", content, ReviewRating.AGAIN, 1)
        val initial = LearningTrajectory.start(EvidenceChain.start(content, PromotionStage.AGAIN_TO_HARD,
            ChainAnchor(ReviewRating.AGAIN, first.timestamp, first, ChainAnchorReason.INITIAL_RATING)))
        val promoted = evidence("hard", content, ReviewRating.HARD, 2)
        val trajectory = initial.reset(
            ChainAnchor(ReviewRating.HARD, promoted.timestamp, promoted, ChainAnchorReason.PROMOTED),
            ChainResetReason.PROMOTED)
        StoreBackedLearningTrajectoryRepository(JsonLearningTrajectoryStore(path)).save(learner, trajectory)

        val restored = StoreBackedLearningTrajectoryRepository(JsonLearningTrajectoryStore(path)).find(learner, content)
        assertEquals(trajectory, restored)
    }

    @Test fun `missing trajectory file is a backward compatible empty store`() {
        val path = Files.createTempDirectory("trajectory-empty").resolve("missing.json")
        assertNull(StoreBackedLearningTrajectoryRepository(JsonLearningTrajectoryStore(path))
            .find(LearnerId("learner"), ContentId("content")))
        assertFalse(Files.exists(path))
    }

    private fun evidence(id: String, content: ContentId, rating: ReviewRating, at: Long) = RecallEvidence(
        ReviewEventId(id), content, Moment(at), rating, RecallResult.CORRECT, SessionId("session-$id"),
        SessionEvaluationPolicy.EVALUATIVE, RatingSource.STANDARD_REVIEW, false, Moment(at), TimeSpan(3)
    )
}
