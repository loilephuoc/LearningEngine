package vn.loi.learning.desktop.ui.study

import kotlin.test.*
import vn.loi.learning.application.learninginsight.*
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.DifficultyConfidence
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan

class LearningInsightPresentationTest {
    @Test fun `answer side state renders shared insight`() {
        assertSame(bundle, StudyUiState(canReview = true, learningInsight = bundle).learningInsight)
    }

    @Test fun `front typing state has no answer side insight`() {
        assertNull(StudyUiState(canRevealAnswer = true).learningInsight)
    }

    @Test fun `practice copy explicitly excludes rating and promotion evidence`() {
        val practice = bundleWith(
            LearningInsightTitle.PRACTICE_DOES_NOT_CHANGE_RATING,
            LearningInsightSummary.PRACTICE_IS_NOT_PROMOTION_EVIDENCE
        )
        val text = LearningInsightPresentationMapper.map(practice, LearningInsightStrings.ENGLISH)
        assertContains(text.title.lowercase(), "rating")
        assertContains(text.summary.lowercase(), "promotion")
    }

    @Test fun `compact and short layouts keep metrics bounded`() {
        assertEquals(1, LearningInsightViewportPolicy.visibleMetricCount(900, 500))
        assertEquals(2, LearningInsightViewportPolicy.visibleMetricCount(600, 900))
        assertEquals(3, LearningInsightViewportPolicy.visibleMetricCount(900, 900))
    }

    @Test fun `accessibility text follows heading title summary metric order`() {
        val p = LearningInsightPresentationMapper.map(bundle, LearningInsightStrings.ENGLISH)
        assertTrue(p.accessibilityText.indexOf(p.heading) < p.accessibilityText.indexOf(p.title))
        assertTrue(p.accessibilityText.indexOf(p.title) < p.accessibilityText.indexOf(p.summary))
        assertTrue(p.accessibilityText.indexOf(p.summary) < p.accessibilityText.indexOf(p.metrics.first()))
    }

    @Test fun `Vietnamese and English maps cover every typed value`() {
        listOf(LearningInsightStrings.ENGLISH, LearningInsightStrings.VIETNAMESE).forEach { strings ->
            assertEquals(LearningInsightTitle.entries.toSet(), strings.titles.keys)
            assertEquals(LearningInsightSummary.entries.toSet(), strings.summaries.keys)
            assertEquals(LearningInsightMetricKind.entries.toSet(), strings.metrics.keys)
        }
    }

    @Test fun `presentation follows shared category without deriving policy`() {
        val p = LearningInsightPresentationMapper.map(bundle, LearningInsightStrings.ENGLISH)
        assertEquals("More evidence needed", p.title)
        assertEquals("Engine confidence low.", p.summary)
    }

    @Test fun `presentation and disclosure policy do not mutate learning state`() {
        val before = bundle.copy()
        repeat(3) { LearningInsightPresentationMapper.map(bundle, LearningInsightStrings.ENGLISH) }
        assertEquals(before, bundle)
    }

    private fun bundleWith(title: LearningInsightTitle, summary: LearningInsightSummary) =
        LearningInsightBundle(insight.copy(title = title, summary = summary), emptyList())

    private val insight = LearningInsight(
        learnerId = LearnerId("learner"), contentId = ContentId("content"),
        category = LearningInsightCategory.INSUFFICIENT_DATA,
        severity = LearningInsightSeverity.INFO,
        title = LearningInsightTitle.MORE_EVIDENCE_NEEDED,
        summary = LearningInsightSummary.ENGINE_CONFIDENCE_LOW,
        difficultyLevel = null, trend = null, engineConfidence = DifficultyConfidence.LOW,
        recommendedAction = null, recommendationPriority = null, recommendationConfidence = null,
        recommendationReasons = emptyList(), promotionTarget = null, promotionEligibility = null,
        promotionReasons = emptyList(), remainingDuration = TimeSpan(0), missingRecallCount = 0,
        supportingMetrics = listOf(
            LearningInsightMetric(
                LearningInsightMetricKind.EVIDENCE_CONFIDENCE,
                LearningInsightMetricValue.Score(0.25), MetricDirection.HIGHER_IS_BETTER
            )
        ),
        generatedAt = Moment(123)
    )
    private val bundle = LearningInsightBundle(insight, emptyList())
}
