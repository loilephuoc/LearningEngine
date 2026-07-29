package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import vn.loi.learning.desktop.shortcut.DesktopKeyChord
import vn.loi.learning.desktop.shortcut.DesktopShortcutKey
import vn.loi.learning.desktop.shortcut.ShortcutChangeResult
import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand

class StudyShortcutPresentationTest {
    @Test
    fun `status strip renders current registry rather than hardcoded keys`() {
        val changed = ShortcutRegistry.defaults().requestChange(
            StudyShortcutCommand.REVEAL_ANSWER,
            DesktopKeyChord(DesktopShortcutKey.ENTER)
        ) as ShortcutChangeResult.Changed

        val status = resolveStudyShortcutStatus(ratingReady = false, registry = changed.registry)

        assertEquals(
            listOf("Enter", "R", "Ctrl+Z", "Esc"),
            status.items.map(StudyShortcutStatusItem::chordText)
        )
        assertContains(status.accessibleDescription, "Enter = Reveal/Next")
        assertFalse(status.items.any { it.chordText == "Space" })
        assertContains(status.accessibleDescription, "R = Replay")
        assertContains(status.accessibleDescription, "Ctrl+Z = Undo")
        assertContains(status.accessibleDescription, "Esc = Pause")
    }

    @Test
    fun `rating status follows configured rating bindings`() {
        val changed = ShortcutRegistry.defaults().requestChange(
            StudyShortcutCommand.RATE_GOOD,
            DesktopKeyChord(DesktopShortcutKey.G)
        ) as ShortcutChangeResult.Changed

        val status = resolveStudyShortcutStatus(ratingReady = true, registry = changed.registry)

        assertEquals(
            "G",
            status.items.single { it.command == StudyShortcutCommand.RATE_GOOD }.chordText
        )
        assertContains(status.accessibleDescription, "G = Good")
        assertFalse(status.items.any {
            it.command == StudyShortcutCommand.RATE_GOOD && it.chordText == "3"
        })
    }
}
