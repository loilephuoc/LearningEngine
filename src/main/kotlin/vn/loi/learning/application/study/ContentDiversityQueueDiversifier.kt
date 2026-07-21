package vn.loi.learning.application.study

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Giảm việc hai LearningItem của cùng một Content xuất hiện liên tiếp.
 *
 * Thuật toán giữ tối đa thứ tự ưu tiên ban đầu:
 *
 * 1. Xét candidate đầu tiên còn lại.
 * 2. Nếu ContentId khác candidate trước thì chọn candidate đó.
 * 3. Nếu trùng ContentId, tìm candidate đầu tiên có ContentId khác.
 * 4. Nếu không còn lựa chọn khác, giữ candidate đầu tiên.
 *
 * Diversifier bảo toàn:
 * - số lượng candidate;
 * - identity của từng candidate;
 * - thứ tự tương đối nhiều nhất có thể;
 * - tính deterministic.
 */
class ContentDiversityQueueDiversifier :
    QueueDiversifier {

    override fun diversify(
        orderedCandidates:
        List<SelectionCandidate>
    ): List<SelectionCandidate> {
        if (orderedCandidates.size < 2) {
            return orderedCandidates.toList()
        }

        val remaining =
            orderedCandidates.toMutableList()

        val diversified =
            ArrayList<SelectionCandidate>(
                orderedCandidates.size
            )

        var previousContentId:
                ContentId? = null

        while (remaining.isNotEmpty()) {
            val selectedIndex =
                findNextIndex(
                    remaining =
                        remaining,
                    previousContentId =
                        previousContentId
                )

            val selectedCandidate =
                remaining.removeAt(
                    selectedIndex
                )

            diversified.add(
                selectedCandidate
            )

            previousContentId =
                selectedCandidate.contentId
        }

        return diversified
    }

    private fun findNextIndex(
        remaining:
        List<SelectionCandidate>,
        previousContentId: ContentId?
    ): Int {
        if (previousContentId == null) {
            return 0
        }

        val alternativeIndex =
            remaining.indexOfFirst { candidate ->
                candidate.contentId !=
                        previousContentId
            }

        return if (alternativeIndex >= 0) {
            alternativeIndex
        } else {
            0
        }
    }
}