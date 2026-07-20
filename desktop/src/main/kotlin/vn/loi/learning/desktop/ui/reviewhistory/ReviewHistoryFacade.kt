package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.application.reviewhistory.ReviewHistoryQuery
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.infrastructure.LearningApplicationContext

class ReviewHistoryFacade(
    private val applicationContext: LearningApplicationContext,
    private val learnerId: LearnerId =
        LearnerId("default-learner")
) {

    fun load(): ReviewHistoryUiState {
        val events =
            applicationContext.reviewHistory.query(
                ReviewHistoryQuery(
                    learnerId = learnerId
                )
            )

        return ReviewHistoryUiState(
            items =
                events
                    .asReversed()
                    .map(ReviewHistoryMapper::map)
        )
    }
}