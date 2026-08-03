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
        assertEquals(RecallDirection.TARGET_TO_SOURCE, result.plan.direction)
        assertEquals(target.text.translatedText, assertIs<RecallPrompt.Typing>(result.plan.prompt).sourceText)
        assertEquals("word", result.plan.answerContract.canonicalAnswer)
        assertTrue(result.fallbackFailures.isEmpty())
    }

    @Test
    fun `production typing preserves representative target cues and English source answers`() {
        listOf(
            content("bed", "bed", "cái giường"),
            content("soap", "soap", "xà phòng"),
            content("multi", "  make the bed!  ", "dọn giường")
        ).forEach { content ->
            val result = assertIs<ProductionRecallPlanResult.Created>(
                resolver.resolve(request(content = content, contents = listOf(content)))
            )
            assertEquals(RecallDirection.TARGET_TO_SOURCE, result.plan.direction)
            assertEquals(content.text.translatedText, assertIs<RecallPrompt.Typing>(result.plan.prompt).sourceText)
            assertEquals(content.text.primaryText, result.plan.answerContract.canonicalAnswer)
        }
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

    @Test
    fun `production strategy can resolve Listening when word audio capability is available`() {
        val listeningContent = Content(
            ContentId("listening-target"),
            ContentType.WORD,
            ContentText("heard word"),
            ContentMedia(primaryAudio = "asset:word-audio")
        )
        val resolved = (1L..128L).map { seed ->
            val base = request(content = listeningContent, contents = listOf(listeningContent))
            resolver.resolve(base.copy(deterministicSeed = RecallDeterministicSeed(seed)))
        }.filterIsInstance<ProductionRecallPlanResult.Created>()
            .firstOrNull { it.plan.mode == RecallMode.LISTENING }

        val result = requireNotNull(resolved)
        assertEquals(RecallMode.LISTENING, result.requestedMode)
        assertEquals(RecallMode.LISTENING, result.resolvedMode)
        assertIs<RecallPrompt.Listening>(result.plan.prompt)
        assertTrue(result.plan.platformRequirements.requiresAudioPlayback)
        assertTrue(result.plan.platformRequirements.requiresTextInput)
    }

    @Test
    fun `production strategy can resolve Image Recall when image capability is available`() {
        val imageContent = Content(
            ContentId("image-target"),
            ContentType.WORD,
            ContentText("pictured word"),
            ContentMedia(image = "asset:image")
        )
        val resolved = (1L..128L).map { seed ->
            val base = request(content = imageContent, contents = listOf(imageContent))
            resolver.resolve(base.copy(deterministicSeed = RecallDeterministicSeed(seed)))
        }.filterIsInstance<ProductionRecallPlanResult.Created>()
            .firstOrNull { it.plan.mode == RecallMode.IMAGE_RECALL }

        val result = requireNotNull(resolved)
        assertEquals(RecallMode.IMAGE_RECALL, result.requestedMode)
        assertEquals(RecallMode.IMAGE_RECALL, result.resolvedMode)
        assertIs<RecallPrompt.ImageRecall>(result.plan.prompt)
        assertTrue(result.plan.platformRequirements.requiresImageRendering)
        assertTrue(result.plan.platformRequirements.requiresTextInput)
    }

    @Test
    fun `production strategy can resolve Example Completion from one safe target span`() {
        val contextualContent = Content(
            ContentId("example-target"),
            ContentType.WORD,
            ContentText("word", exampleText = "A word appears."),
            ContentMedia()
        )
        val resolved = (1L..128L).map { seed ->
            val base = request(content = contextualContent, contents = listOf(contextualContent))
            resolver.resolve(base.copy(deterministicSeed = RecallDeterministicSeed(seed)))
        }.filterIsInstance<ProductionRecallPlanResult.Created>()
            .firstOrNull { it.plan.mode == RecallMode.EXAMPLE_COMPLETION }

        val result = requireNotNull(resolved)
        val prompt = assertIs<RecallPrompt.ExampleCompletion>(result.plan.prompt)
        assertEquals(RecallMode.EXAMPLE_COMPLETION, result.requestedMode)
        assertEquals(RecallMode.EXAMPLE_COMPLETION, result.resolvedMode)
        assertEquals("A ____ appears.", prompt.example)
        assertEquals(RecallTextSpan(2, 6), prompt.targetSpan)
        assertTrue(result.plan.platformRequirements.requiresExampleRendering)
        assertTrue(result.plan.platformRequirements.requiresTextInput)
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
