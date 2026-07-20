package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.loi.learning.domain.study.memory.model.ReviewRating

class StudyViewModel(
    private val facade: StudyFacade,
    private val onStudyDataChanged:
    (() -> Unit)? = null
) {

    var uiState by mutableStateOf(
        facade.load()
    )
        private set

    fun refresh() {
        uiState =
            facade.load()
    }

    fun startStudy() {
        uiState =
            facade.startStudy()
    }

    fun startLessonStudy(
        contentId: String
    ) {
        uiState =
            facade.startLessonStudy(
                contentId
            )
    }

    fun revealAnswer() {
        uiState =
            facade.revealAnswer()
    }

    fun reviewAgain() {
        review(
            ReviewRating.AGAIN
        )
    }

    fun reviewHard() {
        review(
            ReviewRating.HARD
        )
    }

    fun reviewGood() {
        review(
            ReviewRating.GOOD
        )
    }

    fun reviewEasy() {
        review(
            ReviewRating.EASY
        )
    }

    private fun review(
        rating: ReviewRating
    ) {
        uiState =
            facade.review(
                rating
            )

        onStudyDataChanged?.invoke()
    }
}