package vn.loi.learning.application.learningdashboard

/**
 * Section retention của Learning Dashboard.
 *
 * Section này compose [LearningDashboardRetentionStatistics]
 * thay vì sao chép các metric retention lên root Dashboard.
 *
 * Nhờ đó:
 * - retention có contract riêng;
 * - calculator retention độc lập với snapshot;
 * - LearningDashboardSnapshot không trở thành God DTO;
 * - Application Layer chỉ tổng hợp kết quả đã được tính.
 *
 * Model này:
 * - immutable;
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không gọi ForgettingCurve;
 * - không thay đổi MemoryState.
 */
data class LearningDashboardRetentionSnapshot(
    val statistics: LearningDashboardRetentionStatistics
) {

    /**
     * Cho biết section retention có dữ liệu
     * để hiển thị hoặc phân tích hay không.
     *
     * Đây chỉ là delegation tới retention statistics.
     */
    val hasData: Boolean
        get() = statistics.hasData

    companion object {

        val EMPTY: LearningDashboardRetentionSnapshot =
            LearningDashboardRetentionSnapshot(
                statistics =
                    LearningDashboardRetentionStatistics.EMPTY
            )
    }
}