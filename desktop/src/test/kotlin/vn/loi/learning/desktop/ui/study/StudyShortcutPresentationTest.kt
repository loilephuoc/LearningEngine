package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertContains
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

        assertContains(status.text, "[Enter] Reveal/Next")
        assertFalse(status.text.contains("[Space]"))
        assertContains(status.text, "[R] Replay")
        assertContains(status.text, "[Ctrl+Z] Undo")
        assertContains(status.text, "[Esc] Pause")
    }

    @Test
    fun `rating status follows configured rating bindings`() {
        val changed = ShortcutRegistry.defaults().requestChange(
            StudyShortcutCommand.RATE_GOOD,
            DesktopKeyChord(DesktopShortcutKey.G)
        ) as ShortcutChangeResult.Changed

        val status = resolveStudyShortcutStatus(ratingReady = true, registry = changed.registry)

        assertContains(status.text, "[G] Good")
        assertFalse(status.text.contains("[3] Good"))
    }
}
