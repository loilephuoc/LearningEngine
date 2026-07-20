package vn.loi.learning.application.learningdashboard

import vn.loi.learning.application.progress.LearningProgressQuery
import vn.loi.learning.application.progress.LearningProgressQueryService

/**
 * Application service tạo Activity section
 * của Learning Dashboard.
 *
 * Service này:
 * - nhận LearningProgressQuery đã được caller chuẩn bị;
 * - tái sử dụng LearningProgressQueryService;
 * - bọc LearningProgressSnapshot thành
 *   LearningDashboardActivitySnapshot.
 *
 * Service không:
 * - truy cập repository trực tiếp;
 * - tự lọc ReviewEvent;
 * - tự tính activity statistics;
 * - tự xác định ngày hoặc múi giờ;
 * - tự đọc đồng hồ hệ thống.
 */
class LearningDashboardActivityQueryService(
    private val learningProgressQueryService:
    LearningProgressQueryService
) {

    fun query(
        query: LearningProgressQuery
    ): LearningDashboardActivitySnapshot =
        LearningDashboardActivitySnapshot(
            progress =
                learningProgressQueryService.query(
                    query = query
                )
        )
}