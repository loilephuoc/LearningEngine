package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Tự động lựa chọn ordering dựa trên tỷ lệ candidate REVIEW và NEW.
 *
 * Quy tắc:
 *
 * - REVIEW >= dominanceRatio × NEW:
 *   dùng REVIEW_FIRST;
 *
 * - NEW >= dominanceRatio × REVIEW:
 *   dùng NEW_FIRST;
 *
 * - các trường hợp còn lại:
 *   dùng INTERLEAVED.
 *
 * Strategy chỉ xem các candidate đã được planner chuẩn bị.
 * Strategy không:
 * - đọc repository;
 * - áp dụng SessionPolicy limit;
 * - thay đổi candidate;
 * - persist StudyQueue.
 */
class AdaptiveStudyQueueStrategy(
    private val reviewFirstStrategy:
    StudyQueueStrategy =
        ReviewFirstStudyQueueStrategy(),
    private val newFirstStrategy:
    StudyQueueStrategy =
        NewFirstStudyQueueStrategy(),
    private val interleavedStrategy:
    StudyQueueStrategy =
        InterleavedStudyQueueStrategy(),
    private val dominanceRatio: Int = 2
) : StudyQueueStrategy {

    init {
        require(dominanceRatio >= 2) {
            "Dominance ratio must be at least 2."
        }
    }

    override fun order(
        candidates: List<SelectionCandidate>
    ): List<SelectionCandidate> {
        val newItemCount =
            candidates.count { candidate ->
                candidate.isNew
            }

        val reviewItemCount =
            candidates.size - newItemCount

        val selectedStrategy =
            selectStrategy(
                reviewItemCount =
                    reviewItemCount,
                newItemCount =
                    newItemCount
            )

        return selectedStrategy.order(
            candidates
        )
    }

    private fun selectStrategy(
        reviewItemCount: Int,
        newItemCount: Int
    ): StudyQueueStrategy {
        if (
            reviewItemCount > 0 &&
            newItemCount == 0
        ) {
            return reviewFirstStrategy
        }

        if (
            newItemCount > 0 &&
            reviewItemCount == 0
        ) {
            return newFirstStrategy
        }

        if (
            reviewItemCount >=
            newItemCount * dominanceRatio
        ) {
            return reviewFirstStrategy
        }

        if (
            newItemCount >=
            reviewItemCount * dominanceRatio
        ) {
            return newFirstStrategy
        }

        return interleavedStrategy
    }
}