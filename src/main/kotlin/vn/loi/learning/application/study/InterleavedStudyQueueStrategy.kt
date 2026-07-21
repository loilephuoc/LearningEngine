package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Xen kẽ REVIEW và NEW trong StudyQueue.
 *
 * Strategy bắt đầu bằng REVIEW:
 *
 * R1, N1, R2, N2, ...
 *
 * Thứ tự ưu tiên bên trong từng nhóm vẫn được lấy từ
 * ReviewFirstStudyQueueStrategy.
 *
 * Khi một nhóm hết item, phần còn lại của nhóm kia được nối tiếp
 * mà không thay đổi thứ tự nội bộ.
 */
class InterleavedStudyQueueStrategy(
    private val baseOrdering:
    StudyQueueStrategy =
        ReviewFirstStudyQueueStrategy()
) : StudyQueueStrategy {

    override fun order(
        candidates: List<SelectionCandidate>
    ): List<SelectionCandidate> {
        val orderedCandidates =
            baseOrdering.order(candidates)

        val reviewCandidates =
            orderedCandidates.filterNot { candidate ->
                candidate.isNew
            }

        val newCandidates =
            orderedCandidates.filter { candidate ->
                candidate.isNew
            }

        return buildList {
            val maximumGroupSize =
                maxOf(
                    reviewCandidates.size,
                    newCandidates.size
                )

            repeat(maximumGroupSize) { index ->
                reviewCandidates
                    .getOrNull(index)
                    ?.let(::add)

                newCandidates
                    .getOrNull(index)
                    ?.let(::add)
            }
        }
    }
}