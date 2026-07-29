package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import vn.loi.learning.desktop.shortcut.DesktopKeyChord
import vn.loi.learning.desktop.shortcut.DesktopShortcutKey
import vn.loi.learning.desktop.shortcut.ShortcutChangeResult
import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand

class StudyToolbarActionIconTest {
    @Test
    fun `audio actions resolve stable distinct semantic icons and Vietnamese badges`() {
        val vocabulary =
            requireNotNull(
                resolveStudyToolbarActionIcon(
                    StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP
                )
            )
        val example =
            requireNotNull(
                resolveStudyToolbarActionIcon(
                    StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP
                )
            )
        val meaning =
            requireNotNull(
                resolveStudyToolbarActionIcon(
                    StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO
                )
            )
        val vietnameseExample =
            requireNotNull(
                resolveStudyToolbarActionIcon(
                    StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO
                )
            )

        assertNotEquals(vocabulary.icon, example.icon)
        assertNotEquals(meaning.icon, vietnameseExample.icon)
        assertNull(vocabulary.localeBadge)
        assertNull(example.localeBadge)
        assertEquals("VI", meaning.localeBadge)
        assertEquals("VI+", vietnameseExample.localeBadge)
    }

    @Test
    fun `shortcut change and reset update metadata without changing semantic icon`() {
        val command = StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO
        val iconBefore = resolveStudyToolbarActionIcon(command)
        val changed =
            (ShortcutRegistry.defaults().requestChange(
                command,
                DesktopKeyChord(DesktopShortcutKey.M, altPressed = true)
            ) as ShortcutChangeResult.Changed).registry
        val changedItem =
            resolveStudyShortcutStatus(true, changed).items.single { it.command == command }

        assertEquals(iconBefore, resolveStudyToolbarActionIcon(command))
        assertEquals("Alt+M", changedItem.chordText)

        val reset =
            (changed.requestChange(
                command,
                ShortcutRegistry.defaults().chordFor(command)
            ) as ShortcutChangeResult.Changed).registry
        val resetItem =
            resolveStudyShortcutStatus(true, reset).items.single { it.command == command }
        assertEquals(iconBefore, resolveStudyToolbarActionIcon(command))
        assertEquals("V", resetItem.chordText)
    }
}
