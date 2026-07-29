package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertContains
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
    fun `default audio actions expose semantic icon live chord tooltip and accessibility`() {
        val status = resolveStudyShortcutStatus(true, ShortcutRegistry.defaults())
        val expected =
            mapOf(
                StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP to "L",
                StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP to "Shift+L",
                StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO to "V",
                StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO to "Shift+V"
            )

        expected.forEach { (command, chord) ->
            val item = status.items.single { it.command == command }
            val cue = resolveStudyAudioToolbarCue(item, compact = false, available = true, "")

            assertEquals(resolveStudyToolbarActionIcon(command), cue.semantic)
            assertEquals(chord, cue.visualChord)
            assertEquals("${cue.actionName}\nShortcut: $chord", cue.tooltip)
            assertEquals("${cue.actionName} — shortcut $chord", cue.contentDescription)
        }
    }

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
        val changedCue =
            resolveStudyAudioToolbarCue(
                changedItem,
                compact = true,
                available = true,
                unavailableReason = ""
            )

        assertEquals(iconBefore, resolveStudyToolbarActionIcon(command))
        assertEquals("Alt+M", changedItem.chordText)
        assertEquals("Alt+M", changedCue.visualChord)
        assertContains(changedCue.tooltip, "Shortcut: Alt+M")
        assertContains(changedCue.contentDescription, "shortcut Alt+M")

        val reset =
            (changed.requestChange(
                command,
                ShortcutRegistry.defaults().chordFor(command)
            ) as ShortcutChangeResult.Changed).registry
        val resetItem =
            resolveStudyShortcutStatus(true, reset).items.single { it.command == command }
        val resetCue =
            resolveStudyAudioToolbarCue(
                resetItem,
                compact = true,
                available = true,
                unavailableReason = ""
            )
        assertEquals(iconBefore, resolveStudyToolbarActionIcon(command))
        assertEquals("V", resetItem.chordText)
        assertEquals("V", resetCue.visualChord)
        assertContains(resetCue.tooltip, "Shortcut: V")
    }

    @Test
    fun `compact cue retains modifier and disabled cue retains identity and reason`() {
        val command = StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO
        val item =
            resolveStudyShortcutStatus(true, ShortcutRegistry.defaults()).items.single {
                it.command == command
            }
        val cue =
            resolveStudyAudioToolbarCue(
                item,
                compact = true,
                available = false,
                unavailableReason = "Vietnamese example audio unavailable"
            )

        assertEquals(resolveStudyToolbarActionIcon(command), cue.semantic)
        assertEquals(item.compactLabel, cue.visualChord)
        assertContains(cue.visualChord, "V")
        assertContains(cue.tooltip, "Shortcut: Shift+V")
        assertContains(cue.tooltip, "Unavailable in this item/stage")
        assertContains(cue.contentDescription, "Unavailable in this item/stage")
    }
}
