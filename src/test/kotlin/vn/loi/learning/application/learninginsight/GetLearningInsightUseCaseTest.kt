package vn.loi.learning.application.learninginsight

import kotlin.test.*
import org.junit.jupiter.api.Test
import vn.loi.learning.application.port.LearningTrajectoryRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.*

class GetLearningInsightUseCaseTest {
    private val learner = LearnerId("learner")
    private val content = ContentId("content")
    private val clock = EvidenceClock { Moment(100_000) }

    @Test fun `missing evaluative trajectory returns typed not found`() {
        val result = useCase(FakeRepository()).execute(GetLearningInsightQuery(learner, content))
        assertEquals(GetLearningInsightResult.NotFound(learner, content), result)
    }

    @Test fun `missing practice trajectory returns typed context-only insufficient result`() {
        val result = assertIs<GetLearningInsightResult.InsufficientData>(
            useCase(FakeRepository()).execute(GetLearningInsightQuery(
                learner, content, context = LearningInsightContext(LearningInsightMode.PRACTICE_ONLY)
            ))
        )
        assertEquals(LearningInsightTitle.PRACTICE_DOES_NOT_CHANGE_RATING, result.insight.primary.title)
        assertEquals(Moment(100_000), result.insight.primary.generatedAt)
    }

    @Test fun `low evidence trajectory returns typed insufficient data`() {
        val result = useCase(FakeRepository(trajectory(1))).execute(GetLearningInsightQuery(learner, content))
        assertIs<GetLearningInsightResult.InsufficientData>(result)
        assertEquals(LearningInsightCategory.INSUFFICIENT_DATA, result.insight.primary.category)
    }

    @Test fun `long trajectory composes profile strategy and projection without mutation`() {
        val repository = FakeRepository(trajectory(12))
        val result = assertIs<GetLearningInsightResult.Success>(
            useCase(repository).execute(GetLearningInsightQuery(learner, content))
        )
        assertEquals(learner, result.insight.primary.learnerId)
        assertEquals(content, result.insight.primary.contentId)
        assertEquals(0, repository.writeCount)
    }

    @Test fun `sibling consumers of one Content resolve the same insight source deterministically`() {
        val useCase = useCase(FakeRepository(trajectory(12)))
        val first = useCase.execute(GetLearningInsightQuery(learner, content))
        val second = useCase.execute(GetLearningInsightQuery(learner, content))
        assertEquals(first, second)
    }

    private fun useCase(repository: FakeRepository) = GetLearningInsightUseCase(
        repository, LearningDifficultyProfileCalculator(clock), AdaptiveLearningStrategy(),
        LearningInsightProjector(), clock
    )

    private fun trajectory(count: Int): LearningTrajectory {
        val recalls = List(count) { index ->
            RecallEvidence(
                ReviewEventId("event-$index"), content, Moment((index + 1L) * 1_000), ReviewRating.GOOD,
                RecallResult.CORRECT, SessionId("session-$index"), SessionEvaluationPolicy.EVALUATIVE,
                RatingSource.STANDARD_REVIEW, false, Moment((index + 1L) * 1_000)
            )
        }
        val anchor = ChainAnchor(ReviewRating.GOOD, recalls.first().timestamp, recalls.first(),
            ChainAnchorReason.INITIAL_RATING)
        return LearningTrajectory.start(EvidenceChain.reconstitute(
            content, PromotionStage.GOOD_TO_EASY, anchor,
            recalls.map(EvidenceSequenceEntry::Recall), null
        ))
    }

    private inner class FakeRepository(private val trajectory: LearningTrajectory? = null) :
        LearningTrajectoryRepository {
        var writeCount = 0
        override fun find(learnerId: LearnerId, contentId: ContentId) = trajectory
        override fun save(learnerId: LearnerId, trajectory: LearningTrajectory) { writeCount++ }
        override fun delete(learnerId: LearnerId, contentId: ContentId) { writeCount++ }
    }
}
