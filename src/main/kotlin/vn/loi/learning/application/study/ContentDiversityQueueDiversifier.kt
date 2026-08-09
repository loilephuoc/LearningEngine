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

        // LinkedList preserves the exact selection algorithm while avoiding the
        // O(n^2) array shifting caused by MutableList.removeAt(0) for the common
        // case where adjacent contents are already diverse.
        val remaining =
            java.util.LinkedList(orderedCandidates)

        val diversified =
            ArrayList<SelectionCandidate>(
                orderedCandidates.size
            )

        var previousContentId:
                ContentId? = null

        while (remaining.isNotEmpty()) {
            val selectedCandidate =
                takeNext(
                    remaining = remaining,
                    previousContentId = previousContentId
                )

            diversified.add(
                selectedCandidate
            )

            previousContentId =
                selectedCandidate.contentId
        }

        return diversified
    }

    private fun takeNext(
        remaining: java.util.LinkedList<SelectionCandidate>,
        previousContentId: ContentId?
    ): SelectionCandidate {
        if (previousContentId == null || remaining.first().contentId != previousContentId) {
            return remaining.removeFirst()
        }

        val iterator = remaining.listIterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            if (candidate.contentId != previousContentId) {
                iterator.remove()
                return candidate
            }
        }
        return remaining.removeFirst()
    }
}
