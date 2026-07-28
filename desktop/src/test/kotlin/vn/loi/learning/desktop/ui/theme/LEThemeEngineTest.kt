package vn.loi.learning.desktop.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.DesktopThemePreference

class LEThemeEngineTest {

    @Test
    fun `resolveDarkTheme respects explicit and system preferences`() {
        assertFalse(resolveDarkTheme(DesktopThemePreference.LIGHT, systemDark = true))
        assertTrue(resolveDarkTheme(DesktopThemePreference.DARK, systemDark = false))
        assertFalse(resolveDarkTheme(DesktopThemePreference.SYSTEM, systemDark = false))
        assertTrue(resolveDarkTheme(DesktopThemePreference.SYSTEM, systemDark = true))
    }

    @Test
    fun `light colors conform to PLE-028A crisp white canvas specification`() {
        val colors = LightLEColors
        assertEquals(Color(0xFFFAFAFA), colors.windowBackground)
        assertEquals(Color(0xFFFFFFFF), colors.surfacePrimary)
        assertEquals(Color(0xFF09090B), colors.textPrimary)
        assertEquals(Color(0xFF52525B), colors.textSecondary)
        assertEquals(Color(0xFF71717A), colors.textMuted)
        assertEquals(Color(0xFF6D28D9), colors.accentPrimary)
    }

    @Test
    fun `dark colors provide high contrast remediation for IPA, meaning and examples`() {
        val colors = DarkLEColors
        assertEquals(Color(0xFF090D16), colors.windowBackground)
        assertEquals(Color(0xFF161B26), colors.surfacePrimary)

        // Fix UAT contrast checks: Text values must be bright Slate/Off-white
        assertEquals(Color(0xFFF8FAFC), colors.textPrimary)
        assertEquals(Color(0xFFE2E8F0), colors.textSecondary)
        assertEquals(Color(0xFF94A3B8), colors.textMuted)
        assertEquals(Color(0xFF64748B), colors.textDisabled)

        // Ensure Dark surface colors differ from Window Canvas (Luminance layering)
        assertNotEquals(colors.windowBackground, colors.surfacePrimary)
        assertNotEquals(colors.surfacePrimary, colors.surfaceSecondary)
    }

    @Test
    fun `typography creation maps semantic colors dynamically`() {
        val lightTypography = createLETypography(LightLEColors)
        assertEquals(LightLEColors.textPrimary, lightTypography.displayWord.color)
        assertEquals(LightLEColors.textPrimary, lightTypography.meaningPrimary.color)
        assertEquals(LightLEColors.textSecondary, lightTypography.bodyDefinition.color)
        assertEquals(LightLEColors.textMuted, lightTypography.metadataIpa.color)

        val darkTypography = createLETypography(DarkLEColors)
        assertEquals(DarkLEColors.textPrimary, darkTypography.displayWord.color)
        assertEquals(DarkLEColors.textPrimary, darkTypography.meaningPrimary.color)
        assertEquals(DarkLEColors.textSecondary, darkTypography.bodyDefinition.color)
        assertEquals(DarkLEColors.textMuted, darkTypography.metadataIpa.color)
    }

    @Test
    fun `spacing scale conforms to geometric 4dp 8dp grid`() {
        val spacing = DefaultLESpacing
        assertEquals(0.dp, spacing.space0)
        assertEquals(2.dp, spacing.space1)
        assertEquals(4.dp, spacing.space2)
        assertEquals(8.dp, spacing.space3)
        assertEquals(12.dp, spacing.space4)
        assertEquals(16.dp, spacing.space5)
        assertEquals(24.dp, spacing.space6)
        assertEquals(32.dp, spacing.space7)
        assertEquals(48.dp, spacing.space8)
        assertEquals(64.dp, spacing.space9)

        // Verify aliases
        assertEquals(spacing.space1, spacing.xxs)
        assertEquals(spacing.space5, spacing.lg)
        assertEquals(spacing.space8, spacing.xxxl)
    }

    @Test
    fun `shapes scale defines required corner radius tokens`() {
        val shapes = DefaultLEShapes
        assertNotNull(shapes.radiusNone)
        assertNotNull(shapes.radiusXS)
        assertNotNull(shapes.radiusS)
        assertNotNull(shapes.radiusM)
        assertNotNull(shapes.radiusL)
        assertNotNull(shapes.radiusXL)
        assertNotNull(shapes.radius2XL)
        assertNotNull(shapes.radiusPill)
        assertNotNull(shapes.radiusCircle)

        assertEquals(0.dp, shapes.radiusNoneDp)
        assertEquals(4.dp, shapes.radiusXSDp)
        assertEquals(6.dp, shapes.radiusSDp)
        assertEquals(8.dp, shapes.radiusMDp)
        assertEquals(12.dp, shapes.radiusLDp)
        assertEquals(16.dp, shapes.radiusXLDp)
        assertEquals(24.dp, shapes.radius2XLDp)
    }

