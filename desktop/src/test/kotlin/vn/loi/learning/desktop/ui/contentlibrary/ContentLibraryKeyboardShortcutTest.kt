package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContentLibraryKeyboardShortcutTest {
    private val idleContext =
        ContentLibraryKeyboardContext(
            dialogVisible = false,
            lessonBrowserOpen = false,
            lessonSelected = false
        )

    @Test
    fun `control shortcuts refresh and import`() {
        assertEquals(
            ContentLibraryKeyboardAction.Refresh,
            resolveContentLibraryKeyboardAction(
                key = ContentLibraryKeyboardKey.R,
                controlPressed = true,
                context = idleContext
            )
        )

        assertEquals(
            ContentLibraryKeyboardAction.ImportPackage,
            resolveContentLibraryKeyboardAction(
                key = ContentLibraryKeyboardKey.I,
                controlPressed = true,
                context = idleContext
            )
        )
    }

    @Test
    fun `plain letter keys do not trigger global actions`() {
        assertNull(
            resolveContentLibraryKeyboardAction(
                key = ContentLibraryKeyboardKey.R,
                controlPressed = false,
                context = idleContext
            )
        )
        assertNull(
            resolveContentLibraryKeyboardAction(
                key = ContentLibraryKeyboardKey.I,
                controlPressed = false,
                context = idleContext
            )
        )
    }

    @Test
    fun `escape clears selected lesson before closing browser`() {
        assertEquals(
            ContentLibraryKeyboardAction.ClearLessonSelection,
            resolveContentLibraryKeyboardAction(
                key = ContentLibraryKeyboardKey.ESCAPE,
                controlPressed = false,
                context =
                    ContentLibraryKeyboardContext(
                        dialogVisible = false,
                        lessonBrowserOpen = true,
                        lessonSelected = true
                    )
            )
        )
    }

    @Test
    fun `escape closes browser when no lesson is selected`() {
        assertEquals(
            ContentLibraryKeyboardAction.CloseLessonBrowser,
            resolveContentLibraryKeyboardAction(
                key = ContentLibraryKeyboardKey.ESCAPE,
                controlPressed = false,
                context =
                    ContentLibraryKeyboardContext(
                        dialogVisible = false,
                        lessonBrowserOpen = true,
                        lessonSelected = false
                    )
            )
        )
    }

    @Test
    fun `escape is ignored outside browser`() {
        assertNull(
            resolveContentLibraryKeyboardAction(
                key = ContentLibraryKeyboardKey.ESCAPE,
                controlPressed = false,
                context = idleContext
            )
        )
    }

    @Test
    fun `screen shortcuts are suspended while a dialog is open`() {
        val dialogContext =
            ContentLibraryKeyboardContext(
                dialogVisible = true,
                lessonBrowserOpen = true,
                lessonSelected = true
            )

        ContentLibraryKeyboardKey.entries.forEach { key ->
            assertNull(
                resolveContentLibraryKeyboardAction(
                    key = key,
                    controlPressed = key != ContentLibraryKeyboardKey.ESCAPE,
                    context = dialogContext
                )
            )
        }
    }

    @Test
    fun `visible shortcut hint documents the complete contract`() {
        assertEquals(
            "Shortcuts: Ctrl+R refresh, Ctrl+I import package, Escape go back.",
            contentLibraryKeyboardHint()
        )
    }
}
