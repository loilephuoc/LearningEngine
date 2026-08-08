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
    primary = Color(0xFF167A5B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8F1DC), onPrimaryContainer = Color(0xFF052D20),
    secondary = Color(0xFF507665), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8EBDD), onSecondaryContainer = Color(0xFF102C21),
    tertiary = Color(0xFF36766F), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4ECE7), onTertiaryContainer = Color(0xFF082F2B),
    background = Color(0xFFF7F7F1), onBackground = Color(0xFF17201B),
    surface = Color(0xFFFFFEF9), onSurface = Color(0xFF17201B),
    surfaceVariant = Color(0xFFE5EEE7), onSurfaceVariant = Color(0xFF45534B),
    outline = Color(0xFF75827A), outlineVariant = Color(0xFFD0DAD2),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2D3130), inverseOnSurface = Color(0xFFEFF1EE),
    inversePrimary = Color(0xFF72DBB4), surfaceTint = Color(0xFF167A5B),
    scrim = Color(0xFF000000)
)

val LearningEngineDarkColors = darkColorScheme(
    primary = Color(0xFF72DBB4), onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF14513F), onPrimaryContainer = Color(0xFFA8F2D0),
    secondary = Color(0xFFA6D5BE), onSecondary = Color(0xFF123629),
    secondaryContainer = Color(0xFF294B3D), onSecondaryContainer = Color(0xFFC1ECD6),
    tertiary = Color(0xFF83D5CD), onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF17514E), onTertiaryContainer = Color(0xFFA4F1E9),
    background = Color(0xFF0C1516), onBackground = Color(0xFFDCE7E1),
    surface = Color(0xFF121D1E), onSurface = Color(0xFFDCE7E1),
    surfaceVariant = Color(0xFF263334), onSurfaceVariant = Color(0xFFBAC8C2),
    outline = Color(0xFF84938D), outlineVariant = Color(0xFF354443),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFDCE5E1), inverseOnSurface = Color(0xFF26302D),
    inversePrimary = Color(0xFF167A5B), surfaceTint = Color(0xFF72DBB4),
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
