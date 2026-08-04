package vn.loi.learning.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LearningEngineLightColors = lightColorScheme(
    primary = Color(0xFF006B5F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9EF2E1), onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF426277), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC6E7FF), onSecondaryContainer = Color(0xFF001E2D),
    tertiary = Color(0xFF66558E), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADDFF), onTertiaryContainer = Color(0xFF211047),
    background = Color(0xFFF7FAF7), onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFCFDF9), onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDBE5E1), onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976), outlineVariant = Color(0xFFBEC9C5),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2D3130), inverseOnSurface = Color(0xFFEFF1EE),
    inversePrimary = Color(0xFF82D5C5), surfaceTint = Color(0xFF006B5F),
    scrim = Color(0xFF000000)
)

val LearningEngineDarkColors = darkColorScheme(
    primary = Color(0xFF82D5C5), onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005047), onPrimaryContainer = Color(0xFF9EF2E1),
    secondary = Color(0xFFA9CCE5), onSecondary = Color(0xFF103447),
    secondaryContainer = Color(0xFF294B5E), onSecondaryContainer = Color(0xFFC6E7FF),
    tertiary = Color(0xFFCEBDFA), onTertiary = Color(0xFF37275D),
    tertiaryContainer = Color(0xFF4E3E75), onTertiaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF101716), onBackground = Color(0xFFDCE5E1),
    surface = Color(0xFF17201E), onSurface = Color(0xFFDCE5E1),
    surfaceVariant = Color(0xFF283431), onSurfaceVariant = Color(0xFFBEC9C5),
    outline = Color(0xFF89938F), outlineVariant = Color(0xFF3F4946),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFDCE5E1), inverseOnSurface = Color(0xFF26302D),
    inversePrimary = Color(0xFF006B5F), surfaceTint = Color(0xFF82D5C5),
    scrim = Color(0xFF000000)
)

data class LearningSemanticColors(
    val success: Color, val warning: Color, val info: Color,
    val activeLearning: Color, val dueReview: Color, val overdueReview: Color,
    val completed: Color, val difficult: Color, val medium: Color, val easy: Color,
    val streak: Color, val progressTrack: Color
)

val LearningEngineLightSemanticColors = LearningSemanticColors(
    success = Color(0xFF247A4B), warning = Color(0xFF8A5A00), info = Color(0xFF28638A),
    activeLearning = Color(0xFF006B5F), dueReview = Color(0xFF66558E), overdueReview = Color(0xFFBA1A1A),
    completed = Color(0xFF247A4B), difficult = Color(0xFFBA1A1A), medium = Color(0xFF8A5A00),
    easy = Color(0xFF247A4B), streak = Color(0xFF8A4E00), progressTrack = Color(0xFFCFDAD6)
)

val LearningEngineDarkSemanticColors = LearningSemanticColors(
    success = Color(0xFF75DB9B), warning = Color(0xFFFFC86A), info = Color(0xFF8DCEF5),
    activeLearning = Color(0xFF82D5C5), dueReview = Color(0xFFCEBDFA), overdueReview = Color(0xFFFFB4AB),
    completed = Color(0xFF75DB9B), difficult = Color(0xFFFFB4AB), medium = Color(0xFFFFC86A),
    easy = Color(0xFF75DB9B), streak = Color(0xFFFFB86B), progressTrack = Color(0xFF34413E)
)

val LocalLearningSemanticColors = staticCompositionLocalOf { LearningEngineLightSemanticColors }

object LearningEngineThemeTokens {
    val semanticColors: LearningSemanticColors
        @Composable get() = LocalLearningSemanticColors.current
}

@Composable
fun LearningEngineTheme(
    mode: AndroidThemeMode = AndroidThemeMode.FOLLOW_SYSTEM,
    systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dark = mode.resolveDark(systemDark)
    CompositionLocalProvider(
        LocalLearningSemanticColors provides if (dark) LearningEngineDarkSemanticColors else LearningEngineLightSemanticColors
    ) {
        MaterialTheme(
            colorScheme = if (dark) LearningEngineDarkColors else LearningEngineLightColors,
            typography = LearningEngineTypography,
            shapes = LearningEngineShapes,
            content = content
        )
    }
}
