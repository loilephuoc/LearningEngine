package vn.loi.learning.desktop.ui.study

import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand

data class StudyShortcutStatusPresentation(
    val text: String
)

fun resolveStudyShortcutStatus(
    ratingReady: Boolean,
    registry: ShortcutRegistry
): StudyShortcutStatusPresentation {
    val commands =
        if (ratingReady) {
            listOf(
                StudyShortcutCommand.RATE_AGAIN,
                StudyShortcutCommand.RATE_HARD,
                StudyShortcutCommand.RATE_GOOD,
                StudyShortcutCommand.RATE_EASY,
                StudyShortcutCommand.REPLAY_PRIMARY_AUDIO,
                StudyShortcutCommand.UNDO
            )
        } else {
            listOf(
                StudyShortcutCommand.REVEAL_ANSWER,
                StudyShortcutCommand.REPLAY_PRIMARY_AUDIO,
                StudyShortcutCommand.UNDO,
                StudyShortcutCommand.PAUSE
            )
        }
    return StudyShortcutStatusPresentation(
        commands.joinToString("  ") { command ->
            val label =
                when (command) {
                    StudyShortcutCommand.REVEAL_ANSWER -> "Reveal/Next"
                    StudyShortcutCommand.RATE_AGAIN -> "Again"
                    StudyShortcutCommand.RATE_HARD -> "Hard"
                    StudyShortcutCommand.RATE_GOOD -> "Good"
                    StudyShortcutCommand.RATE_EASY -> "Easy"
                    StudyShortcutCommand.REPLAY_PRIMARY_AUDIO -> "Replay"
                    StudyShortcutCommand.UNDO -> "Undo"
                    StudyShortcutCommand.PAUSE -> "Pause"
                }
            "[${registry.chordFor(command).displayName}] $label"
        }
    )
}
