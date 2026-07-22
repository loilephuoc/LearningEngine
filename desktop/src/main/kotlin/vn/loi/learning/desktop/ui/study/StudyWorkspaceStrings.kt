package vn.loi.learning.desktop.ui.study

data class StudyWorkspaceStrings(
    val labels: Map<StudyActionControl, String>,
    val shortcutTemplate: (String, String) -> String
) {
    fun label(control: StudyActionControl): String = requireNotNull(labels[control])

    companion object {
        val ENGLISH = StudyWorkspaceStrings(
            labels = mapOf(
                StudyActionControl.RETRY_LOAD to "Retry Load",
                StudyActionControl.START_STUDY to "Start Study",
                StudyActionControl.START_GENERAL_STUDY to "Start General Study",
                StudyActionControl.REVEAL_ANSWER to "Reveal Answer",
                StudyActionControl.REVIEW_AGAIN to "Again",
                StudyActionControl.REVIEW_HARD to "Hard",
                StudyActionControl.REVIEW_GOOD to "Good",
                StudyActionControl.REVIEW_EASY to "Easy",
                StudyActionControl.UNDO_LATEST to "Undo latest rating",
                StudyActionControl.PAUSE_WORKSPACE to "Pause"
            ),
            shortcutTemplate = { label, shortcut -> "$label. Keyboard shortcut: $shortcut." }
        )
    }
}
