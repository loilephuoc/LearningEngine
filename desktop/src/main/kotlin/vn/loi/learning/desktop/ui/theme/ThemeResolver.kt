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

/*
 * Existing screens still consume MaterialTheme directly. Keep their established palette until
 * those consumers are migrated deliberately; providing LE tokens must not restyle them.
 */
private val LegacyDarkMaterialColorScheme = darkColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5E35B1),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF26C6DA),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF006064),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF66BB6A),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFD0D0D0),
    outline = Color(0xFF6E6E6E),
    error = Color(0xFFEF5350),
    onError = Color.White
)

private val LegacyLightMaterialColorScheme = lightColorScheme(
    primary = Color(0xFF5E35B1),
    onPrimary = Color.White,
    secondary = Color(0xFF007C91),
    background = Color(0xFFF8F7FC),
    onBackground = Color(0xFF1C1B20),
    surface = Color.White,
    onSurface = Color(0xFF1C1B20),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private fun resolveMaterialColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) LegacyDarkMaterialColorScheme else LegacyLightMaterialColorScheme

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
        LocalLEBorders provides resolved.borders
    ) {
        MaterialTheme(
            colorScheme = resolved.materialColorScheme,
            typography = LearningTypography,
            shapes = LearningShapes,
            content = content
        )
    }
}
