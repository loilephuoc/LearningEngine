package vn.loi.learning.desktop.ui.theme

import androidx.compose.runtime.Composable
import vn.loi.learning.desktop.runtime.DesktopThemePreference

/**
 * Compatibility adapter for the established Desktop composition root.
 *
 * Theme resolution and Material adaptation belong exclusively to [LearningEngineTheme].
 */
@Composable
fun LearningTheme(
    preference: DesktopThemePreference = DesktopThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    LearningEngineTheme(
        preference = preference,
        content = content
    )
}
