package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.packageprogress.StudyHeaderStatistics
import vn.loi.learning.application.packageprogress.StudyPackageLearningStatistics
import vn.loi.learning.application.packageprogress.StudySessionProgressStatistics
import vn.loi.learning.domain.study.memory.model.Moment

class StudyHeaderStatisticsPresentationTest {
    @Test
    fun `approved order contains exactly all eight metrics`() {
        val result = presentation()
        assertEquals(
            StudyHeaderMetricType.entries,
            result.metrics.map(StudyHeaderMetricPresentation::type)
        )
        assertEquals(
            listOf("Total", "New", "Review", "Due", "Again", "Hard", "Good", "Easy"),
            result.metrics.map(StudyHeaderMetricPresentation::label)
        )
    }

    @Test
    fun `values and fraction parts map without presentation arithmetic`() {
        val metrics = presentation().metrics.associateBy(StudyHeaderMetricPresentation::type)
        assertEquals("10", metrics.getValue(StudyHeaderMetricType.TOTAL).primaryValue)
        assertEquals("3", metrics.getValue(StudyHeaderMetricType.NEW).primaryValue)
        assertEquals("/20", metrics.getValue(StudyHeaderMetricType.NEW).secondaryValue)
        assertEquals("42", metrics.getValue(StudyHeaderMetricType.REVIEW).primaryValue)
        assertEquals("/100", metrics.getValue(StudyHeaderMetricType.REVIEW).secondaryValue)
        assertEquals("2", metrics.getValue(StudyHeaderMetricType.DUE).primaryValue)
        assertEquals("1", metrics.getValue(StudyHeaderMetricType.AGAIN).primaryValue)
        assertEquals("2", metrics.getValue(StudyHeaderMetricType.HARD).primaryValue)
        assertEquals("6", metrics.getValue(StudyHeaderMetricType.GOOD).primaryValue)
        assertEquals("1", metrics.getValue(StudyHeaderMetricType.EASY).primaryValue)
    }

    @Test
    fun `positive progress and counts are active while zero values are muted`() {
        val active = presentation().metrics.associateBy(StudyHeaderMetricPresentation::type)
        listOf(
            StudyHeaderMetricType.NEW,
            StudyHeaderMetricType.REVIEW,
            StudyHeaderMetricType.DUE,
            StudyHeaderMetricType.AGAIN,
            StudyHeaderMetricType.HARD,
            StudyHeaderMetricType.GOOD,
            StudyHeaderMetricType.EASY
        ).forEach { assertEquals(StudyMetricEmphasis.ACTIVE, active.getValue(it).emphasis, it.name) }
        assertEquals(
            StudyMetricEmphasis.NEUTRAL,
            active.getValue(StudyHeaderMetricType.TOTAL).emphasis
        )

        val zero = presentation(statistics(newCompleted = 0, reviewRemaining = 0, due = 0,
            again = 0, hard = 0, good = 0, easy = 0))
            .metrics.associateBy(StudyHeaderMetricPresentation::type)
        StudyHeaderMetricType.entries.filterNot { it == StudyHeaderMetricType.TOTAL }.forEach {
            assertEquals(StudyMetricEmphasis.MUTED, zero.getValue(it).emphasis, it.name)
        }
        assertEquals(StudyMetricEmphasis.NEUTRAL, zero.getValue(StudyHeaderMetricType.TOTAL).emphasis)
    }

    @Test
    fun `accessibility describes fractions by meaning and provides group summary`() {
        val result = presentation()
        val metrics = result.metrics.associateBy(StudyHeaderMetricPresentation::type)
        assertEquals(
            "Completed 3 of the 20 new-item session target.",
            metrics.getValue(StudyHeaderMetricType.NEW).accessibilityText
        )
        assertEquals(
            "42 review items remain from the 100 session target.",
            metrics.getValue(StudyHeaderMetricType.REVIEW).accessibilityText
        )
        assertEquals(
            "2 items are due now.",
            metrics.getValue(StudyHeaderMetricType.DUE).accessibilityText
        )
        assertTrue(result.accessibilityDescription.contains("10 learned items."))
        assertFalse(result.accessibilityDescription.contains("3/20"))
        assertFalse(result.accessibilityDescription.contains("42/100"))
    }

