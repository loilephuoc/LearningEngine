package vn.loi.learning.application.recall

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.*
import vn.loi.learning.domain.study.session.model.SessionId

class ProductionRecallPlanResolverTest {
    private val resolver = ProductionRecallPlanResolver()

    @Test
    fun `production strategy typing creates typing plan`() {
        val result = assertIs<ProductionRecallPlanResult.Created>(resolver.resolve(request()))

        assertEquals(RecallMode.TYPING, result.requestedMode)
        assertEquals(RecallMode.TYPING, result.resolvedMode)
        assertEquals(RecallMode.TYPING, result.plan.mode)
        assertTrue(result.fallbackFailures.isEmpty())
    }

    @Test
    fun `production strategy multiple choice creates deterministic multiple choice plan`() {
        val request = request(
            contents = contents(),
            profile = profile(DifficultyLevel.MASTERED),
            recommendation = recommendation(RecommendationAction.MONITOR_ONLY)
        )
        val first = assertIs<ProductionRecallPlanResult.Created>(resolver.resolve(request))
        val second = assertIs<ProductionRecallPlanResult.Created>(resolver.resolve(request))

        assertEquals(RecallMode.MULTIPLE_CHOICE, first.requestedMode)
        assertEquals(RecallMode.MULTIPLE_CHOICE, first.resolvedMode)
        assertEquals(first.plan, second.plan)
        assertEquals(
            assertIs<RecallPrompt.MultipleChoice>(first.plan.prompt).choices,
            assertIs<RecallPrompt.MultipleChoice>(second.plan.prompt).choices
        )
    }

    @Test
    fun `insufficient multiple choice inventory follows ordered typed fallback with provenance`() {
        val result = assertIs<ProductionRecallPlanResult.Created>(
            resolver.resolve(request(policy = recognitionFirstPolicy(), contents = listOf(target)))
        )

        assertEquals(RecallMode.MULTIPLE_CHOICE, result.requestedMode)
        assertEquals(RecallMode.TYPING, result.resolvedMode)
        assertEquals(listOf(RecallMode.MULTIPLE_CHOICE), result.fallbackFailures.map { it.mode })
        assertIs<RecallPlanFactoryResult.GenerationFailed>(result.fallbackFailures.single().result)
    }

    @Test
    fun `unsupported content returns typed unavailable without a plan`() {
        val unsupported = Content(ContentId("unsupported"), ContentType.WORD, ContentText("word"))
        val result = assertIs<ProductionRecallPlanResult.Unavailable>(
            resolver.resolve(request(content = unsupported, contents = listOf(unsupported)))
        )

        assertIs<RecallStrategyDecisionResult.NoEligibleMode>(result.strategyResult)
        assertTrue(result.failures.isEmpty())
    }

    @Test
    fun `practice multiple choice remains evidence ineligible`() {
        val result = assertIs<ProductionRecallPlanResult.Created>(
            resolver.resolve(
                request(
                    policy = recognitionFirstPolicy(),
                    contents = contents(),
                    context = RecallStrategyContext.PRACTICE_ONLY
                )
            )
        )

        assertEquals(RecallMode.MULTIPLE_CHOICE, result.plan.mode)
        assertEquals(RecallEvidenceEligibility.INELIGIBLE, result.plan.evidenceClass)
        assertEquals(RecallProvenance.PRACTICE, result.plan.provenance)
    }

    private fun request(
        content: Content = target,
        contents: List<Content> = listOf(target),
        policy: AdaptiveRecallStrategyPolicy = AdaptiveRecallStrategyPolicy(),
        context: RecallStrategyContext = RecallStrategyContext.EVALUATIVE,
        profile: LearningDifficultyProfile? = null,
        recommendation: LearningRecommendation? = null
    ) = ProductionRecallPlanRequest(
        LearnerId("learner"),
        content,
        LearningItemId("item"),
        SessionId("session"),
        RecallAttemptNonce("attempt"),
        MultipleChoiceScopeId("scope"),
        contents,
        profile,
        recommendation,
        RecallPromotionEvidenceContext(context),
        strategyPolicy = policy,
        planPolicy = RecallPlanPolicy(
            sourceLanguage = RecallLanguageTag("en"),
            targetLanguage = RecallLanguageTag("vi")
        ),
        deterministicSeed = RecallDeterministicSeed(17),
        generatedAt = Moment(1_000)
    )

    private fun recognitionFirstPolicy() = AdaptiveRecallStrategyPolicy(
        baseScores = AdaptiveRecallStrategyPolicy().baseScores.toMutableMap().apply {
            this[RecallModeStrength.RECOGNITION] = 250
        }
    )

    private fun contents() = listOf(
        target,
        content("one", "one", "má»™t"),
        content("two", "two", "hai"),
        content("three", "three", "ba")
    )

    private fun content(id: String, source: String, target: String) =
        Content(ContentId(id), ContentType.WORD, ContentText(source, target))

    private fun recommendation(action: RecommendationAction) = LearningRecommendation(
        LearnerId("learner"), target.id, Moment(900), action, RecommendationCategory.MAINTENANCE,
        RecommendationPriority.LOW, RecommendationConfidence.HIGH,
        listOf(RecommendationReason.STABLE_RETENTION)
    )

    private fun profile(level: DifficultyLevel) = LearningDifficultyProfile(
        LearnerId("learner"), target.id, Moment(900),
        LifetimeStatistics(Moment(100), Moment(800), 12, 0, 1, 8, 3, 3, 0, 12),
        CurrentStability(
            RetentionScore(.9), StabilityScore(.9), EvidenceConfidenceScore(1.0),
            DifficultyScore(.1), DifficultyTrend.PLATEAU
        ),
        PromotionAnalytics(null, AverageCount(3.0), AverageCount(2.0), emptyMap()),
        RiskProfile(RiskScore(.1), RiskScore(.1), ReadinessScore(.8)),
        DifficultyConfidence.HIGH, DifficultyTrend.PLATEAU, level
    )

    private val target = content("target", "word", "tá»«")
}
