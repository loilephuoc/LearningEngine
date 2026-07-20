package vn.loi.learning.domain.study.fsrs.model

/**
 * Mục tiêu tỷ lệ ghi nhớ mà người học mong muốn duy trì.
 *
 * Giá trị hợp lệ nằm trong khoảng từ 0.0 đến 1.0:
 *
 * - 0.80: học nhanh, ít ôn hơn
 * - 0.90: mặc định
 * - 0.95: học chắc hơn, ôn thường xuyên hơn
 *
 * Đây là mục tiêu học tập, không phải trạng thái hiện tại
 * của một Memory.
 */
@JvmInline
value class DesiredRetention(
    val value: Double
) {

    init {
        require(value.isFinite()) {
            "DesiredRetention must be finite, but was $value."
        }

        require(value in MIN_VALUE..MAX_VALUE) {
            "DesiredRetention must be between $MIN_VALUE and $MAX_VALUE, but was $value."
        }
    }

    companion object {

        const val MIN_VALUE = 0.0
        const val MAX_VALUE = 1.0

        val LOW =
            DesiredRetention(0.80)

        val DEFAULT =
            DesiredRetention(0.90)

        val HIGH =
            DesiredRetention(0.95)
    }
}