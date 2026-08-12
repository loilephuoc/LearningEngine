package vn.loi.learning.desktop.ui.responsive

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.designsystem.responsive.DesktopContentWidthClass
import vn.loi.learning.desktop.ui.designsystem.responsive.DesktopResponsivePolicyResolver

class DesktopResponsiveArchitectureTest {
    @Test
    fun `shared policy resolves narrow medium and wide consistently`() {
        assertEquals(DesktopContentWidthClass.NARROW, DesktopResponsivePolicyResolver.resolve(619).widthClass)
        assertEquals(DesktopContentWidthClass.MEDIUM, DesktopResponsivePolicyResolver.resolve(620).widthClass)
        assertEquals(DesktopContentWidthClass.MEDIUM, DesktopResponsivePolicyResolver.resolve(999).widthClass)
        assertEquals(DesktopContentWidthClass.WIDE, DesktopResponsivePolicyResolver.resolve(1000).widthClass)
        assertEquals(1, DesktopResponsivePolicyResolver.resolve(500).metricColumns)
        assertEquals(2, DesktopResponsivePolicyResolver.resolve(800).metricColumns)
        assertEquals(4, DesktopResponsivePolicyResolver.resolve(1200).metricColumns)
    }

    @Test
    fun `F1 keeps CTA visible and stacks below wide breakpoint`() {
        val source = source("dashboard/DashboardTodaySection.kt")
        assertTrue(source.contains("DesktopResponsivePolicyResolver.resolve"))
        assertTrue(source.contains("DesktopContentWidthClass.WIDE"))
        assertTrue(source.contains("Button(onClick = onStudyNow, modifier = Modifier.fillMaxWidth())"))
        assertTrue(source.contains("Text(presentation.value, maxLines = 1"))
    }

    @Test
    fun `F5 wraps header metrics package details and actions without hiding commands`() {
        val header = source("library/LibraryHeader.kt")
        val packages = source("library/PackageListSection.kt")
        val tabs = source("library/LibrarySectionTabs.kt")

        assertTrue(header.contains("DesktopResponsivePolicyResolver.resolve"))
        assertTrue(header.contains("FlowRow("))
        assertTrue(tabs.contains("ScrollableTabRow("))
        assertTrue(tabs.contains("edgePadding = 0.dp"))
        assertTrue(packages.contains("maxWidth >= 1000.dp -> 6"))
        assertTrue(packages.contains("maxWidth >= 620.dp -> 3"))
        assertTrue(packages.contains("else -> 2"))
        listOf("Browse", "Set Active", "Export", "Up", "Down", "Archive", "Remove Topic").forEach {
            assertTrue(packages.contains(it), "Missing reachable package action: $it")
        }
    }

    @Test
    fun `F6 wraps presets bounds inputs and stacks shortcuts on narrow panes`() {
        val source = source("settings/SettingsScreen.kt")
        assertTrue(source.contains("FlowRow(horizontalArrangement"))
        assertTrue(source.contains("Modifier.fillMaxWidth().widthIn(max = 300.dp)"))
        assertTrue(source.contains("val narrow = maxWidth < 620.dp"))
        assertTrue(source.contains("Text(label, modifier = Modifier.weight(1f))"))
        assertTrue(source.contains("modifier = Modifier.weight(2f)"))
        assertFalse(source.contains("modifier = Modifier.width(300.dp)"))
    }

    private fun source(relative: String): String = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/$relative")
    )
}
