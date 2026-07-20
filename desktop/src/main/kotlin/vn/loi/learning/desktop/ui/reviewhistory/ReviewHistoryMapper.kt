package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.domain.study.memory.model.ReviewEvent

object ReviewHistoryMapper {

    fun map(
        event: ReviewEvent
    ): ReviewHistoryItemUi =
        ReviewHistoryItemUi(
            reviewedAt =
                ReviewHistoryFormatter.formatEpochMillis(
                    event.reviewedAt.epochMillis
                ),
            rating =
                event.rating.name,
            responseTime =
                ReviewHistoryFormatter.formatResponseTime(
                    event.responseTime?.millis
                ),
            stability =
                ReviewHistoryFormatter.formatDouble(
                    event.stateAfter.stabilityDays
                ),
            difficulty =
                ReviewHistoryFormatter.formatDouble(
                    event.stateAfter.difficulty
                )
        )
}