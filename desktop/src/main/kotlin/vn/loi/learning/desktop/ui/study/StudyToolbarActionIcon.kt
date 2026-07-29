package vn.loi.learning.desktop.ui.study

import vn.loi.learning.desktop.shortcut.StudyShortcutCommand

enum class StudyToolbarSemanticIcon {
    REPLAY_AUDIO,
    LOOP_VOCABULARY_AUDIO,
    LOOP_EXAMPLE_AUDIO,
    PLAY_VIETNAMESE_MEANING,
    PLAY_VIETNAMESE_EXAMPLE,
    UNDO,
    SESSION_STATUS
}

data class StudyToolbarActionIconPresentation(
    val icon: StudyToolbarSemanticIcon,
    val localeBadge: String? = null
)

data class StudyAudioToolbarCue(
    val semantic: StudyToolbarActionIconPresentation,
    val actionName: String,
    val visualChord: String,
    val tooltip: String,
    val contentDescription: String
)

internal fun resolveStudyToolbarActionIcon(
    command: StudyShortcutCommand
): StudyToolbarActionIconPresentation? =
    when (command) {
        StudyShortcutCommand.REPLAY_PRIMARY_AUDIO ->
            StudyToolbarActionIconPresentation(StudyToolbarSemanticIcon.REPLAY_AUDIO)
        StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP ->
            StudyToolbarActionIconPresentation(StudyToolbarSemanticIcon.LOOP_VOCABULARY_AUDIO)
        StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP ->
            StudyToolbarActionIconPresentation(StudyToolbarSemanticIcon.LOOP_EXAMPLE_AUDIO)
        StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO ->
            StudyToolbarActionIconPresentation(
                StudyToolbarSemanticIcon.PLAY_VIETNAMESE_MEANING,
                localeBadge = "VI"
            )
        StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO ->
            StudyToolbarActionIconPresentation(
                StudyToolbarSemanticIcon.PLAY_VIETNAMESE_EXAMPLE,
                localeBadge = "VI+"
            )
        StudyShortcutCommand.UNDO ->
            StudyToolbarActionIconPresentation(StudyToolbarSemanticIcon.UNDO)
        else -> null
    }

internal fun resolveStudyAudioToolbarCue(
    item: StudyShortcutStatusItem,
    compact: Boolean,
    available: Boolean,
    unavailableReason: String
): StudyAudioToolbarCue {
    val semantic = requireNotNull(resolveStudyToolbarActionIcon(item.command))
    val actionName = studyAudioToolbarActionName(item.command)
    val unavailableLine =
        if (available) "" else "\nUnavailable in this item/stage: $unavailableReason"
    return StudyAudioToolbarCue(
        semantic = semantic,
        actionName = actionName,
        visualChord = if (compact) item.compactLabel else item.chordText,
        tooltip = "$actionName\nShortcut: ${item.chordText}$unavailableLine",
        contentDescription =
            "$actionName — shortcut ${item.chordText}" +
                if (available) "" else ". Unavailable in this item/stage: $unavailableReason"
    )
}

internal fun studyAudioToolbarActionName(command: StudyShortcutCommand): String =
    when (command) {
        StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP -> "Loop Vocabulary Audio"
        StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP -> "Loop Example Audio"
        StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO -> "Play Vietnamese Meaning"
        StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO -> "Play Vietnamese Example"
        else -> error("$command is not an audio toolbar action")
    }
