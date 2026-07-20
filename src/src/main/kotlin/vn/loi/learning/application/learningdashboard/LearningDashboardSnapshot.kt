package vn.loi.learning.application.learningdashboard

/**
 * Snapshot cấp cao nhất của Learning Dashboard.
 *
 * Dashboard được xây dựng theo hướng composition:
 * mỗi nhóm dữ liệu là một section độc lập, có model
 * và trách nhiệm riêng.
 *
 * Snapshot này chỉ tổng hợp các section đã được
 * thiết kế hoàn chỉnh. Nó không:
 * - truy cập repository;
 * - tính toán analytics;
 * - thực hiện scheduling;
 * - thay đổi domain state;
 * - sao chép các field bên trong từng section.
 */
data class LearningDashboardSnapshot(
    val activity: LearningDashboardActivitySnapshot,
    val memory: LearningDashboardMemorySnapshot,
    val scheduling: LearningDashboardSchedulingSnapshot,
    val retention: LearningDashboardRetentionSnapshot,
    val forecast: LearningDashboardForecastSnapshot
) {

    /**
     * Cho biết Dashboard có dữ liệu hoạt động học tập
     * trong khoảng thời gian được truy vấn hay không.
     *
     * Đây chỉ là delegation tới Activity section.
     */
    val hasActivity: Boolean
        get() = activity.hasActivity

    /**
     * Cho biết learner đã có memory hay chưa.
     *
     * Đây chỉ là delegation tới Memory section.
     */
    val hasMemories: Boolean
        get() = memory.hasMemories

    /**
     * Cho biết learner đang có memory cần học hoặc ôn.
     *
     * Đây chỉ là delegation tới Scheduling section.
     */
    val hasDueMemories: Boolean
        get() = scheduling.hasDueMemories

    /**
     * Cho biết learner đang có memory đã quá hạn.
     *
     * Đây chỉ là delegation tới Scheduling section.
     */
    val hasOverdueMemories: Boolean
        get() = scheduling.hasOverdueMemories

    /**
     * Cho biết Dashboard có retention data
     * để hiển thị hoặc phân tích hay không.
     *
     * Đây chỉ là delegation tới Retention section.
     */
    val hasRetentionData: Boolean
        get() = retention.hasData

    /**
     * Cho biết forecast có ít nhất một bucket
     * được yêu cầu hay không.
     *
     * Đây chỉ là delegation tới Forecast section.
     */
    val hasForecastBuckets: Boolean
        get() = forecast.hasBuckets

    /**
     * Cho biết có ít nhất một memory dự kiến đến hạn
     * trong toàn bộ forecast hay không.
     *
     * Đây chỉ là delegation tới Forecast section.
     */
    val hasForecastDueMemories: Boolean
        get() = forecast.hasDueMemories

    companion object {

        val EMPTY: LearningDashboardSnapshot =
            LearningDashboardSnapshot(
                activity =
                    LearningDashboardActivitySnapshot.EMPTY,
                memory =
                    LearningDashboardMemorySnapshot.EMPTY,
                scheduling =
                    LearningDashboardSchedulingSnapshot.EMPTY,
                retention =
                    LearningDashboardRetentionSnapshot.EMPTY,
                forecast =
                    LearningDashboardForecastSnapshot.EMPTY
            )
    }
}