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
