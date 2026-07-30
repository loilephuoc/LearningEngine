package vn.loi.learning.desktop.ui.study

import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand

enum class StudyActionControl {
    RETRY_LOAD,
    START_STUDY,
    START_GENERAL_STUDY,
    REVEAL_ANSWER,
    REVIEW_AGAIN,
    REVIEW_HARD,
    REVIEW_GOOD,
    REVIEW_EASY,
    UNDO_LATEST,
    PAUSE_WORKSPACE
}

data class StudyActionAccessibility(
    val visibleLabel: String,
    val shortcutHint: String,
    val contentDescription: String
)

fun resolveStudyActionAccessibility(
    control: StudyActionControl,
    strings: StudyWorkspaceStrings = StudyWorkspaceStrings.ENGLISH,
    registry: ShortcutRegistry = ShortcutRegistry.defaults()
): StudyActionAccessibility {
    val command = when (control) {
        StudyActionControl.RETRY_LOAD,
        StudyActionControl.START_STUDY,
        StudyActionControl.START_GENERAL_STUDY,
        StudyActionControl.REVEAL_ANSWER -> StudyShortcutCommand.REVEAL_ANSWER
        StudyActionControl.REVIEW_AGAIN -> StudyShortcutCommand.RATE_AGAIN
        StudyActionControl.REVIEW_HARD -> StudyShortcutCommand.RATE_HARD
        StudyActionControl.REVIEW_GOOD -> StudyShortcutCommand.RATE_GOOD
        StudyActionControl.REVIEW_EASY -> StudyShortcutCommand.RATE_EASY
        StudyActionControl.UNDO_LATEST -> StudyShortcutCommand.UNDO
        StudyActionControl.PAUSE_WORKSPACE -> StudyShortcutCommand.PAUSE
    }
    val shortcut = registry.chordFor(command).displayName
    val label = strings.label(control)
    val accessibleShortcut =
        if (control == StudyActionControl.REVIEW_GOOD) "$shortcut or Space" else shortcut
    return StudyActionAccessibility(
        label,
        shortcut,
        strings.shortcutTemplate(label, accessibleShortcut)
    )
}
