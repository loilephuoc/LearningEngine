package vn.loi.learning.desktop.ui.settings

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionLimitSettingTest {
    @Test
    fun `custom review input accepts configured boundaries and rejects invalid content`() {
        val range = 0..500
        assertEquals(0, parseSessionLimitInput("0", range, otherValue = 5))
        assertEquals(5, parseSessionLimitInput("5", range, otherValue = 5))
        assertEquals(500, parseSessionLimitInput("500", range, otherValue = 5))
        assertNull(parseSessionLimitInput("", range, otherValue = 5))
        assertNull(parseSessionLimitInput("abc", range, otherValue = 5))
        assertNull(parseSessionLimitInput("-1", range, otherValue = 5))
        assertNull(parseSessionLimitInput("501", range, otherValue = 5))
        assertNull(parseSessionLimitInput("0", range, otherValue = 0))
    }

    @Test
    fun `review custom binding updates active and remembered values while presets only update active target`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/settings/SettingsScreen.kt")
        )
        assertTrue(source.contains("customValue = runtimeConfiguration.customReviewItemsPerSession"))
        assertTrue(source.contains("reviewItemsPerSession = it,"))
        assertTrue(source.contains("customReviewItemsPerSession = it"))
        assertTrue(source.contains("onValidValue(preset)"))
        assertTrue(source.contains("onValidCustomValue(parsed)"))
    }
}
