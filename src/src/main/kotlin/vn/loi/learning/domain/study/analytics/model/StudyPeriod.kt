package vn.loi.learning.domain.study.analytics.model

import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Khoảng thời gian dùng để phân tích lịch sử học.
 *
 * Quy ước:
 * - startInclusive được tính;
 * - endExclusive không được tính.
 */
data class StudyPeriod(
    val startInclusive: Moment,
    val endExclusive: Moment
) {

    init {
        require(startInclusive < endExclusive) {
            "Study period start must be before end."
        }
    }

    /**
     * Kiểm tra một thời điểm có nằm trong khoảng hay không.
     */
    operator fun contains(
        moment: Moment
    ): Boolean =
        moment >= startInclusive &&
                moment < endExclusive
}