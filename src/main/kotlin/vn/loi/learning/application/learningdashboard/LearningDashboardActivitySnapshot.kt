package vn.loi.learning.application.learningdashboard

import vn.loi.learning.application.progress.LearningProgressSnapshot

/**
 * Section hoạt động học tập của Learning Dashboard.
 *
 * Section này tái sử dụng [LearningProgressSnapshot] để:
 * - không lặp lại các trường thống kê review;
 * - không lặp lại các invariant của Learning Progress;
 * - giữ ranh giới rõ ràng giữa Dashboard composition
 *   và việc tính toán dữ liệu hoạt động;
 * - cho phép Learning Dashboard phát triển độc lập
 *   mà không biến thành một God DTO.
 *
 * Model này:
 * - là immutable Application result model;
 * - không truy cập repository;
 * - không thực hiện analytics;
 * - không thay đổi trạng thái domain;
 * - không tự đọc đồng hồ hệ thống.
 */
data class LearningDashboardActivitySnapshot(
    val progress: LearningProgressSnapshot
) {

    /**
     * Dashboard có ít nhất một hoạt động review
     * trong khoảng thời gian đã được truy vấn.
     */
    val hasActivity: Boolean
        get() = progress.hasReviewActivity

    companion object {

        val EMPTY: LearningDashboardActivitySnapshot =
            LearningDashboardActivitySnapshot(
                progress = LearningProgressSnapshot.EMPTY
            )
    }
}