package vn.loi.learning.desktop.ui.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.DesktopLocale
import vn.loi.learning.desktop.ui.navigation.NavigationDestination

class DesktopLocalizationTest {
    @Test
    fun `English and Vietnamese catalogs cover every shell destination`() {
        DesktopLocale.entries.forEach { locale ->
            val strings = DesktopLocalization.strings(locale)
            NavigationDestination.entries.forEach { destination ->
                assertTrue(strings.destination(destination).isNotBlank())
            }
        }
    }

    @Test
    fun `Vietnamese catalog provides localized shell and settings vocabulary`() {
        val strings = DesktopLocalization.strings(DesktopLocale.VIETNAMESE)

        assertEquals("Trang chủ", strings.destination(NavigationDestination.DASHBOARD))
        assertEquals("Cài đặt", strings.settingsTitle)
        assertEquals("Tiếng Việt", strings.language(DesktopLocale.VIETNAMESE))
    }
}
