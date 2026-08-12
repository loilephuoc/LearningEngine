package vn.loi.learning.desktop.ui.theme

import androidx.compose.ui.graphics.Color
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.DesktopThemePreference

class BlueNeutralThemeTest {
    @Test
    fun `light and dark semantic themes use blue primary and focus tokens`() {
        assertEquals(Color(0xFF2563EB), LightLEColors.accentPrimary)
        assertEquals(Color(0xFF3B82F6), LightLEColors.borderFocus)
        assertEquals(Color(0xFF60A5FA), DarkLEColors.accentPrimary)
        assertEquals(Color(0xFF60A5FA), DarkLEColors.borderFocus)
        assertNotEquals(Color(0xFF6D28D9), LightLEColors.accentPrimary)
        assertNotEquals(Color(0xFF8B5CF6), DarkLEColors.accentPrimary)
    }

    @Test
    fun `material schemes preserve blue tonal navigation semantics in both themes`() {
        val light = resolveLETheme(DesktopThemePreference.LIGHT, false, LEDensityMode.COMFORT)
        val dark = resolveLETheme(DesktopThemePreference.DARK, false, LEDensityMode.COMFORT)
        assertEquals(Color(0xFF2563EB), light.materialColorScheme.primary)
        assertEquals(Color(0xFFDBEAFE), light.materialColorScheme.primaryContainer)
        assertEquals(Color(0xFF60A5FA), dark.materialColorScheme.primary)
        assertEquals(Color(0xFF1E3A5F), dark.materialColorScheme.primaryContainer)

        val sidebar = source("component/Sidebar.kt")
        assertTrue(sidebar.contains("MaterialTheme.colorScheme.primaryContainer"))
        assertTrue(sidebar.contains("MaterialTheme.colorScheme.onPrimaryContainer"))
    }

    @Test
    fun `generic Material surface containers remain neutral in light and dark themes`() {
        val light = resolveLETheme(DesktopThemePreference.LIGHT, false, LEDensityMode.COMFORT).materialColorScheme
        val dark = resolveLETheme(DesktopThemePreference.DARK, false, LEDensityMode.COMFORT).materialColorScheme

        assertEquals(Color(0xFFF8FAFC), light.surfaceContainerLow)
        assertEquals(Color(0xFFF1F5F9), light.surfaceContainer)
        assertEquals(Color(0xFFE2E8F0), light.outlineVariant)
        assertEquals(Color(0xFFFEF3C7), light.tertiaryContainer)
        assertEquals(Color(0xFF172033), dark.surfaceContainerLow)
        assertEquals(Color(0xFF1E293B), dark.surfaceContainer)
        assertEquals(Color(0xFF334155), dark.outlineVariant)
        assertEquals(Color(0xFF14532D), dark.tertiaryContainer)
    }

    @Test
    fun `rating and danger semantics remain distinct from primary blue`() {
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            assertNotEquals(colors.accentPrimary, colors.danger)
            assertNotEquals(colors.accentPrimary, colors.warning)
            assertNotEquals(colors.accentPrimary, colors.success)
            assertNotEquals(colors.danger, colors.warning)
            assertNotEquals(colors.warning, colors.success)
        }
    }

    @Test
    fun `Studio copy image remains primary and sibling image actions remain secondary`() {
        val editor = source("studio/ContentEditorPane.kt")
        val toolbar = editor.substringAfter("text = \"Open Fullscreen\"")
        assertTrue(toolbar.contains("LEPrimaryButton("))
        assertTrue(toolbar.contains("text = if (copiedHeroImageFile) \"Copied\" else \"Copy Image\""))
        assertTrue(toolbar.contains("LESecondaryButton("))
        assertTrue(toolbar.contains("text = if (copiedHeroImageName) \"Copied\" else \"Copy Name\""))
    }

    private fun source(relative: String): String = Files.readString(
        Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/$relative")
    )
}