    @Test
    fun `motion tokens provide duration scale and easing curves`() {
        val motion = DefaultLEMotion
        assertEquals(0, motion.durationInstant)
        assertEquals(80, motion.durationVeryFast)
        assertEquals(120, motion.durationFast)
        assertEquals(200, motion.durationNormal)
        assertEquals(300, motion.durationSlow)
        assertEquals(400, motion.durationVerySlow)

        assertEquals(80, motion.hoverDuration)
        assertEquals(120, motion.ratingDuration)
        assertEquals(200, motion.revealDuration)

        assertNotNull(motion.easingStandard)
        assertNotNull(motion.easingDecelerate)
        assertNotNull(motion.easingAccelerate)
    }

    @Test
    fun `elevation tokens define 5 layering levels`() {
        val elevation = DefaultLEElevation
        assertEquals(0.dp, elevation.elevation0)
        assertEquals(1.dp, elevation.elevation1)
        assertEquals(2.dp, elevation.elevation2)
        assertEquals(8.dp, elevation.elevation3)
        assertEquals(16.dp, elevation.elevation4)
    }

    @Test
    fun `density modes calculate target sizes and padding correctly`() {
        val comfort = createLEDensityTokens(LEDensityMode.COMFORT)
        assertEquals(LEDensityMode.COMFORT, comfort.mode)
        assertEquals(1.0f, comfort.spacingMultiplier)
        assertEquals(40.dp, comfort.minTouchTargetSize)
        assertEquals(16.dp, comfort.cardPadding)

        val compact = createLEDensityTokens(LEDensityMode.COMPACT)
        assertEquals(LEDensityMode.COMPACT, compact.mode)
        assertEquals(0.75f, compact.spacingMultiplier)
        assertEquals(32.dp, compact.minTouchTargetSize)
        assertEquals(12.dp, compact.cardPadding)

        val touch = createLEDensityTokens(LEDensityMode.TOUCH)
        assertEquals(LEDensityMode.TOUCH, touch.mode)
        assertEquals(1.25f, touch.spacingMultiplier)
        assertEquals(48.dp, touch.minTouchTargetSize)
        assertEquals(24.dp, touch.cardPadding)
    }

    @Test
    fun `border tokens bind dynamically to active colors`() {
        val lightBorders = createLEBorderTokens(LightLEColors)
        assertEquals(LightLEColors.borderSubtle, lightBorders.subtle.brush.let { (it as androidx.compose.ui.graphics.SolidColor).value })
        assertEquals(LightLEColors.borderFocus, lightBorders.focus.brush.let { (it as androidx.compose.ui.graphics.SolidColor).value })

        val darkBorders = createLEBorderTokens(DarkLEColors)
        assertEquals(DarkLEColors.borderSubtle, darkBorders.subtle.brush.let { (it as androidx.compose.ui.graphics.SolidColor).value })
        assertEquals(DarkLEColors.borderFocus, darkBorders.focus.brush.let { (it as androidx.compose.ui.graphics.SolidColor).value })
    }

    @Test
    fun `material color scheme adapter bridges LEColors seamlessly`() {
        val lightScheme = toMaterialColorScheme(LightLEColors, isDark = false)
        assertEquals(LightLEColors.accentPrimary, lightScheme.primary)
        assertEquals(LightLEColors.surfacePrimary, lightScheme.surface)
        assertEquals(LightLEColors.windowBackground, lightScheme.background)

        val darkScheme = toMaterialColorScheme(DarkLEColors, isDark = true)
        assertEquals(DarkLEColors.accentPrimary, darkScheme.primary)
        assertEquals(DarkLEColors.surfacePrimary, darkScheme.surface)
        assertEquals(DarkLEColors.windowBackground, darkScheme.background)
    }

    @Test
    fun `material compatibility adapter preserves production visual palette`() {
        val lightScheme = resolveMaterialColorScheme(isDark = false)
        assertEquals(Color(0xFF5E35B1), lightScheme.primary)
        assertEquals(Color(0xFFF8F7FC), lightScheme.background)
        assertEquals(Color(0xFF1C1B20), lightScheme.onBackground)
        assertEquals(Color.White, lightScheme.surface)

        val darkScheme = resolveMaterialColorScheme(isDark = true)
        assertEquals(Color(0xFF7C4DFF), darkScheme.primary)
        assertEquals(Color(0xFF121212), darkScheme.background)
        assertEquals(Color(0xFF1E1E1E), darkScheme.surface)
        assertEquals(Color(0xFFD0D0D0), darkScheme.onSurfaceVariant)
    }

    @Test
    fun `icons tokens provide full set of vector icon definitions`() {
        val icons = DefaultLEIcons
        assertNotNull(icons.New)
        assertNotNull(icons.Save)
        assertNotNull(icons.Search)
        assertNotNull(icons.Audio)
        assertNotNull(icons.Play)
        assertNotNull(icons.Success)
        assertNotNull(icons.Learning)
        assertNotNull(icons.Scheduler)
    }
}
