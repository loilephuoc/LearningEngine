package vn.loi.learning.application.recall

import kotlin.test.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.*

class AdaptiveRecallStrategyTest {
    private val strategy = AdaptiveRecallStrategy()

    @Test fun `selection and fallback only contain eligible modes with supported directions`() {
        val projection = projection(audio = true, image = true, example = true)
        val decision = selected(request(projection = projection))
        (listOf(RecallStrategyCandidate(decision.selectedMode, decision.selectedDirection, RecallModeStrength.STRONG_RECALL, emptyList())) +
            decision.fallbackCandidates).forEach {
            assertTrue(projection.supports(it.mode)); assertContains(projection.supportedDirectionsFor(it.mode), it.direction)
        }
        assertEquals(decision.fallbackCandidates.map { it.mode }.distinct(), decision.fallbackCandidates.map { it.mode })
    }

    @Test fun `empty projection returns typed no eligible mode`() {
        assertIs<RecallStrategyDecisionResult.NoEligibleMode>(strategy.select(request(projection = projection(target = false))))
    }

    @Test fun `same input and seed gives identical decision and stable fallback order`() {
        val input = request(projection = projection(audio = true, image = true, example = true), seed = 71)
        assertEquals(strategy.select(input), strategy.select(input))
    }

    @Test fun `low or missing intelligence uses safe typing fallback`() {
        val decision = selected(request())
        assertEquals(RecallMode.TYPING, decision.selectedMode)
        assertEquals(RecallDirection.TARGET_TO_SOURCE, decision.selectedDirection)
        assertEquals(RecallStrategyConfidence.LOW, decision.confidence)
        assertEquals(RecallStrategyReason.SAFE_FALLBACK, decision.primaryReason)
    }

    @Test fun `text modes retain distinct direction authority`() {
        val decision = selected(request())
        assertEquals(RecallDirection.TARGET_TO_SOURCE, decision.selectedDirection)
        val reverse = decision.fallbackCandidates.single { it.mode == RecallMode.REVERSE_TRANSLATION }
        assertEquals(RecallDirection.SOURCE_TO_TARGET, reverse.direction)
        val multipleChoice = decision.fallbackCandidates.single { it.mode == RecallMode.MULTIPLE_CHOICE }
        assertEquals(RecallDirection.SOURCE_TO_TARGET, multipleChoice.direction)
    }

    @Test fun `very difficult and needs evidence prefer strong recall`() {
        listOf(RecommendationAction.FOCUS_PRACTICE, RecommendationAction.NEEDS_MORE_EVIDENCE,
            RecommendationAction.READY_FOR_PROMOTION, RecommendationAction.RECOVERY_NEEDED).forEach { action ->
            val decision = selected(request(profile = profile(DifficultyLevel.VERY_DIFFICULT), recommendation = recommendation(action)))
            assertEquals(RecallModeStrength.STRONG_RECALL, strength(decision.selectedMode))
            assertNotEquals(RecallMode.MULTIPLE_CHOICE, decision.selectedMode)
        }
    }

    @Test fun `unstable recent lapse does not choose recognition when strong recall exists`() {
        val context = RecallPromotionEvidenceContext(RecallStrategyContext.EVALUATIVE, recentLapse = true)
        val decision = selected(request(profile = profile(DifficultyLevel.UNSTABLE), recommendation = recommendation(), context = context))
        assertNotEquals(RecallMode.MULTIPLE_CHOICE, decision.selectedMode)
        assertContains(decision.secondaryReasons, RecallStrategyReason.RECENT_LAPSE)
    }

    @Test fun `stable context diversifies after repeated typing`() {
        val history = RecallModeHistory(List(2) { RecallModeHistoryEntry(RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET, usedAt = Moment((it + 1).toLong())) }, 8)
        val decision = selected(request(profile = profile(DifficultyLevel.STABLE), recommendation = recommendation(), history = history))
        assertNotEquals(RecallMode.TYPING, decision.selectedMode)
    }

    @Test fun `recovery exemption permits repeated strong mode`() {
        val history = RecallModeHistory(List(3) { RecallModeHistoryEntry(RecallMode.TYPING, RecallDirection.SOURCE_TO_TARGET, usedAt = Moment((it + 1).toLong())) }, 8)
        val decision = selected(request(profile = profile(DifficultyLevel.UNSTABLE), recommendation = recommendation(RecommendationAction.RECOVERY_NEEDED), history = history))
        assertEquals(RecallMode.TYPING, decision.selectedMode)
    }

    @Test fun `mastered context can choose efficient recognition mode`() {
        val decision = selected(request(profile = profile(DifficultyLevel.MASTERED), recommendation = recommendation(RecommendationAction.MASTERED)))
        assertEquals(RecallMode.MULTIPLE_CHOICE, decision.selectedMode)
    }

