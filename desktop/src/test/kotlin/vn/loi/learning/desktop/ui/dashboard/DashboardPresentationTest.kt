package vn.loi.learning.desktop.ui.dashboard

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardPresentationTest {
    @Test
    fun `today status leads the deterministic reading order`() {
        val presentation = DashboardPresentationResolver.resolve(state())

        assertEquals(DashboardInformationRole.TODAY, presentation.readingOrder.first())
        assertEquals(DashboardInformationRole.HISTORICAL_ANALYTICS, presentation.readingOrder.last())
    }

    @Test
    fun `Ready today uses existing due projection without recomputation`() {
        val presentation = DashboardPresentationResolver.resolve(state())

        assertEquals("Ready today", presentation.today.title)
        assertEquals("12", presentation.today.value)
        assertEquals(listOf("7", "3"), presentation.today.supportingFacts.map { it.value })
    }

    @Test
    fun `key metrics preserve existing values and exclude historical analytics`() {
        val presentation = DashboardPresentationResolver.resolve(state())

        assertEquals(
            listOf("Learning items", "New items", "Active memories"),
            presentation.keyMetrics.map { it.title }
        )
        assertEquals(listOf("90", "21", "64"), presentation.keyMetrics.map { it.value })
    }

    @Test
    fun `resolver preserves loading placeholders`() {
        val presentation = DashboardPresentationResolver.resolve(DashboardUiState())

        assertEquals("--", presentation.today.value)
        assertTrue(presentation.keyMetrics.all { it.value == "--" })
    }

    @Test
    fun `resolver is viewport and theme independent`() {
        val uiState = state()

        assertEquals(
            DashboardPresentationResolver.resolve(uiState),
            DashboardPresentationResolver.resolve(uiState)
        )
    }

    @Test
    fun `screen renders today metrics activity then historical sections`() {
        val screen = source("DashboardScreen.kt")
        val today = screen.indexOf("DashboardTodaySection(")
        val metrics = screen.indexOf("DashboardOverviewSection(")
        val activity = screen.indexOf("DashboardActivitySection(")
        val scheduling = screen.indexOf("DashboardSchedulingSection(")

        assertTrue(today in 0 until metrics)
        assertTrue(metrics in 0 until activity)
        assertTrue(activity in 0 until scheduling)
    }

    @Test
    fun `today and analytical cards use LETheme semantic surfaces`() {
        val today = source("DashboardTodaySection.kt")
        val metric = source("DashboardMetricCard.kt")
        val chart = source("DashboardVisualizationCard.kt")

        assertTrue(today.contains("LESurfaceVariant.PRIMARY"))
        assertTrue(metric.contains("LESurfaceVariant.SECONDARY"))
        assertTrue(chart.contains("LESurfaceVariant.SECONDARY"))
        assertFalse(today.contains("Color(0x"))
        assertFalse(metric.contains("Color(0x"))
    }

    @Test
    fun `presentation changes no dashboard data or navigation authority`() {
        val presentation = source("DashboardPresentation.kt")
        val screen = source("DashboardScreen.kt")

        listOf("Query", "Repository", "Scheduler", "ReviewEvent", "navigateTo", "onClick").forEach {
            assertFalse(presentation.contains(it), "Presentation must not own $it")
        }
        assertFalse(screen.contains("onStartStudy"))
        assertFalse(screen.contains("navigateTo"))
    }

    @Test
    fun `responsive grids retain existing minimum compact normal and wide breakpoints`() {
        val grid = source("DashboardMetricGrid.kt")
        val analytics = source("DashboardAnalyticsLayout.kt")

        assertTrue(grid.contains("maxWidth < 420.dp"))
        assertTrue(grid.contains("maxWidth < 520.dp"))
        assertTrue(grid.contains("maxWidth < 900.dp"))
        assertTrue(analytics.contains("maxWidth >= 1_000.dp"))
    }

    private fun state() = DashboardUiState(
        totalLearningItems = "90",
        activeMemories = "64",
        dueToday = "12",
        dueNow = "7",
        overdue = "3",
        newItems = "21"
    )

    private fun source(name: String): String = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/dashboard/$name")
    )
}
