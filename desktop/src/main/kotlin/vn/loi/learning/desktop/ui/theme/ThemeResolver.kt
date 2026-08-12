package vn.loi.learning.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import vn.loi.learning.desktop.runtime.DesktopThemePreference

/**
 * Resolves whether Dark Theme should be active based on preference and system state.
 */
internal fun resolveDarkTheme(
    preference: DesktopThemePreference,
    systemDark: Boolean
): Boolean = when (preference) {
    DesktopThemePreference.LIGHT -> false
    DesktopThemePreference.DARK -> true
    DesktopThemePreference.SYSTEM -> systemDark
}

private val DarkMaterialColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF273449),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF14532D),
    onTertiaryContainer = Color(0xFFDCFCE7),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF172033),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceContainerLowest = Color(0xFF0F172A),
    surfaceContainerLow = Color(0xFF172033),
    surfaceContainer = Color(0xFF1E293B),
    surfaceContainerHigh = Color(0xFF273449),
    surfaceContainerHighest = Color(0xFF334155),
    surfaceBright = Color(0xFF273449),
    surfaceDim = Color(0xFF0F172A),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color.White
)

private val LightMaterialColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFEFF3F8),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private fun resolveMaterialColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) DarkMaterialColorScheme else LightMaterialColorScheme

@Immutable
internal data class ResolvedLETheme(
    val isDark: Boolean,
    val colors: LEColors,
    val typography: LETypography,
    val spacing: LESpacingTokens,
    val shapes: LEShapesTokens,
    val motion: LEMotionTokens,
    val elevation: LEElevationTokens,
    val icons: LEIconsTokens,
    val density: LEDensityTokens,
    val borders: LEBorderTokens,
    val partOfSpeech: LEPartOfSpeechTokens,
    val materialColorScheme: ColorScheme
)

internal fun resolveLETheme(
    preference: DesktopThemePreference,
    systemDark: Boolean,
    densityMode: LEDensityMode
): ResolvedLETheme {
    val isDark = resolveDarkTheme(preference, systemDark)
    val colors = if (isDark) DarkLEColors else LightLEColors
    return ResolvedLETheme(
        isDark = isDark,
        colors = colors,
        typography = createLETypography(colors),
        spacing = DefaultLESpacing,
        shapes = DefaultLEShapes,
        motion = DefaultLEMotion,
        elevation = DefaultLEElevation,
        icons = DefaultLEIcons,
        density = createLEDensityTokens(densityMode),
        borders = createLEBorderTokens(colors),
        partOfSpeech = createLEPartOfSpeechTokens(isDark),
        materialColorScheme = resolveMaterialColorScheme(isDark)
    )
}

/**
 * Theme Engine Resolver & CompositionLocal Provider for Learning Engine 2.0 (PLE-028B).
 */
@Composable
fun LearningEngineTheme(
    preference: DesktopThemePreference = DesktopThemePreference.SYSTEM,
    densityMode: LEDensityMode = LEDensityMode.COMFORT,
    content: @Composable () -> Unit
) {
    val resolved = resolveLETheme(preference, isSystemInDarkTheme(), densityMode)

    CompositionLocalProvider(
        LocalLEColors provides resolved.colors,
        LocalLETypography provides resolved.typography,
        LocalLESpacing provides resolved.spacing,
        LocalLEShapes provides resolved.shapes,
        LocalLEMotion provides resolved.motion,
        LocalLEElevation provides resolved.elevation,
        LocalLEIcons provides resolved.icons,
        LocalLEDensity provides resolved.density,
        LocalLEBorders provides resolved.borders,
        LocalLEPartOfSpeech provides resolved.partOfSpeech
    ) {
        MaterialTheme(
            colorScheme = resolved.materialColorScheme,
            typography = LearningTypography,
            shapes = LearningShapes,
            content = content
        )
    }
}
