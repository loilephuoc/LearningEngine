package vn.loi.learning.application.study

import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Điều phối toàn bộ các bước biến đổi ordering của StudyQueue.
 *
 * Pipeline:
 *
 * 1. StudyQueueStrategy quyết định ordering chính.
 * 2. QueueDiversifier giảm các candidate giống nhau đứng cạnh nhau.
 * 3. QueueBalancer cân bằng ordering theo tiêu chí bổ sung.
 * 4. QueueDiversifier chạy lại như final guard.
 *
 * Sau mỗi bước, QueueTransformationValidator kiểm tra rằng
 * transformation chỉ thay đổi ordering và vẫn bảo toàn chính xác
 * toàn bộ candidate.
 *
 * API:
 *
 * - transform(): trả trực tiếp final candidate list;
 * - transformWithReport(): trả snapshot của toàn bộ pipeline;
 * - transformWithDiagnostics(): trả snapshot và metric tổng hợp.
 *
 * Pipeline không:
 * - lựa chọn candidate;
 * - thêm hoặc loại candidate;
 * - áp dụng SessionPolicy limit;
 * - đọc hoặc ghi repository;
 * - tạo hoặc persist StudyQueue.
 */
class QueueTransformationPipeline(
    private val validator:
    QueueTransformationValidator =
        QueueTransformationValidator(),
    private val metricsCalculator:
    QueueTransformationMetricsCalculator =
        QueueTransformationMetricsCalculator()
) {

    /**
     * API chính dùng trong StudyQueuePlanner.
     */
    fun transform(
        candidates:
        List<SelectionCandidate>,
        strategy:
        StudyQueueStrategy,
        queueDiversifier:
        QueueDiversifier,
        queueBalancer:
        QueueBalancer,
        postStrategyOrderer: (List<SelectionCandidate>) -> List<SelectionCandidate> = { it }
    ): List<SelectionCandidate> =
        transformWithReport(
            candidates =
                candidates,
            strategy =
                strategy,
            queueDiversifier =
                queueDiversifier,
            queueBalancer =
                queueBalancer,
            postStrategyOrderer = postStrategyOrderer
        ).finalCandidates

    /**
     * Chạy pipeline và trả snapshot sau từng transformation.
     */
    fun transformWithReport(
        candidates:
        List<SelectionCandidate>,
        strategy:
        StudyQueueStrategy,
        queueDiversifier:
        QueueDiversifier,
        queueBalancer:
        QueueBalancer,
        postStrategyOrderer: (List<SelectionCandidate>) -> List<SelectionCandidate> = { it }
    ): QueueTransformationReport {
        val inputCandidates =
            candidates.toList()

        val orderedCandidates =
            strategy.order(
                inputCandidates
            )

        validator.validate(
            stage =
                QueueTransformationStage
                    .STRATEGY,
            before =
                inputCandidates,
            after =
                orderedCandidates
        )

        val postStrategyCandidates = postStrategyOrderer(orderedCandidates)
        validator.validate(
            stage = QueueTransformationStage.STRATEGY,
            before = orderedCandidates,
            after = postStrategyCandidates
        )

        val initiallyDiversifiedCandidates =
            queueDiversifier.diversify(
                postStrategyCandidates
            )

        validator.validate(
            stage =
                QueueTransformationStage
                    .INITIAL_DIVERSITY,
            before =
                postStrategyCandidates,
            after =
                initiallyDiversifiedCandidates
        )

        val balancedCandidates =
            queueBalancer.balance(
                initiallyDiversifiedCandidates
            )

        validator.validate(
            stage =
                QueueTransformationStage
                    .BALANCER,
            before =
                initiallyDiversifiedCandidates,
            after =
                balancedCandidates
        )

        val finallyDiversifiedCandidates =
            queueDiversifier.diversify(
                balancedCandidates
            )

        validator.validate(
            stage =
                QueueTransformationStage
                    .FINAL_DIVERSITY,
            before =
                balancedCandidates,
            after =
                finallyDiversifiedCandidates
        )

        return QueueTransformationReport.create(
            inputCandidates =
                inputCandidates,
            strategyCandidates =
                postStrategyCandidates,
            initiallyDiversifiedCandidates =
                initiallyDiversifiedCandidates,
            balancedCandidates =
                balancedCandidates,
            finalCandidates =
                finallyDiversifiedCandidates
        )
    }

    /**
     * Chạy pipeline và trả cả report lẫn metric tổng hợp.
     */
    fun transformWithDiagnostics(
        candidates:
        List<SelectionCandidate>,
        strategy:
        StudyQueueStrategy,
        queueDiversifier:
        QueueDiversifier,
        queueBalancer:
        QueueBalancer
    ): QueueTransformationDiagnostics {
        val report =
            transformWithReport(
                candidates =
                    candidates,
                strategy =
                    strategy,
                queueDiversifier =
                    queueDiversifier,
                queueBalancer =
                    queueBalancer
            )

        return QueueTransformationDiagnostics(
            report =
                report,
            metrics =
                metricsCalculator.calculate(
                    report
                )
        )
    }
}
