package vn.loi.learning.desktop.ui.startup

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopStartupStateTest {
    @Test
    fun `startup transitions one way into ready shell`() {
        assertEquals(DesktopStartupState.READY, DesktopStartupState.STARTING.complete())
        assertEquals(DesktopStartupState.READY, DesktopStartupState.READY.complete())
    }
}
