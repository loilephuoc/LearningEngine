package vn.loi.learning.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun LearningTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LearningDarkColorScheme,
        typography = LearningTypography,
        shapes = LearningShapes,
        content = content
    )
}