package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Kết quả diagnostics đầy đủ của QueueTransformationPipeline.
 *
 * report chứa snapshot chi tiết.
 * metrics chứa số liệu tổng hợp phù hợp cho logging và telemetry.
 */
data class QueueTransformationDiagnostics(
    val report: QueueTransformationReport,
    val metrics: QueueTransformationMetrics
) {

    val finalCandidates:
            List<SelectionCandidate>
        get() =
            report.finalCandidates

    init {
        require(
            report.inputCandidates.size ==
                    metrics.candidateCount
        ) {
            "Diagnostics report and metrics candidate counts must match."
        }
    }
}