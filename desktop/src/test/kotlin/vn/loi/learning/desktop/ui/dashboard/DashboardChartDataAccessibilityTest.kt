package vn.loi.learning.desktop.ui.dashboard

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardChartDataAccessibilityTest {
    @Test
    fun `forecast value exposes label value and unit`() {
        val accessibility =
            resolveDashboardChartValueAccessibility(
                item =
                    DashboardChartValue(
                        label = "Tomorrow",
                        value = 12
                    ),
                unit = "reviews"
            )

        assertEquals(
            "Tomorrow: 12 reviews.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank labels and units receive stable fallbacks`() {
        val accessibility =
            resolveDashboardChartValueAccessibility(
                item =
                    DashboardChartValue(
                        label = " ",
                        value = 4
                    ),
                unit = ""
            )

        assertEquals(
            "Unlabeled value: 4 items.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `memory stage exposes count and calculated percentage`() {
        val accessibility =
            resolveDashboardMemoryStageAccessibility(
                item =
                    DashboardChartValue(
                        label = "Review",
                        value = 3
                    ),
                total = 4
            )

        assertEquals(
            "3 · 75%",
            accessibility.value
        )
        assertEquals(
            "Review: 3 cards, 75 percent.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `heatmap day exposes full date and plural review count`() {
        val accessibility =
            resolveDashboardHeatmapDayAccessibility(
                DashboardHeatmapDay(
                    epochDay =
                        LocalDate
                            .of(2026, 7, 22)
                            .toEpochDay(),
                    reviewCount = 5,
                    isFuture = false
                )
            )

        assertEquals(
            "Wednesday, July 22, 2026. 5 reviews.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `heatmap day handles singular zero and future states`() {
        val epochDay =
            LocalDate
                .of(2026, 7, 23)
                .toEpochDay()

        assertEquals(
            "Thursday, July 23, 2026. 1 review.",
            resolveDashboardHeatmapDayAccessibility(
                DashboardHeatmapDay(
                    epochDay = epochDay,
                    reviewCount = 1,
                    isFuture = false
                )
            ).contentDescription
        )

        assertEquals(
            "Thursday, July 23, 2026. No reviews.",
            resolveDashboardHeatmapDayAccessibility(
                DashboardHeatmapDay(
                    epochDay = epochDay,
                    reviewCount = 0,
                    isFuture = false
                )
            ).contentDescription
        )

        assertEquals(
            "Thursday, July 23, 2026. Future date.",
            resolveDashboardHeatmapDayAccessibility(
                DashboardHeatmapDay(
                    epochDay = epochDay,
                    reviewCount = 0,
                    isFuture = true
                )
            ).contentDescription
        )
    }
}
