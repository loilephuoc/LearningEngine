package vn.loi.learning.desktop.ui.theme

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegacyLavenderSurfaceCleanupTest {
    @Test
    fun `F4 action cards consume neutral shared surface and blue interaction semantics`() {
        val source = source("reviewhistory/ReviewHistoryScreen.kt")
        val action = source.substringAfter("@Composable private fun ReviewAction(").substringBefore("@Composable private fun History(")

        assertTrue(action.contains("MaterialTheme.colorScheme.surfaceContainerLow"))
        assertTrue(action.contains("MaterialTheme.colorScheme.primary"))
        assertTrue(action.contains("MaterialTheme.colorScheme.outlineVariant"))
        assertFalse(action.contains("secondaryContainer"))
        assertFalse(action.contains("tertiaryContainer"))
    }

    @Test
    fun `F6 cards share neutral surface containers while controls retain Material selected semantics`() {
        val source = source("settings/SettingsScreen.kt")
        assertTrue(source.contains("CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)"))
        assertTrue(source.contains("FilterChip("))
        assertTrue(source.contains("Switch("))
        assertFalse(source.contains("Color(0xFF6D28D9)"))
        assertFalse(source.contains("Color(0xFFF3E8FF)"))
    }

    private fun source(relative: String): String = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/$relative")
    )
}
