package vn.loi.learning.desktop.shortcut

import kotlin.test.Test
import kotlin.test.assertEquals

class ShortcutChordFormatterTest {
    @Test
    fun `compact formatter shortens shift without losing complex modifiers`() {
        assertEquals(
            "⇧L",
            ShortcutChordFormatter.format(
                DesktopKeyChord(DesktopShortcutKey.L, shiftPressed = true),
                compact = true
            )
        )
        assertEquals(
            "Alt+M",
            ShortcutChordFormatter.format(
                DesktopKeyChord(DesktopShortcutKey.M, altPressed = true),
                compact = true
            )
        )
        assertEquals(
            "Ctrl+Alt+⇧M",
            ShortcutChordFormatter.format(
                DesktopKeyChord(
                    DesktopShortcutKey.M,
                    controlPressed = true,
                    altPressed = true,
                    shiftPressed = true
                ),
                compact = true
            )
        )
    }

    @Test
    fun `change and reset preserve one registry authority for every audio command`() {
        val changes =
            listOf(
                StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP to
                    DesktopKeyChord(DesktopShortcutKey.M, altPressed = true),
                StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP to
                    DesktopKeyChord(DesktopShortcutKey.N, altPressed = true),
                StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO to
                    DesktopKeyChord(DesktopShortcutKey.B, altPressed = true),
                StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO to
                    DesktopKeyChord(DesktopShortcutKey.C, altPressed = true)
            )

        changes.forEach { (command, replacement) ->
            val defaults = ShortcutRegistry.defaults()
            val original = defaults.chordFor(command)
            val changed =
                (defaults.requestChange(command, replacement) as ShortcutChangeResult.Changed)
                    .registry

            assertEquals(command, changed.commandFor(replacement))
            assertEquals(null, changed.commandFor(original))
            assertEquals(
                ShortcutChordFormatter.format(replacement, compact = true),
                ShortcutChordFormatter.format(changed.chordFor(command), compact = true)
            )

            val reset =
                (changed.requestChange(command, original) as ShortcutChangeResult.Changed).registry
            assertEquals(defaults, reset)
            assertEquals(command, reset.commandFor(original))
        }
    }
}
