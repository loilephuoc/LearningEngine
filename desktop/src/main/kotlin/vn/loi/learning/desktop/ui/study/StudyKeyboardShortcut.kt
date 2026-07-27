package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.desktop.shortcut.DesktopKeyChord
import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand

enum class StudyKeyboardAction {
    RETRY_LOAD,
    START_STUDY,
    REVEAL_ANSWER,
    REVIEW_AGAIN,
    REVIEW_HARD,
    REVIEW_GOOD,
    REVIEW_EASY,
    REPLAY_PRIMARY_AUDIO,
    UNDO_LATEST,
    PAUSE_WORKSPACE
}

data class StudyKeyboardInput(
    val chord: DesktopKeyChord,
    val textInputFocused: Boolean = false,
    val repeated: Boolean = false
)

fun resolveStudyKeyboardAction(
    uiState: StudyUiState,
    input: StudyKeyboardInput,
    registry: ShortcutRegistry = ShortcutRegistry.defaults()
): StudyKeyboardAction? {
    if (uiState.actionInProgress || input.repeated) return null
    val command = registry.commandFor(input.chord) ?: return null
    if (command == StudyShortcutCommand.PAUSE && uiState.hasActiveSession) {
        return StudyKeyboardAction.PAUSE_WORKSPACE
    }
    if (input.textInputFocused) return null
    return when (command) {
        StudyShortcutCommand.REVEAL_ANSWER -> resolvePrimaryAction(uiState)
        StudyShortcutCommand.RATE_AGAIN ->
            StudyKeyboardAction.REVIEW_AGAIN.takeIf { uiState.workspaceState is ReviewWorkspaceState.AnswerRevealed }
        StudyShortcutCommand.RATE_HARD ->
            StudyKeyboardAction.REVIEW_HARD.takeIf { uiState.workspaceState is ReviewWorkspaceState.AnswerRevealed }
        StudyShortcutCommand.RATE_GOOD ->
            StudyKeyboardAction.REVIEW_GOOD.takeIf { uiState.workspaceState is ReviewWorkspaceState.AnswerRevealed }
        StudyShortcutCommand.RATE_EASY ->
            StudyKeyboardAction.REVIEW_EASY.takeIf { uiState.workspaceState is ReviewWorkspaceState.AnswerRevealed }
        StudyShortcutCommand.REPLAY_PRIMARY_AUDIO ->
            StudyKeyboardAction.REPLAY_PRIMARY_AUDIO.takeIf {
                uiState.hasActiveSession &&
                    uiState.learningContent?.question?.blocks?.any { it is LearningContentBlock.Audio } == true
            }
        StudyShortcutCommand.UNDO ->
            StudyKeyboardAction.UNDO_LATEST.takeIf { uiState.canUndo }
        StudyShortcutCommand.PAUSE -> null
    }
}

private fun resolvePrimaryAction(uiState: StudyUiState): StudyKeyboardAction? =
    when {
        uiState.workspaceState.allows(ReviewWorkspaceAction.Retry) -> StudyKeyboardAction.RETRY_LOAD
        uiState.workspaceState.allows(ReviewWorkspaceAction.Start) -> StudyKeyboardAction.START_STUDY
        uiState.workspaceState.allows(ReviewWorkspaceAction.ShowAnswer) -> StudyKeyboardAction.REVEAL_ANSWER
        else -> null
    }
