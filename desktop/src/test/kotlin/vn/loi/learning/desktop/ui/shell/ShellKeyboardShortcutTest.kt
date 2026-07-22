package vn.loi.learning.desktop.ui.shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.desktop.ui.navigation.NavigationDestination

class ShellKeyboardShortcutTest {
    @Test
    fun `function keys map directly to every destination`() {
        val expected =
            listOf(
                ShellKeyboardKey.F1 to NavigationDestination.DASHBOARD,
                ShellKeyboardKey.F2 to NavigationDestination.STUDY,
                ShellKeyboardKey.F3 to NavigationDestination.STATISTICS,
                ShellKeyboardKey.F4 to NavigationDestination.REVIEW_HISTORY,
                ShellKeyboardKey.F5 to NavigationDestination.CONTENT_LIBRARY,
                ShellKeyboardKey.F6 to NavigationDestination.SETTINGS
            )

        expected.forEach { (key, destination) ->
            assertEquals(
                ShellKeyboardAction.Navigate(destination),
                resolveShellKeyboardAction(
                    key = key,
                    controlPressed = false,
                    shiftPressed = false
                )
            )
        }
    }

    @Test
    fun `control page keys traverse shell destinations`() {
        assertEquals(
            ShellKeyboardAction.NavigatePrevious,
            resolveShellKeyboardAction(
                key = ShellKeyboardKey.PAGE_UP,
                controlPressed = true,
                shiftPressed = false
            )
        )

        assertEquals(
            ShellKeyboardAction.NavigateNext,
            resolveShellKeyboardAction(
                key = ShellKeyboardKey.PAGE_DOWN,
                controlPressed = true,
                shiftPressed = false
            )
        )
    }

    @Test
    fun `refresh requires control shift r`() {
        assertEquals(
            ShellKeyboardAction.RefreshCurrent,
            resolveShellKeyboardAction(
                key = ShellKeyboardKey.R,
                controlPressed = true,
                shiftPressed = true
            )
        )

        assertNull(
            resolveShellKeyboardAction(
                key = ShellKeyboardKey.R,
                controlPressed = true,
                shiftPressed = false
            )
        )
    }

    @Test
    fun `modified function keys do not steal local controls`() {
        assertNull(
            resolveShellKeyboardAction(
                key = ShellKeyboardKey.F1,
                controlPressed = true,
                shiftPressed = false
            )
        )

        assertNull(
            resolveShellKeyboardAction(
                key = ShellKeyboardKey.F5,
                controlPressed = false,
                shiftPressed = true
            )
        )
    }

    @Test
    fun `each destination exposes its visible function key`() {
        assertEquals("F1", NavigationDestination.DASHBOARD.shortcutLabel())
        assertEquals("F2", NavigationDestination.STUDY.shortcutLabel())
        assertEquals("F3", NavigationDestination.STATISTICS.shortcutLabel())
        assertEquals("F4", NavigationDestination.REVIEW_HISTORY.shortcutLabel())
        assertEquals("F5", NavigationDestination.CONTENT_LIBRARY.shortcutLabel())
        assertEquals("F6", NavigationDestination.SETTINGS.shortcutLabel())
    }

    @Test
    fun `global hint documents navigation traversal and refresh`() {
        assertEquals(
            "Global shortcuts: F1 Home, F2 Learn, F3 Statistics, " +
                "F4 Review, F5 Library, F6 Settings, " +
                "Ctrl+PageUp previous screen, Ctrl+PageDown next screen, " +
                "Ctrl+Shift+R refresh current screen.",
            shellKeyboardHint()
        )
    }

    @Test
    fun `navigation registry exposes stable shell route identifiers`() {
        assertEquals(
            listOf("home", "learn", "statistics", "review", "library", "settings"),
            NavigationDestination.entries.map(NavigationDestination::routeId)
        )
    }
}
