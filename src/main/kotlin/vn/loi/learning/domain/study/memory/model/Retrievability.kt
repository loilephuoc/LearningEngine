package vn.loi.learning.domain.study.memory.model

/**
 * Xác suất một LearningItem có thể được truy hồi thành công
 * tại một thời điểm nhất định.
 *
 * Giá trị hợp lệ nằm trong khoảng từ 0.0 đến 1.0:
 *
 * - 0.0: hoàn toàn không thể truy hồi
 * - 1.0: có khả năng truy hồi hoàn toàn
 *
 * Retrievability hiện chỉ là một Value Object.
 * Việc tính toán giá trị này sẽ thuộc trách nhiệm của
 * ForgettingCurve trong một bước phát triển sau.
 */
@JvmInline
value class Retrievability(
    val value: Double
) {

    init {
        require(value.isFinite()) {
            "Retrievability must be finite, but was $value."
        }

        require(value in MIN_VALUE..MAX_VALUE) {
            "Retrievability must be between $MIN_VALUE and $MAX_VALUE, but was $value."
        }
    }

    companion object {

        const val MIN_VALUE: Double = 0.0
        const val MAX_VALUE: Double = 1.0

        val FORGOTTEN: Retrievability =
            Retrievability(MIN_VALUE)

        val FULLY_RETRIEVABLE: Retrievability =
            Retrievability(MAX_VALUE)
    }
}