package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Báo cáo đầy đủ của một lần chạy QueueTransformationPipeline.
 *
 * Mỗi danh sách là snapshot bất biến tại một stage cụ thể.
 * Report phục vụ:
 *
 * - kiểm thử pipeline;
 * - debug ordering;
 * - quan sát tác động của strategy, diversity và balancer;
 * - xây dựng diagnostics trong tương lai.
 *
 * Report không được dùng để thay đổi candidate hoặc persist state.
 */
data class QueueTransformationReport(
    val inputCandidates:
    List<SelectionCandidate>,
    val strategyCandidates:
    List<SelectionCandidate>,
    val initiallyDiversifiedCandidates:
    List<SelectionCandidate>,
    val balancedCandidates:
    List<SelectionCandidate>,
    val finalCandidates:
    List<SelectionCandidate>
) {

    init {
        val expectedSize =
            inputCandidates.size

        require(
            strategyCandidates.size ==
                    expectedSize
        ) {
            "Strategy snapshot size must match input size."
        }

        require(
            initiallyDiversifiedCandidates.size ==
                    expectedSize
        ) {
            "Initial diversity snapshot size must match input size."
        }

        require(
            balancedCandidates.size ==
                    expectedSize
        ) {
            "Balancer snapshot size must match input size."
        }

        require(
            finalCandidates.size ==
                    expectedSize
        ) {
            "Final snapshot size must match input size."
        }
    }

    /**
     * Cho biết strategy có thay đổi ordering ban đầu hay không.
     */
    val strategyChangedOrdering: Boolean
        get() =
            inputCandidates !=
                    strategyCandidates

    /**
     * Cho biết diversity lần đầu có thay đổi ordering hay không.
     */
    val initialDiversityChangedOrdering: Boolean
        get() =
            strategyCandidates !=
                    initiallyDiversifiedCandidates

    /**
     * Cho biết balancer có thay đổi ordering hay không.
     */
    val balancerChangedOrdering: Boolean
        get() =
            initiallyDiversifiedCandidates !=
                    balancedCandidates

    /**
     * Cho biết final diversity guard có sửa ordering hay không.
     */
    val finalDiversityChangedOrdering: Boolean
        get() =
            balancedCandidates !=
                    finalCandidates

    /**
     * Cho biết toàn pipeline có thay đổi ordering đầu vào hay không.
     */
    val pipelineChangedOrdering: Boolean
        get() =
            inputCandidates !=
                    finalCandidates

    companion object {

        fun create(
            inputCandidates:
            List<SelectionCandidate>,
            strategyCandidates:
            List<SelectionCandidate>,
            initiallyDiversifiedCandidates:
            List<SelectionCandidate>,
            balancedCandidates:
            List<SelectionCandidate>,
            finalCandidates:
            List<SelectionCandidate>
        ): QueueTransformationReport =
            QueueTransformationReport(
                inputCandidates =
                    inputCandidates.toList(),
                strategyCandidates =
                    strategyCandidates.toList(),
                initiallyDiversifiedCandidates =
                    initiallyDiversifiedCandidates
                        .toList(),
                balancedCandidates =
                    balancedCandidates.toList(),
                finalCandidates =
                    finalCandidates.toList()
            )
    }
}