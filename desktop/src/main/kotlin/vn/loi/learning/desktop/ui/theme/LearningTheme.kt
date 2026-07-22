package vn.loi.learning.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import vn.loi.learning.desktop.runtime.DesktopThemePreference

private val LearningDarkColorScheme =
    darkColorScheme(
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

private val LearningLightColorScheme =
    lightColorScheme(
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

fun resolveDarkTheme(
    preference: DesktopThemePreference,
    systemDark: Boolean
): Boolean =
    when (preference) {
        DesktopThemePreference.LIGHT -> false
        DesktopThemePreference.DARK -> true
        DesktopThemePreference.SYSTEM -> systemDark
    }

@Composable
fun LearningTheme(
    preference: DesktopThemePreference = DesktopThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = resolveDarkTheme(preference, isSystemInDarkTheme())

    MaterialTheme(
        colorScheme =
            if (darkTheme) {
                LearningDarkColorScheme
            } else {
                LearningLightColorScheme
            },
        typography = LearningTypography,
        shapes = LearningShapes,
        content = content
    )
}
