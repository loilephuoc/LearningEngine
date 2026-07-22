package vn.loi.learning.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopStartupVerificationTest {
    @Test
    fun `startup verification requires an explicit true property`() {
        assertTrue(isDesktopStartupVerificationRequested("true"))
        assertFalse(isDesktopStartupVerificationRequested("false"))
        assertFalse(isDesktopStartupVerificationRequested("TRUE"))
        assertFalse(isDesktopStartupVerificationRequested("unexpected"))
        assertFalse(isDesktopStartupVerificationRequested(null))
    }
}
