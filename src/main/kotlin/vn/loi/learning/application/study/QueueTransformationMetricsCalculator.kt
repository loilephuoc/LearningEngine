package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Chuyển QueueTransformationReport thành metric nhỏ gọn.
 *
 * Calculator không thay đổi report hoặc candidate.
 */
class QueueTransformationMetricsCalculator {

    fun calculate(
        report: QueueTransformationReport
    ): QueueTransformationMetrics =
        QueueTransformationMetrics(
            candidateCount =
                report.inputCandidates.size,
            strategyMovedCandidateCount =
                movedCandidateCount(
                    before =
                        report.inputCandidates,
                    after =
                        report.strategyCandidates
                ),
            initialDiversityMovedCandidateCount =
                movedCandidateCount(
                    before =
                        report.strategyCandidates,
                    after =
                        report
                            .initiallyDiversifiedCandidates
                ),
            balancerMovedCandidateCount =
                movedCandidateCount(
                    before =
                        report
                            .initiallyDiversifiedCandidates,
                    after =
                        report.balancedCandidates
                ),
            finalDiversityMovedCandidateCount =
                movedCandidateCount(
                    before =
                        report.balancedCandidates,
                    after =
                        report.finalCandidates
                ),
            pipelineMovedCandidateCount =
                movedCandidateCount(
                    before =
                        report.inputCandidates,
                    after =
                        report.finalCandidates
                )
        )

    private fun movedCandidateCount(
        before: List<SelectionCandidate>,
        after: List<SelectionCandidate>
    ): Int {
        check(before.size == after.size) {
            "Cannot calculate queue movement for lists of different sizes."
        }

        return before.indices.count { index ->
            before[index] != after[index]
        }
    }
}