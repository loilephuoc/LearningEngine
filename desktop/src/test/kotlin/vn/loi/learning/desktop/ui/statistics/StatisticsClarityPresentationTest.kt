package vn.loi.learning.desktop.ui.statistics

import androidx.compose.ui.unit.dp
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.dashboard.resolveHeatmapLayoutPolicy

class StatisticsClarityPresentationTest {
    @Test
    fun `statistics offers all learned and real single package scopes`() {
        val screen = statisticsSource("StatisticsScreen.kt")
        val facade = statisticsSource("StatisticsFacade.kt")
        val viewModel = statisticsSource("StatisticsViewModel.kt")

        assertTrue(screen.contains("uiState.scopeOptions"))
        assertTrue(screen.contains("FilterChip"))
        assertTrue(facade.contains("LearningDashboardQuery("))
        assertTrue(facade.contains("ReviewHistoryQuery("))
        assertTrue(facade.contains("getContentDescriptorsForPackage"))
        assertTrue(facade.contains("getLearningItemsByContentIds"))
        assertTrue(facade.contains("learningItemIds = scopedItemIds"))
        assertTrue(viewModel.contains("taskRunner.run"))
        assertTrue(viewModel.contains("fun selectScope"))
    }

    @Test
    fun `heatmap policy keeps twelve by seven cells compact across widths`() {
        val narrow = resolveHeatmapLayoutPolicy(190.dp)
        val medium = resolveHeatmapLayoutPolicy(300.dp)
        val wide = resolveHeatmapLayoutPolicy(900.dp)
        val heatmap = dashboardSource("DashboardReviewHeatmap.kt")
        val facade = statisticsSource("StatisticsFacade.kt")

        assertEquals(12.dp, narrow.cellSize)
        assertTrue(medium.cellSize in 12.dp..18.dp)
        assertEquals(18.dp, wide.cellSize)
        assertEquals(3.dp, narrow.gap)
        assertTrue(heatmap.contains("HEATMAP_WEEK_COUNT = 12"))
        assertTrue(heatmap.contains("DAYS_PER_WEEK = 7"))
        assertTrue(facade.contains("return List(84)"))
        assertFalse(heatmap.contains("Modifier.weight(1f)"))
        assertFalse(heatmap.contains("aspectRatio(1f)"))
        assertTrue(heatmap.contains("horizontalScroll(rememberScrollState())"))
        listOf("Mon", "Wed", "Fri", "HeatmapMonthLabels").forEach {
            assertTrue(heatmap.contains(it))
        }
    }

    @Test
    fun `activity metric semantics remain present`() {
        val screen = statisticsSource("StatisticsScreen.kt")

        listOf(
            "Lượt ôn",
            "Ngày hoạt động (30 ngày)",
            "Lượt ôn / ngày",
            "Chuỗi hiện tại",
            "Lần ôn gần nhất"
        ).forEach { assertTrue(screen.contains(it)) }
    }

    private fun statisticsSource(name: String): String = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/statistics/$name")
    )

    private fun dashboardSource(name: String): String = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/dashboard/$name")
    )
}
