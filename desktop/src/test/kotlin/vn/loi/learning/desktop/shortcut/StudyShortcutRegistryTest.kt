package vn.loi.learning.desktop.shortcut

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class StudyShortcutRegistryTest {
    @Test
    fun `defaults serialize and deserialize without Compose keys`() {
        val defaults = ShortcutRegistry.defaults()
        val serialized = defaults.serialize()

        assertEquals(defaults, ShortcutRegistry.deserialize(serialized))
        assertEquals("Space", defaults.chordFor(StudyShortcutCommand.REVEAL_ANSWER).displayName)
        assertEquals("Ctrl+Z", defaults.chordFor(StudyShortcutCommand.UNDO).displayName)
        assertNull(defaults.commandFor(DesktopKeyChord(DesktopShortcutKey.ENTER)))
    }

    @Test
    fun `duplicate request reports owning command without mutating registry`() {
        val defaults = ShortcutRegistry.defaults()
        val result = defaults.requestChange(
            StudyShortcutCommand.RATE_GOOD,
            defaults.chordFor(StudyShortcutCommand.RATE_AGAIN)
        )

        val conflict = assertIs<ShortcutChangeResult.Conflict>(result).conflict
        assertEquals(StudyShortcutCommand.RATE_GOOD, conflict.requestedCommand)
        assertEquals(StudyShortcutCommand.RATE_AGAIN, conflict.occupiedBy)
        assertEquals(StudyShortcutCommand.RATE_GOOD, defaults.commandFor(DesktopKeyChord(DesktopShortcutKey.THREE)))
    }

    @Test
    fun `swap exchanges bindings and cancel preserves them`() {
        val defaults = ShortcutRegistry.defaults()
        val conflict = assertIs<ShortcutChangeResult.Conflict>(
            defaults.requestChange(
                StudyShortcutCommand.RATE_GOOD,
                DesktopKeyChord(DesktopShortcutKey.ONE)
            )
        ).conflict

        val swapped = defaults.resolveConflict(conflict, ShortcutConflictResolution.SWAP)
        assertEquals(DesktopShortcutKey.ONE, swapped.chordFor(StudyShortcutCommand.RATE_GOOD).key)
        assertEquals(DesktopShortcutKey.THREE, swapped.chordFor(StudyShortcutCommand.RATE_AGAIN).key)
        assertEquals(defaults, defaults.resolveConflict(conflict, ShortcutConflictResolution.CANCEL))
    }

    @Test
    fun `replace preserves a complete duplicate-free registry`() {
        val defaults = ShortcutRegistry.defaults()
        val conflict = assertIs<ShortcutChangeResult.Conflict>(
            defaults.requestChange(
                StudyShortcutCommand.REVEAL_ANSWER,
                DesktopKeyChord(DesktopShortcutKey.R)
            )
        ).conflict

        val replaced = defaults.resolveConflict(conflict, ShortcutConflictResolution.REPLACE)
        assertEquals(StudyShortcutCommand.REVEAL_ANSWER, replaced.commandFor(DesktopKeyChord(DesktopShortcutKey.R)))
        assertEquals(StudyShortcutCommand.REPLAY_PRIMARY_AUDIO, replaced.commandFor(DesktopKeyChord(DesktopShortcutKey.SPACE)))
        assertEquals(StudyShortcutCommand.entries.size, replaced.bindings.map { it.chord }.toSet().size)
    }

    @Test
    fun `malformed incomplete and duplicate serialized registries are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ShortcutRegistry.deserialize("REVEAL_ANSWER=")
        }
        assertFailsWith<IllegalArgumentException> {
            ShortcutRegistry.deserialize("REVEAL_ANSWER=SPACE")
        }
        val duplicate = ShortcutRegistry.defaults().serialize()
            .replace("RATE_HARD=TWO", "RATE_HARD=ONE")
        assertFailsWith<IllegalArgumentException> {
            ShortcutRegistry.deserialize(duplicate)
        }
        assertFailsWith<IllegalArgumentException> {
            ShortcutRegistry.deserialize(
                ShortcutRegistry.defaults().serialize() + ",REVEAL_ANSWER=ENTER"
            )
        }
    }
}
