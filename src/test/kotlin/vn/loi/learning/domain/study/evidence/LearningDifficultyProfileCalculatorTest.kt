package vn.loi.learning.domain.study.evidence

import kotlin.test.*
import org.junit.jupiter.api.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.*

class LearningDifficultyProfileCalculatorTest {
    private val learner = LearnerId("learner")
    private val content = ContentId("content")
    private var now = hour(1_000)
    private val calculator = LearningDifficultyProfileCalculator(EvidenceClock { now })

    @Test fun `lifetime statistics span every chain and never reset`() {
        val trajectory = trajectory(
            chain(ReviewRating.AGAIN, listOf(false, true), 0, ChainResetReason.PROMOTED),
            chain(ReviewRating.HARD, listOf(true, true), 24, ChainResetReason.NEW_LAPSE),
            chain(ReviewRating.AGAIN, listOf(false, true), 48)
        )
        val lifetime = calculator.calculateDifficultyProfile(learner, trajectory).lifetime
        assertEquals(6, lifetime.totalRecall)
        assertEquals(4, lifetime.totalAgain)
        assertEquals(2, lifetime.totalHard)
        assertEquals(1, lifetime.totalPromotion)
        assertEquals(1, lifetime.totalLapse)
        assertEquals(hour(0), lifetime.firstSeen)
        assertEquals(hour(49), lifetime.lastSeen)
    }

    @Test fun `new trajectory has low engine confidence and typed bounded current scores`() {
        now = hour(2)
        val profile = calculator.calculateDifficultyProfile(
            learner, trajectory(chain(ReviewRating.GOOD, listOf(true), 0))
        )
        assertEquals(DifficultyConfidence.LOW, profile.confidence)
        assertTrue(profile.current.retentionScore.value in 0.0..1.0)
        assertTrue(profile.current.stabilityScore.value in 0.0..1.0)
        assertTrue(profile.risk.forgetRisk.value in 0.0..1.0)
        assertEquals(DifficultyTrend.PLATEAU, profile.trend)
    }

    @Test fun `long trajectory reaches high confidence`() {
        val profile = calculator.calculateDifficultyProfile(
            learner, trajectory(chain(ReviewRating.HARD, List(12) { true }, 0))
        )
        assertEquals(DifficultyConfidence.HIGH, profile.confidence)
        assertEquals(1.0, profile.current.evidenceConfidence.value)
    }

    @Test fun `many incorrect Again recalls are very difficult with high relapse risk`() {
        val difficult = calculator.calculateDifficultyProfile(
            learner, trajectory(chain(ReviewRating.AGAIN, List(12) { false }, 0))
        )
        val stable = calculator.calculateDifficultyProfile(
            learner, trajectory(chain(ReviewRating.GOOD, List(12) { true }, 0))
        )
        assertEquals(DifficultyLevel.VERY_DIFFICULT, difficult.level)
        assertTrue(difficult.risk.relapseRisk.value > stable.risk.relapseRisk.value)
        assertTrue(difficult.risk.promotionReadiness.value < stable.risk.promotionReadiness.value)
    }

    @Test fun `long promoted stable trajectory becomes mastered independent of final rating label`() {
        val trajectory = trajectory(
            chain(ReviewRating.AGAIN, List(4) { true }, 0, ChainResetReason.PROMOTED),
            chain(ReviewRating.HARD, List(4) { true }, 24, ChainResetReason.PROMOTED),
            chain(ReviewRating.GOOD, List(4) { true }, 96, ChainResetReason.PROMOTED),
            chain(ReviewRating.EASY, List(4) { true }, 432)
        )
        now = hour(440)
        assertEquals(DifficultyLevel.MASTERED, calculator.calculateDifficultyProfile(learner, trajectory).level)
    }

    @Test fun `Easy anchor can remain unstable when recall evidence declines`() {
        val profile = calculator.calculateDifficultyProfile(
            learner, trajectory(chain(ReviewRating.EASY, listOf(true, true, true, false, false, false), 0))
        )
        assertEquals(DifficultyTrend.DECLINING, profile.trend)
        assertNotEquals(DifficultyLevel.MASTERED, profile.level)
    }

