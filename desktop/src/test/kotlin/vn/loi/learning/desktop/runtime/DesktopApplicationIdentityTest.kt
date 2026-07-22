package vn.loi.learning.desktop.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopApplicationIdentityTest {
    @Test
    fun `identity is stable and filesystem safe`() {
        assertEquals("vn.loi.learning.desktop", DesktopApplicationIdentity.APPLICATION_ID)
        assertEquals("Learning Engine 2.0", DesktopApplicationIdentity.DISPLAY_NAME)
        assertEquals("LearningEngine", DesktopApplicationIdentity.DIRECTORY_NAME)
    }
}
