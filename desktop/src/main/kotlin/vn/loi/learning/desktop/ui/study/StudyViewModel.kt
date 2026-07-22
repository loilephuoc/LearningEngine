package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.loi.learning.domain.study.memory.model.ReviewRating

class StudyViewModel(
    private val facade: StudyFacade,
    private val onStudyDataChanged: (() -> Unit)? = null
) {
    private var actionInProgress = false

    var uiState by mutableStateOf(loadSafely())
        private set

    fun refresh() {
        uiState = loadSafely(previousState = uiState)
    }

    fun startStudy() = updateSafely(StudyFailureKind.PREPARATION) { facade.startStudy() }

    fun startLessonStudy(contentId: String) =
        updateSafely(StudyFailureKind.PREPARATION) { facade.startLessonStudy(contentId) }

    fun revealAnswer() = updateSafely(StudyFailureKind.CONTENT) { facade.revealAnswer() }

    fun reviewAgain() = review(ReviewRating.AGAIN)
    fun reviewHard() = review(ReviewRating.HARD)
    fun reviewGood() = review(ReviewRating.GOOD)
    fun reviewEasy() = review(ReviewRating.EASY)

    fun undoLatestReview() {
        val succeeded = updateSafely(StudyFailureKind.UNDO) { facade.undoLatestReview() }
        if (succeeded) onStudyDataChanged?.invoke()
    }

    private fun review(rating: ReviewRating) {
        val succeeded = updateSafely(StudyFailureKind.REVIEW_TRANSACTION) { facade.review(rating) }
        if (succeeded) {
            onStudyDataChanged?.invoke()
        }
    }

    private fun loadSafely(
        previousState: StudyUiState = StudyUiState()
    ): StudyUiState =
        try {
            facade.load().copy(loadError = null, failureKind = null)
        } catch (exception: Exception) {
            previousState.copy(
                loadError = StudyFailureMessage.forStudyData(exception),
                failureKind = StudyFailureKind.SESSION_RECOVERY,
                message = "Study data needs attention.",
                workspaceState = ReviewWorkspaceState.RecoverableFailure
            )
        }

    private fun updateSafely(
        failureKind: StudyFailureKind,
        operation: () -> StudyUiState
    ): Boolean =
        if (actionInProgress) {
            false
        } else {
            actionInProgress = true
            uiState = uiState.copy(actionInProgress = true)
            try {
                uiState = operation().copy(loadError = null, failureKind = null, actionInProgress = false)
                true
            } catch (exception: Exception) {
                uiState = uiState.copy(
                    loadError = StudyFailureMessage.forStudyData(exception),
                    failureKind = failureKind,
                    message = "Study data needs attention.",
                    workspaceState = ReviewWorkspaceState.RecoverableFailure,
                    actionInProgress = false
                )
                false
            } finally {
                actionInProgress = false
            }
        }
}
