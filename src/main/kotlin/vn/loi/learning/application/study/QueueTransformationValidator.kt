package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Kiểm tra invariant của một bước biến đổi StudyQueue.
 *
 * Transformation hợp lệ chỉ được thay đổi thứ tự candidate.
 * Transformation không được:
 *
 * - thêm candidate;
 * - loại candidate;
 * - nhân đôi candidate;
 * - thay thế candidate bằng candidate khác.
 *
 * Validator so sánh theo toàn bộ giá trị SelectionCandidate,
 * không chỉ LearningItemId, để phát hiện cả trường hợp implementation
 * tạo candidate mới có cùng id nhưng dữ liệu khác.
 */
class QueueTransformationValidator {

    fun validate(
        stage: QueueTransformationStage,
        before:
        List<SelectionCandidate>,
        after:
        List<SelectionCandidate>
    ) {
        check(before.size == after.size) {
            violationMessage(
                stage = stage,
                detail =
                    "Candidate count changed from " +
                            "${before.size} to ${after.size}."
            )
        }

        val beforeCounts =
            before.groupingBy { candidate ->
                candidate
            }.eachCount()

        val afterCounts =
            after.groupingBy { candidate ->
                candidate
            }.eachCount()

        check(beforeCounts == afterCounts) {
            violationMessage(
                stage = stage,
                detail =
                    "The candidate collection was modified. " +
                            "Transformations may only change ordering."
            )
        }
    }

    private fun violationMessage(
        stage: QueueTransformationStage,
        detail: String
    ): String =
        "Queue transformation invariant violated at " +
                "${stage.name}: $detail"
}