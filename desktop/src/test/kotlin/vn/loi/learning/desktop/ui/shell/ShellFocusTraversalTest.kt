package vn.loi.learning.desktop.ui.shell

import kotlin.test.Test
import kotlin.test.assertEquals

class ShellFocusTraversalTest {
    @Test
    fun `focus traversal cycles deterministically between shell regions`() {
        assertEquals(ShellFocusRegion.CONTENT, ShellFocusRegion.NAVIGATION.next())
        assertEquals(ShellFocusRegion.NAVIGATION, ShellFocusRegion.CONTENT.next())
        assertEquals(ShellFocusRegion.CONTENT, ShellFocusRegion.NAVIGATION.previous())
        assertEquals(ShellFocusRegion.NAVIGATION, ShellFocusRegion.CONTENT.previous())
    }
}
