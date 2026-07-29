package vn.loi.learning.desktop.ui.study

import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand

data class StudyShortcutStatusItem(
    val command: StudyShortcutCommand,
    val chordText: String,
    val compactLabel: String,
    val fullAccessibleLabel: String,
    val priority: Int
)

data class StudyShortcutStatusPresentation(
    val items: List<StudyShortcutStatusItem>
) {
    val accessibleDescription: String =
        items.joinToString(". ") { "${it.chordText} = ${it.fullAccessibleLabel}" }
}

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
        items =
            commands.map { command ->
                val label = when (command) {
                    StudyShortcutCommand.REVEAL_ANSWER -> "Reveal/Next"
                    StudyShortcutCommand.RATE_AGAIN -> "Again"
                    StudyShortcutCommand.RATE_HARD -> "Hard"
                    StudyShortcutCommand.RATE_GOOD -> "Good"
                    StudyShortcutCommand.RATE_EASY -> "Easy"
                    StudyShortcutCommand.REPLAY_PRIMARY_AUDIO -> "Replay"
                    StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP -> "Loop word"
                    StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP -> "Loop example"
                    StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO -> "Vietnamese meaning"
                    StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO -> "Vietnamese example"
                    StudyShortcutCommand.UNDO -> "Undo"
                    StudyShortcutCommand.PAUSE -> "Pause"
                }
                StudyShortcutStatusItem(
                    command = command,
                    chordText = registry.chordFor(command).displayName,
                    compactLabel = registry.chordFor(command).displayName,
                    fullAccessibleLabel = label,
                    priority =
                        when (command) {
                            StudyShortcutCommand.REVEAL_ANSWER,
                            StudyShortcutCommand.RATE_AGAIN,
                            StudyShortcutCommand.RATE_HARD,
                            StudyShortcutCommand.RATE_GOOD,
                            StudyShortcutCommand.RATE_EASY -> 0
                            StudyShortcutCommand.REPLAY_PRIMARY_AUDIO -> 1
                            StudyShortcutCommand.UNDO -> 2
                            StudyShortcutCommand.PAUSE -> 3
                            else -> 4
                        }
                )
            }
    )
}
