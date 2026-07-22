package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.loi.learning.domain.study.memory.model.ReviewRating

class StudyViewModel(
    private val facade: StudyFacade,
    private val onStudyDataChanged: (() -> Unit)? = null
) {

    var uiState by mutableStateOf(loadSafely())
        private set

    fun refresh() {
        uiState = loadSafely(previousState = uiState)
    }

    fun startStudy() = updateSafely { facade.startStudy() }

    fun startLessonStudy(contentId: String) =
        updateSafely { facade.startLessonStudy(contentId) }

    fun revealAnswer() = updateSafely { facade.revealAnswer() }

    fun reviewAgain() = review(ReviewRating.AGAIN)
    fun reviewHard() = review(ReviewRating.HARD)
    fun reviewGood() = review(ReviewRating.GOOD)
    fun reviewEasy() = review(ReviewRating.EASY)

    fun undoLatestReview() {
        val succeeded = updateSafely { facade.undoLatestReview() }
        if (succeeded) onStudyDataChanged?.invoke()
    }

    private fun review(rating: ReviewRating) {
        val succeeded = updateSafely { facade.review(rating) }
        if (succeeded) {
            onStudyDataChanged?.invoke()
        }
    }

    private fun loadSafely(
        previousState: StudyUiState = StudyUiState()
    ): StudyUiState =
        try {
            facade.load().copy(loadError = null)
        } catch (exception: Exception) {
            previousState.copy(
                loadError = StudyFailureMessage.forStudyData(exception),
                message = "Study data needs attention.",
                workspaceState = ReviewWorkspaceState.RecoverableFailure
            )
        }

    private fun updateSafely(
        operation: () -> StudyUiState
    ): Boolean =
        try {
            uiState = operation().copy(loadError = null)
            true
        } catch (exception: Exception) {
            uiState = uiState.copy(
                loadError = StudyFailureMessage.forStudyData(exception),
                message = "Study data needs attention.",
                workspaceState = ReviewWorkspaceState.RecoverableFailure
            )
            false
        }
}
