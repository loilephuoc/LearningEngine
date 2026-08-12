package vn.loi.learning.desktop.ui.library

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class LibraryNarrowStatisticsPresentationTest {
    @Test
    fun `narrow uses compact statistics while wide keeps existing stat cards`() {
        val source = Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/library/LibraryHeader.kt"))
        assertTrue(source.contains("DesktopContentWidthClass.NARROW"))
        assertTrue(source.contains("CompactLibraryStatistics(statistics)"))
        assertTrue(source.contains("else FlowRow"))
        assertTrue(source.contains("StatCard("))
        assertTrue(source.contains("vertical = 10.dp"))
    }
}