    @Test fun `trend distinguishes improving recovering plateau declining and regressing`() {
        assertEquals(DifficultyTrend.IMPROVING, profile(listOf(false, false, false, true, true, true)).trend)
        assertEquals(DifficultyTrend.PLATEAU, profile(List(6) { true }).trend)
        assertEquals(DifficultyTrend.DECLINING, profile(listOf(true, true, true, false, false, false)).trend)
        val recovering = trajectory(
            chain(ReviewRating.GOOD, listOf(false), 0, ChainResetReason.NEW_LAPSE),
            chain(ReviewRating.AGAIN, List(4) { true }, 10)
        )
        assertEquals(DifficultyTrend.RECOVERING, calculator.calculateDifficultyProfile(learner, recovering).trend)
        val regressing = trajectory(
            chain(ReviewRating.GOOD, listOf(true, true, true), 0, ChainResetReason.NEW_LAPSE),
            chain(ReviewRating.AGAIN, listOf(false, false, false), 10)
        )
        assertEquals(DifficultyTrend.REGRESSING, calculator.calculateDifficultyProfile(learner, regressing).trend)
    }

    @Test fun `promotion analytics derive generic duration length and per-stage map`() {
        val profile = calculator.calculateDifficultyProfile(
            learner,
            trajectory(
                chain(ReviewRating.AGAIN, listOf(false, true), 0, ChainResetReason.PROMOTED),
                chain(ReviewRating.HARD, listOf(true, true, true), 24, ChainResetReason.PROMOTED),
                chain(ReviewRating.GOOD, listOf(true), 96)
            )
        )
        assertEquals(TimeSpan.hours(48), profile.promotionAnalytics.averagePromotionDuration)
        assertEquals(2.5, profile.promotionAnalytics.averageRecallBetweenPromotions.value)
        assertEquals(setOf(PromotionStage.AGAIN_TO_HARD, PromotionStage.HARD_TO_GOOD),
            profile.promotionAnalytics.byStage.keys)
    }

    @Test fun `profile identity is learner plus Content and sibling consumers derive one result`() {
        val shared = trajectory(chain(ReviewRating.HARD, List(5) { true }, 0))
        val firstSibling = calculator.calculateDifficultyProfile(learner, shared)
        val secondSibling = calculator.calculateDifficultyProfile(learner, shared)
        assertEquals(firstSibling, secondSibling)
        assertEquals(content, firstSibling.contentId)
        assertEquals(learner, firstSibling.learnerId)
    }

    @Test fun `fake clock deterministically increases forget risk without changing lifetime`() {
        val trajectory = trajectory(chain(ReviewRating.GOOD, List(6) { true }, 0))
        now = hour(6)
        val fresh = calculator.calculateDifficultyProfile(learner, trajectory)
        now = hour(6 + 24 * 30)
        val stale = calculator.calculateDifficultyProfile(learner, trajectory)
        assertEquals(fresh.lifetime, stale.lifetime)
        assertTrue(stale.risk.forgetRisk.value > fresh.risk.forgetRisk.value)
    }

    private fun profile(results: List<Boolean>) = calculator.calculateDifficultyProfile(
        learner, trajectory(chain(ReviewRating.GOOD, results, 0))
    )

    private fun trajectory(vararg chains: EvidenceChain) = LearningTrajectory.reconstitute(content, chains.toList())

    private fun chain(
        rating: ReviewRating,
        results: List<Boolean>,
        startHour: Int,
        closedBy: ChainResetReason? = null
    ): EvidenceChain {
        val evidence = results.mapIndexed { index, correct ->
            evidence("$startHour-$index", startHour + index, rating,
                if (correct) RecallResult.CORRECT else RecallResult.INCORRECT)
        }
        val anchorReason = when (closedBy) {
            ChainResetReason.NEW_AGAIN -> ChainAnchorReason.NEW_AGAIN
            ChainResetReason.NEW_LAPSE -> ChainAnchorReason.NEW_LAPSE
            else -> if (startHour == 0) ChainAnchorReason.INITIAL_RATING else ChainAnchorReason.PROMOTED
        }
        val anchor = ChainAnchor(rating, evidence.first().timestamp, evidence.first(), anchorReason)
        return EvidenceChain.reconstitute(
            content, PromotionStage.fromAnchorRating(rating), anchor,
            evidence.map(EvidenceSequenceEntry::Recall), closedBy
        )
    }

    private fun evidence(id: String, at: Int, rating: ReviewRating, result: RecallResult) = RecallEvidence(
        ReviewEventId(id), content, hour(at), rating, result, SessionId("session-$id"),
        SessionEvaluationPolicy.EVALUATIVE, RatingSource.STANDARD_REVIEW, false, hour(at)
    )

    private fun hour(value: Int) = Moment(value * 3_600_000L)
}
