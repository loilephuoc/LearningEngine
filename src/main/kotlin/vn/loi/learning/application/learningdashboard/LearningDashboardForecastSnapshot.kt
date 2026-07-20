package vn.loi.learning.application.learningdashboard

/**
 * Section forecast của Learning Dashboard.
 *
 * Section này compose [LearningDashboardForecast]
 * thay vì sao chép bucket hoặc tổng số due lên root Dashboard.
 *
 * Nhờ đó:
 * - forecast có contract riêng;
 * - calculator forecast độc lập với snapshot;
 * - LearningDashboardSnapshot không trở thành God DTO;
 * - Application Layer chỉ tổng hợp kết quả đã được tính.
 *
 * Model này:
 * - immutable;
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không thực hiện scheduling;
 * - không thay đổi MemoryState.
 */
data class LearningDashboardForecastSnapshot(
    val forecast: LearningDashboardForecast
) {

    /**
     * Cho biết forecast có ít nhất một bucket hay không.
     *
     * Đây chỉ là delegation tới forecast.
     */
    val hasBuckets: Boolean
        get() = forecast.hasBuckets

    /**
     * Cho biết có ít nhất một memory dự kiến đến hạn
     * trong toàn bộ forecast hay không.
     *
     * Đây chỉ là delegation tới forecast.
     */
    val hasDueMemories: Boolean
        get() = forecast.hasDueMemories

    companion object {

        val EMPTY: LearningDashboardForecastSnapshot =
            LearningDashboardForecastSnapshot(
                forecast =
                    LearningDashboardForecast.EMPTY
            )
    }
}