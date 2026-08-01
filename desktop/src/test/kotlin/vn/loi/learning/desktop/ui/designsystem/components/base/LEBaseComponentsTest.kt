package vn.loi.learning.desktop.ui.designsystem.components.base

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.theme.DarkLEColors
import vn.loi.learning.desktop.ui.theme.DefaultLESpacing
import vn.loi.learning.desktop.ui.theme.LightLEColors
import vn.loi.learning.desktop.ui.theme.createLEBorderTokens
import vn.loi.learning.desktop.ui.theme.createLEDensityTokens
import vn.loi.learning.desktop.ui.theme.LEDensityMode

class LEBaseComponentsTest {
    @Test
    fun `surface variants resolve semantic roles without arbitrary styling`() {
        assertEquals(
            LightLEColors.surfacePrimary,
            resolveSurfaceStyle(LightLEColors, LESurfaceVariant.PRIMARY).containerColor
        )
        assertEquals(
            DarkLEColors.surfaceSecondary,
            resolveSurfaceStyle(DarkLEColors, LESurfaceVariant.SECONDARY).containerColor
        )
        val error = resolveSurfaceStyle(LightLEColors, LESurfaceVariant.ERROR)
        assertEquals(LightLEColors.dangerContainer, error.containerColor)
        assertEquals(LightLEColors.dangerText, error.contentColor)
        assertEquals(LightLEColors.surfacePrimary, resolveSurfaceStyle(LightLEColors, LESurfaceVariant.ANSWER).containerColor)
        assertEquals(DarkLEColors.surfaceMeaning, resolveSurfaceStyle(DarkLEColors, LESurfaceVariant.MEANING).containerColor)
        assertEquals(LightLEColors.surfaceExample, resolveSurfaceStyle(LightLEColors, LESurfaceVariant.EXAMPLE).containerColor)
        assertEquals(DarkLEColors.surfaceScheduler, resolveSurfaceStyle(DarkLEColors, LESurfaceVariant.SCHEDULER).containerColor)
        assertEquals(LightLEColors.surfaceToolbar, resolveSurfaceStyle(LightLEColors, LESurfaceVariant.RATING_DOCK).containerColor)
    }

    @Test
    fun `button resting hover pressed focused and disabled states are deterministic`() {
        val resting = style(enabled = true)
        val hovered = style(enabled = true, hovered = true)
        val pressed = style(enabled = true, pressed = true)
        val focused = style(enabled = true, focused = true)
        val disabled = style(enabled = false)
        assertEquals(LightLEColors.accentPrimary, resting.containerColor)
        assertEquals(LightLEColors.accentHover, hovered.containerColor)
        assertEquals(hovered.containerColor, pressed.containerColor)
        assertEquals(createLEBorderTokens(LightLEColors).thick, focused.focusWidth)
        assertEquals(LightLEColors.borderFocus, focused.focusColor)
        assertEquals(LightLEColors.surfaceSecondary, disabled.containerColor)
        assertEquals(LightLEColors.textDisabled, disabled.contentColor)
        assertNotEquals(disabled.containerColor, disabled.contentColor)
    }

    @Test
    fun `button variants map to semantic token roles in both themes`() {
        val secondary = style(variant = LEButtonVariant.SECONDARY)
        val destructive = style(variant = LEButtonVariant.DESTRUCTIVE)
        val darkPrimary = style(colorsDark = true)
        assertEquals(LightLEColors.surfaceSecondary, secondary.containerColor)
        assertEquals(LightLEColors.textPrimary, secondary.contentColor)
        assertEquals(LightLEColors.danger, destructive.containerColor)
        assertEquals(DarkLEColors.accentPrimary, darkPrimary.containerColor)
        assertEquals(LightLEColors.textSecondary, style(variant = LEButtonVariant.QUIET).contentColor)
        assertEquals(LightLEColors.dangerContainer, style(variant = LEButtonVariant.RATING_AGAIN).containerColor)
        assertEquals(LightLEColors.warningContainer, style(variant = LEButtonVariant.RATING_HARD).containerColor)
        assertEquals(LightLEColors.successContainer, style(variant = LEButtonVariant.RATING_GOOD).containerColor)
        assertEquals(LightLEColors.infoContainer, style(variant = LEButtonVariant.RATING_EASY).containerColor)
    }

