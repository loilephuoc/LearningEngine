package vn.loi.learning.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import vn.loi.learning.desktop.runtime.DesktopThemePreference

/**
 * Resolves whether Dark Theme should be active based on preference and system state.
 */
fun resolveDarkTheme(
    preference: DesktopThemePreference,
    systemDark: Boolean
): Boolean = when (preference) {
    DesktopThemePreference.LIGHT -> false
    DesktopThemePreference.DARK -> true
    DesktopThemePreference.SYSTEM -> systemDark
}

/**
 * Adapter mapping [LEColors] to Material 3 [ColorScheme] to maintain compatibility with Material3 components.
 */
fun toMaterialColorScheme(colors: LEColors, isDark: Boolean): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = colors.accentPrimary,
        onPrimary = colors.windowBackground,
        primaryContainer = colors.accentSoft,
        onPrimaryContainer = colors.textPrimary,
        secondary = colors.accentHover,
        onSecondary = colors.windowBackground,
        background = colors.windowBackground,
        onBackground = colors.textPrimary,
        surface = colors.surfacePrimary,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceSecondary,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.borderSubtle,
        error = colors.danger,
        onError = colors.windowBackground
    )
} else {
    lightColorScheme(
        primary = colors.accentPrimary,
        onPrimary = colors.surfacePrimary,
        primaryContainer = colors.accentSoft,
        onPrimaryContainer = colors.accentPrimary,
        secondary = colors.accentHover,
        onSecondary = colors.surfacePrimary,
        background = colors.windowBackground,
        onBackground = colors.textPrimary,
        surface = colors.surfacePrimary,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceSecondary,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.borderSubtle,
        error = colors.danger,
        onError = colors.surfacePrimary
    )
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

internal fun resolveMaterialColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) LegacyDarkMaterialColorScheme else LegacyLightMaterialColorScheme

/**
 * Theme Engine Resolver & CompositionLocal Provider for Learning Engine 2.0 (PLE-028B).
 */
@Composable
fun LearningEngineTheme(
    preference: DesktopThemePreference = DesktopThemePreference.SYSTEM,
    densityMode: LEDensityMode = LEDensityMode.COMFORT,
    content: @Composable () -> Unit
) {
    val isDark = resolveDarkTheme(preference, isSystemInDarkTheme())
    val activeColors = if (isDark) DarkLEColors else LightLEColors
    val activeTypography = createLETypography(activeColors)
    val activeBorders = createLEBorderTokens(activeColors)
    val activeDensity = createLEDensityTokens(densityMode)
    val materialScheme = resolveMaterialColorScheme(isDark)

    CompositionLocalProvider(
        LocalLEColors provides activeColors,
        LocalLETypography provides activeTypography,
        LocalLESpacing provides DefaultLESpacing,
        LocalLEShapes provides DefaultLEShapes,
        LocalLEMotion provides DefaultLEMotion,
        LocalLEElevation provides DefaultLEElevation,
        LocalLEIcons provides DefaultLEIcons,
        LocalLEDensity provides activeDensity,
        LocalLEBorders provides activeBorders
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = LearningTypography,
            shapes = LearningShapes,
            content = content
        )
    }
}