    @Test fun `practice and evaluative policy produce distinct ranking`() {
        val p = AdaptiveRecallStrategyPolicy(allowRecognitionInEvaluative = false)
        val evaluative = selected(request(profile = profile(DifficultyLevel.MASTERED), recommendation = recommendation(RecommendationAction.MASTERED), policy = p))
        val practice = selected(request(profile = profile(DifficultyLevel.MASTERED), recommendation = recommendation(RecommendationAction.MASTERED), context = RecallPromotionEvidenceContext(RecallStrategyContext.PRACTICE_ONLY)))
        assertNotEquals(RecallMode.MULTIPLE_CHOICE, evaluative.selectedMode)
        assertEquals(RecallMode.MULTIPLE_CHOICE, practice.selectedMode)
    }

    @Test fun `identity mismatch is invalid and does not create decision`() {
        val mismatched = profile().copy(contentId = ContentId("other"))
        val result = strategy.select(request(profile = mismatched, recommendation = recommendation()))
        assertEquals(listOf(RecallStrategyViolation.IDENTITY_MISMATCH), assertIs<RecallStrategyDecisionResult.InvalidRequest>(result).violations)
    }

    @Test fun `rejected candidates use typed capability reasons`() {
        val decision = selected(request())
        assertTrue(decision.rejectedCandidates.isNotEmpty())
        assertTrue(decision.rejectedCandidates.all { it.reasons == listOf(RecallStrategyReason.CAPABILITY_UNAVAILABLE) })
    }

    @Test fun `wire identifiers are stable and never derived from ordinal`() {
        assertEquals("strong-recall", RecallModeStrength.STRONG_RECALL.wireId)
        assertEquals("practice-only", RecallStrategyContext.PRACTICE_ONLY.wireId)
        assertEquals("safe-fallback", RecallStrategyReason.SAFE_FALLBACK.wireId)
    }

    @Test fun `policy rejects invalid thresholds and incomplete strength mapping`() {
        assertFailsWith<IllegalArgumentException> { RecallModeDiversityPolicy(maxConsecutiveSameMode = 0) }
        assertFailsWith<IllegalArgumentException> { AdaptiveRecallStrategyPolicy(baseScores = emptyMap()) }
    }

    private fun selected(request: RecallStrategyRequest) = assertIs<RecallStrategyDecisionResult.Selected>(strategy.select(request)).decision
    private fun strength(mode: RecallMode) = when (mode) {
        RecallMode.TYPING, RecallMode.DICTATION, RecallMode.LISTENING -> RecallModeStrength.STRONG_RECALL
        RecallMode.REVERSE_TRANSLATION, RecallMode.IMAGE_RECALL -> RecallModeStrength.STANDARD_RECALL
        RecallMode.EXAMPLE_COMPLETION -> RecallModeStrength.ASSISTED_CONTEXT
        RecallMode.MULTIPLE_CHOICE -> RecallModeStrength.RECOGNITION
    }

    private fun request(
        projection: RecallCapabilityProjection = projection(), profile: LearningDifficultyProfile? = null,
        recommendation: LearningRecommendation? = null,
        context: RecallPromotionEvidenceContext = RecallPromotionEvidenceContext(RecallStrategyContext.EVALUATIVE),
        history: RecallModeHistory = RecallModeHistory.empty(), policy: AdaptiveRecallStrategyPolicy = AdaptiveRecallStrategyPolicy(), seed: Long = 1
    ) = RecallStrategyRequest(LearnerId("learner"), ContentId("content"), projection, profile, recommendation,
        context, history, policy, RecallDeterministicSeed(seed), Moment(2_000))

    private fun projection(target: Boolean = true, audio: Boolean = false, image: Boolean = false, example: Boolean = false): RecallCapabilityProjection {
        val text = ContentText("word", if (target) "translation" else null, exampleText = if (example) "A word appears." else null)
        val media = ContentMedia(primaryAudio = if (audio) "asset:audio" else null, image = if (image) "asset:image" else null)
        return ContentRecallCapabilityResolver.resolve(Content(ContentId("content"), ContentType.WORD, text, media))
    }

    private fun recommendation(action: RecommendationAction = RecommendationAction.NORMAL_REVIEW) = LearningRecommendation(
        LearnerId("learner"), ContentId("content"), Moment(1_000), action, RecommendationCategory.MAINTENANCE,
        RecommendationPriority.NORMAL, RecommendationConfidence.HIGH, listOf(RecommendationReason.STABLE_RETENTION)
    )

    private fun profile(level: DifficultyLevel = DifficultyLevel.STABLE) = LearningDifficultyProfile(
        LearnerId("learner"), ContentId("content"), Moment(1_000),
        LifetimeStatistics(Moment(100), Moment(900), 12, 1, 2, 6, 3, 1, 1, 12),
        CurrentStability(RetentionScore(.8), StabilityScore(.7), EvidenceConfidenceScore(.8), DifficultyScore(.3), DifficultyTrend.PLATEAU),
        PromotionAnalytics(null, AverageCount(3.0), AverageCount(2.0), emptyMap()),
        RiskProfile(RiskScore(.2), RiskScore(.2), ReadinessScore(.5)), DifficultyConfidence.HIGH, DifficultyTrend.PLATEAU, level
    )
}
