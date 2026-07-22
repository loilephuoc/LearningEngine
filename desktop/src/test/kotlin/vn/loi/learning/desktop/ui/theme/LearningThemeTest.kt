package vn.loi.learning.desktop.ui.theme

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.DesktopThemePreference

class LearningThemeTest {
    @Test
    fun `explicit theme preferences ignore system appearance`() {
        assertFalse(resolveDarkTheme(DesktopThemePreference.LIGHT, systemDark = true))
        assertTrue(resolveDarkTheme(DesktopThemePreference.DARK, systemDark = false))
    }

    @Test
    fun `system preference follows system appearance`() {
        assertFalse(resolveDarkTheme(DesktopThemePreference.SYSTEM, systemDark = false))
        assertTrue(resolveDarkTheme(DesktopThemePreference.SYSTEM, systemDark = true))
    }
}
