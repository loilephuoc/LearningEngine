package vn.loi.learning.application.study

/**
 * Số liệu tổng hợp của một lần biến đổi StudyQueue.
 *
 * movedCandidateCount của mỗi stage là số candidate có vị trí
 * khác giữa đầu vào và đầu ra của stage đó.
 *
 * Metric không phản ánh candidate được thêm hoặc loại vì invariant
 * của QueueTransformationPipeline đã cấm các thay đổi đó.
 */
data class QueueTransformationMetrics(
    val candidateCount: Int,
    val strategyMovedCandidateCount: Int,
    val initialDiversityMovedCandidateCount: Int,
    val balancerMovedCandidateCount: Int,
    val finalDiversityMovedCandidateCount: Int,
    val pipelineMovedCandidateCount: Int
) {

    init {
        require(candidateCount >= 0) {
            "Candidate count must not be negative."
        }

        require(
            strategyMovedCandidateCount in
                    0..candidateCount
        ) {
            "Strategy moved candidate count is invalid."
        }

        require(
            initialDiversityMovedCandidateCount in
                    0..candidateCount
        ) {
            "Initial diversity moved candidate count is invalid."
        }

        require(
            balancerMovedCandidateCount in
                    0..candidateCount
        ) {
            "Balancer moved candidate count is invalid."
        }

        require(
            finalDiversityMovedCandidateCount in
                    0..candidateCount
        ) {
            "Final diversity moved candidate count is invalid."
        }

        require(
            pipelineMovedCandidateCount in
                    0..candidateCount
        ) {
            "Pipeline moved candidate count is invalid."
        }
    }

    val strategyChangedOrdering: Boolean
        get() =
            strategyMovedCandidateCount > 0

    val initialDiversityChangedOrdering: Boolean
        get() =
            initialDiversityMovedCandidateCount > 0

    val balancerChangedOrdering: Boolean
        get() =
            balancerMovedCandidateCount > 0

    val finalDiversityChangedOrdering: Boolean
        get() =
            finalDiversityMovedCandidateCount > 0

    val pipelineChangedOrdering: Boolean
        get() =
            pipelineMovedCandidateCount > 0

    /**
     * Tổng số lần candidate được ghi nhận là đổi vị trí
     * qua các transformation stage.
     *
     * Một candidate có thể được tính nhiều lần nếu bị di chuyển
     * tại nhiều stage khác nhau.
     */
    val totalStageMovementCount: Int
        get() =
            strategyMovedCandidateCount +
                    initialDiversityMovedCandidateCount +
                    balancerMovedCandidateCount +
                    finalDiversityMovedCandidateCount
}