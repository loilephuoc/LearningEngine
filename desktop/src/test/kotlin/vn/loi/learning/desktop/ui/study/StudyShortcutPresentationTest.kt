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
            listOf("Enter", "R", "L", "Shift+L", "V", "Shift+V", "Ctrl+Z", "Esc"),
            status.items.map(StudyShortcutStatusItem::chordText)
        )
        assertContains(status.accessibleDescription, "Enter = Reveal/Next")
        assertFalse(status.items.any { it.chordText == "Space" })
        assertContains(status.accessibleDescription, "R = Replay")
        assertContains(status.accessibleDescription, "Ctrl+Z = Undo")
        assertContains(status.accessibleDescription, "Esc = Pause")
        assertEquals(
            listOf("L", "⇧L", "V", "⇧V"),
            status.items.filter { it.command in audioCommands }.map {
                it.compactLabel
            }
        )
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

    @Test
    fun `audio toolbar chord follows changed and reset runtime registry`() {
        val command = StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP
        val replacement = DesktopKeyChord(DesktopShortcutKey.M, altPressed = true)
        val changed =
            (ShortcutRegistry.defaults().requestChange(
                command,
                replacement
            ) as ShortcutChangeResult.Changed).registry

        val changedItem =
            resolveStudyShortcutStatus(true, changed).items.single { it.command == command }
        assertEquals("Alt+M", changedItem.compactLabel)

        val reset =
            (changed.requestChange(
                command,
                ShortcutRegistry.defaults().chordFor(command)
            ) as ShortcutChangeResult.Changed).registry
        val resetItem =
            resolveStudyShortcutStatus(true, reset).items.single { it.command == command }
        assertEquals("L", resetItem.compactLabel)
    }

    private val audioCommands =
        setOf(
            StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP,
            StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP,
            StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO,
            StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO
        )
}