    @Test
    fun `last known good remains stable and absent data never renders false zero`() {
        val value = statistics()
        assertEquals(
            presentation(value),
            resolveStudyHeaderStatisticsPresentation(
                StudyHeaderStatisticsState.Unavailable(value),
                StudyStatisticsStrings.ENGLISH
            )
        )
        assertNull(resolveStudyHeaderStatisticsPresentation(
            StudyHeaderStatisticsState.Loading,
            StudyStatisticsStrings.ENGLISH
        ))
        assertNull(resolveStudyHeaderStatisticsPresentation(
            StudyHeaderStatisticsState.Unavailable(),
            StudyStatisticsStrings.ENGLISH
        ))
    }

    @Test
    fun `existing viewport authority selects one row or compact four plus four`() {
        assertEquals(
            StudyStatisticsLayoutPresentation(metricsPerRow = 8, showSubtitles = false),
            resolveStudyStatisticsLayout(StudyViewportClass.WIDE)
        )
        assertEquals(
            StudyStatisticsLayoutPresentation(metricsPerRow = 8, showSubtitles = false),
            resolveStudyStatisticsLayout(StudyViewportClass.STANDARD)
        )
        assertEquals(
            StudyStatisticsLayoutPresentation(metricsPerRow = 4, showSubtitles = false),
            resolveStudyStatisticsLayout(StudyViewportClass.COMPACT)
        )
    }

    @Test
    fun `dashboard uses theme tokens split fraction and stable non interactive layout`() {
        val source = source("StudyStatisticsDashboard.kt")
        StudyHeaderMetricType.entries.forEach {
            assertTrue(source.contains("StudyHeaderMetricType.${it.name}"), it.name)
        }
        listOf(
            "metricLabel", "metricValue", "metricSubtitle",
            "LETheme.colors", "LETheme.icons", "LETheme.spacing",
            "primaryValue", "secondaryValue", "VerticalDivider", "weight(1f)"
        ).forEach { assertTrue(source.contains(it), it) }
        listOf(
            "MaterialTheme.colorScheme", "Color(0x", "fontSize", "clickable",
            "horizontalScroll", "AnimatedContent", "Crossfade"
        ).forEach { assertFalse(source.contains(it), it) }
    }

    @Test
    fun `semantic families and icon roles are explicit for every metric`() {
        val source = source("StudyStatisticsDashboard.kt")
        listOf(
            "TOTAL -> colors.metricPurple",
            "NEW -> colors.metricGreen",
            "REVIEW -> colors.metricBlue",
            "DUE -> colors.metricOrange",
            "AGAIN -> colors.metricRed",
            "HARD -> colors.metricOrange",
            "GOOD -> colors.metricGreen",
            "EASY -> colors.metricBlue",
            "icons.StatisticsTotal", "icons.StatisticsNew", "icons.StatisticsReview",
            "icons.StatisticsDue", "icons.StatisticsAgain", "icons.StatisticsHard",
            "icons.StatisticsGood", "icons.StatisticsEasy"
        ).forEach { assertTrue(source.contains(it), it) }
    }

    @Test
    fun `presentation component has no repository business or responsive authority`() {
        val presentation = source("StudyHeaderStatisticsPresentation.kt")
        val dashboard = source("StudyStatisticsDashboard.kt")
        listOf("Repository", "StudyFacade", "StudyViewModel", "MemoryState", "ReviewEvent")
            .forEach { forbidden ->
                assertFalse(presentation.contains(forbidden), forbidden)
                assertFalse(dashboard.contains(forbidden), forbidden)
            }
        listOf("BoxWithConstraints", "viewportWidth", "StudyVisualLayoutResolver.resolve")
            .forEach { assertFalse(dashboard.contains(it), it) }
        assertFalse(presentation.contains("MaterialTheme"))
    }

    private fun presentation(value: StudyHeaderStatistics = statistics()) =
        resolveStudyHeaderStatisticsPresentation(
            StudyHeaderStatisticsState.Available(value),
            StudyStatisticsStrings.ENGLISH
        )!!

    private fun statistics(
        newCompleted: Int = 3,
        reviewRemaining: Int = 42,
        due: Int = 2,
        again: Int = 1,
        hard: Int = 2,
        good: Int = 6,
        easy: Int = 1
    ) = StudyHeaderStatistics(
        session = StudySessionProgressStatistics(
            "session", newCompleted, 20, maxOf(newCompleted, 6),
            reviewRemaining, 100, maxOf(reviewRemaining, 42)
        ),
        packageLearning = StudyPackageLearningStatistics(
            "scope", Moment(100), again + hard + good + easy,
            due, again, hard, good, easy, Moment(200)
        )
    )

    private fun source(name: String): String = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
    )
}
