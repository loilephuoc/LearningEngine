package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.desktop.shortcut.DesktopKeyChord
import vn.loi.learning.desktop.shortcut.DesktopShortcutKey
import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand
import java.nio.file.Path

enum class StudyKeyboardAction {
    RETRY_LOAD,
    START_STUDY,
    REVEAL_ANSWER,
    REVIEW_AGAIN,
    REVIEW_HARD,
    REVIEW_GOOD,
    REVIEW_EASY,
    COMPLETE_FORCED_AGAIN,
    RETRY_AUTOMATIC_TYPING,
    REPLAY_PRIMARY_AUDIO,
    TOGGLE_VOCABULARY_AUDIO_LOOP,
    TOGGLE_EXAMPLE_AUDIO_LOOP,
    PLAY_VIETNAMESE_MEANING_AUDIO,
    PLAY_VIETNAMESE_EXAMPLE_AUDIO,
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
    if (
        !input.textInputFocused &&
        uiState.typingRatingMode == TypingRatingMode.FORCED_AGAIN &&
        uiState.workspaceState is ReviewWorkspaceState.AnswerRevealed &&
        input.chord.key in
            setOf(
                DesktopShortcutKey.ENTER,
                DesktopShortcutKey.SPACE,
                DesktopShortcutKey.ONE,
                DesktopShortcutKey.TWO,
                DesktopShortcutKey.THREE,
                DesktopShortcutKey.FOUR
            )
    ) {
        return StudyKeyboardAction.COMPLETE_FORCED_AGAIN
    }
    if (
        !input.textInputFocused &&
        uiState.typingRatingMode == TypingRatingMode.AUTOMATIC_PENDING &&
        uiState.workspaceState is ReviewWorkspaceState.AnswerRevealed &&
        input.chord.key in
            setOf(
                DesktopShortcutKey.ENTER,
                DesktopShortcutKey.SPACE,
                DesktopShortcutKey.ONE,
                DesktopShortcutKey.TWO,
                DesktopShortcutKey.THREE,
                DesktopShortcutKey.FOUR
            )
    ) {
        return StudyKeyboardAction.RETRY_AUTOMATIC_TYPING
    }
    val command = registry.commandFor(input.chord) ?: return null
    if (command == StudyShortcutCommand.PAUSE && uiState.hasActiveSession) {
        return StudyKeyboardAction.PAUSE_WORKSPACE
    }
    if (input.textInputFocused) return null
    if (
        uiState.typingRatingMode == TypingRatingMode.AUTOMATIC_PENDING &&
        uiState.workspaceState is ReviewWorkspaceState.AnswerRevealed &&
        command in
            setOf(
                StudyShortcutCommand.REVEAL_ANSWER,
                StudyShortcutCommand.RATE_AGAIN,
                StudyShortcutCommand.RATE_HARD,
                StudyShortcutCommand.RATE_GOOD,
                StudyShortcutCommand.RATE_EASY
            )
    ) {
        return StudyKeyboardAction.RETRY_AUTOMATIC_TYPING
    }
    if (
        uiState.typingRatingMode == TypingRatingMode.FORCED_AGAIN &&
        uiState.workspaceState is ReviewWorkspaceState.AnswerRevealed &&
        command in
            setOf(
                StudyShortcutCommand.REVEAL_ANSWER,
                StudyShortcutCommand.RATE_AGAIN,
                StudyShortcutCommand.RATE_HARD,
                StudyShortcutCommand.RATE_GOOD,
                StudyShortcutCommand.RATE_EASY
            )
    ) {
        return StudyKeyboardAction.COMPLETE_FORCED_AGAIN
    }
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
        StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP ->
            StudyKeyboardAction.TOGGLE_VOCABULARY_AUDIO_LOOP.takeIf { uiState.hasActiveSession }
        StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP ->
            StudyKeyboardAction.TOGGLE_EXAMPLE_AUDIO_LOOP.takeIf { uiState.hasActiveSession }
        StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO ->
            StudyKeyboardAction.PLAY_VIETNAMESE_MEANING_AUDIO.takeIf { uiState.hasActiveSession }
        StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO ->
            StudyKeyboardAction.PLAY_VIETNAMESE_EXAMPLE_AUDIO.takeIf { uiState.hasActiveSession }
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
        uiState.workspaceState is ReviewWorkspaceState.AnswerRevealed ->
            if (uiState.typingRatingMode == TypingRatingMode.FORCED_AGAIN) {
                StudyKeyboardAction.COMPLETE_FORCED_AGAIN
            } else {
                StudyKeyboardAction.REVIEW_GOOD
            }
        else -> null
    }

internal data class StudyShortcutAudioPaths(
    val vocabulary: Path? = null,
    val englishExample: Path? = null,
    val vietnameseMeaning: Path? = null,
    val vietnameseExample: Path? = null
)

internal fun performStudyAudioKeyboardAction(
    action: StudyKeyboardAction,
    paths: StudyShortcutAudioPaths,
    audioController: LearningContentAudioController
): Boolean =
    when (action) {
        StudyKeyboardAction.REPLAY_PRIMARY_AUDIO -> audioController.replayPrimary()
        StudyKeyboardAction.TOGGLE_VOCABULARY_AUDIO_LOOP ->
            paths.vocabulary?.let { audioController.toggleLoop(it); true } ?: false
        StudyKeyboardAction.TOGGLE_EXAMPLE_AUDIO_LOOP ->
            paths.englishExample?.let { audioController.toggleLoop(it); true } ?: false
        StudyKeyboardAction.PLAY_VIETNAMESE_MEANING_AUDIO ->
            paths.vietnameseMeaning?.let { audioController.playOnce(it); true } ?: false
        StudyKeyboardAction.PLAY_VIETNAMESE_EXAMPLE_AUDIO ->
            paths.vietnameseExample?.let { audioController.playOnce(it); true } ?: false
        else -> false
    }