    @Test
    fun `density modes project exact minimum targets`() {
        assertEquals(
            createLEDensityTokens(LEDensityMode.COMFORT).minTouchTargetSize,
            style(densityMode = LEDensityMode.COMFORT).minimumTargetSize
        )
        assertEquals(
            createLEDensityTokens(LEDensityMode.COMPACT).minTouchTargetSize,
            style(densityMode = LEDensityMode.COMPACT).minimumTargetSize
        )
        assertEquals(
            createLEDensityTokens(LEDensityMode.TOUCH).minTouchTargetSize,
            style(densityMode = LEDensityMode.TOUCH).minimumTargetSize
        )
    }

    @Test
    fun `focus border has no layout padding authority`() {
        val source = baseSourceDirectory().resolve("LEButton.kt").readText()
        assertFalse(source.contains(".padding("))
        assertTrue(source.contains("if (emphasized) LETheme.borders.thick else style.focusWidth"))
        assertTrue(source.contains("if (emphasized) style.contentColor else style.focusColor"))
        assertTrue(source.contains("color = style.contentColor"))
        assertTrue(source.contains("TextDecoration.Underline"))
        assertTrue(source.contains("showPreviousValueIndicator"))
        assertEquals(createLEBorderTokens(LightLEColors).thick, style(focused = true).focusWidth)
    }

    @Test
    fun `base component API and dependencies stay semantic`() {
        val source = baseSourceDirectory().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        listOf(
            "MaterialTheme.colorScheme",
            "isSystemInDarkTheme",
            "Color(0x",
            "RoundedCornerShape(",
            "vn.loi.learning.domain",
            "persistence",
            "scheduler",
            "repository",
            "filesystem",
            "audio",
            "navigation"
        ).forEach { forbidden -> assertFalse(source.contains(forbidden), forbidden) }
        val publicComposableApi = listOf("LEButton.kt", "LESurface.kt")
            .joinToString("\n") { baseSourceDirectory().resolve(it).readText() }
        assertFalse(
            Regex("""(background|content|border)Color\s*:""")
                .containsMatchIn(publicComposableApi)
        )
        assertFalse(Regex("""\b(viewport|maxWidth|minWidth\s*<)\b""").containsMatchIn(source))
    }

    @Test
    fun `migrated load-state consumer preserves retry callback and enabled authority`() {
        val source = sourceRoot().resolve("ui/state/DesktopLoadStateCard.kt").readText()
        assertTrue(source.contains("onClick = onRetry"))
        assertFalse(source.contains("enabled ="))
        assertTrue(source.contains("presentation.actionLabel"))
    }

    @Test
    fun `migrated consumers use LE surfaces without Material color authority`() {
        listOf(
            sourceRoot().resolve("ui/state/DesktopLoadStateCard.kt"),
            sourceRoot().resolve("ui/search/SearchScopeCard.kt")
        ).forEach { file ->
            val source = file.readText()
            assertTrue(source.contains("LESurface("))
            assertFalse(source.contains("MaterialTheme.colorScheme"))
            assertFalse(source.contains("CardDefaults"))
        }
    }

    private fun style(
        variant: LEButtonVariant = LEButtonVariant.PRIMARY,
        enabled: Boolean = true,
        hovered: Boolean = false,
        pressed: Boolean = false,
        focused: Boolean = false,
        colorsDark: Boolean = false,
        densityMode: LEDensityMode = LEDensityMode.COMFORT
    ) = (if (colorsDark) DarkLEColors else LightLEColors).let { colors ->
        resolveButtonStyle(
            colors = colors,
            borders = createLEBorderTokens(colors),
            density = createLEDensityTokens(densityMode),
            variant = variant,
            enabled = enabled,
            hovered = hovered,
            pressed = pressed,
            focused = focused
        )
    }

    private fun baseSourceDirectory(): File =
        sourceRoot().resolve("ui/designsystem/components/base")

    private fun sourceRoot(): File {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop")
        return if (fromRoot.isDirectory) fromRoot else File("src/main/kotlin/vn/loi/learning/desktop")
    }
}
