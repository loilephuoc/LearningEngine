package vn.loi.learning.desktop.ui.study

import vn.loi.learning.desktop.ui.designsystem.components.base.LEButtonVariant
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant

internal val studyRatingOrder: List<StudyActionControl> = listOf(
    StudyActionControl.REVIEW_AGAIN,
    StudyActionControl.REVIEW_HARD,
    StudyActionControl.REVIEW_GOOD,
    StudyActionControl.REVIEW_EASY
)

internal fun resolveStudyRatingVariant(control: StudyActionControl): LEButtonVariant =
    when (control) {
        StudyActionControl.REVIEW_AGAIN -> LEButtonVariant.RATING_AGAIN
        StudyActionControl.REVIEW_HARD -> LEButtonVariant.RATING_HARD
        StudyActionControl.REVIEW_GOOD -> LEButtonVariant.RATING_GOOD
        StudyActionControl.REVIEW_EASY -> LEButtonVariant.RATING_EASY
        else -> LEButtonVariant.PRIMARY
    }

internal object StudySurfaceRoles {
    val answer = LESurfaceVariant.ANSWER
    val meaning = LESurfaceVariant.MEANING
    val example = LESurfaceVariant.EXAMPLE
    val scheduler = LESurfaceVariant.SCHEDULER
    val ratingDock = LESurfaceVariant.RATING_DOCK
}
